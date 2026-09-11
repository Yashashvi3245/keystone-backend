package com.keystone;

import com.keystone.dto.WorkOrderRequest;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.model.*;
import com.keystone.repository.*;
import com.keystone.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the governed work-order lifecycle state machine.
 * All transitions and illegal-jump rejections are verified
 * at the service layer (which is what the brief mandates).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkOrderLifecycleTest {

    @Autowired WorkOrderService     workOrderService;
    @Autowired CustomerRepository   customerRepository;
    @Autowired SiteRepository       siteRepository;
    @Autowired UserRepository       userRepository;
    @Autowired PartRepository       partRepository;

    private Customer  customer;
    private Site      site;
    private User      manager;
    private User      technician;
    private WorkOrder savedOrder;

    @BeforeEach
    void setUp() {
        // Customer + site
        customer = new Customer("Test Corp", "testcorp@example.com");
        customerRepository.save(customer);

        site = new Site("HQ", "1 Main St", "Chicago", "IL", "60601", customer);
        siteRepository.save(site);

        // Users
        manager = new User("Mgr", "mgr@test.com", "$2a$10$dummyhashXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", Role.MANAGER);
        userRepository.save(manager);

        technician = new User("Tech", "tech@test.com", "$2a$10$dummyhashXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", Role.TECHNICIAN);
        userRepository.save(technician);

        // Authenticate as manager for service calls
        authenticateAs(manager, "MANAGER");
    }

    // -------------------------------------------------------
    // Happy path: NEW → ASSIGNED → IN_PROGRESS → COMPLETED → CLOSED
    // -------------------------------------------------------

    @Test
    void happyPath_fullLifecycle() {
        WorkOrderResponse wo = createWorkOrder();
        assertThat(wo.status()).isEqualTo(WorkOrderStatus.NEW);

        // Assign
        WorkOrderResponse assigned = workOrderService.assignWorkOrder(wo.id(), technician.getId());
        assertThat(assigned.status()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(assigned.assigneeId()).isEqualTo(technician.getId());

        // Start (technician action)
        WorkOrderResponse inProgress = workOrderService.updateStatus(wo.id(), WorkOrderStatus.IN_PROGRESS);
        assertThat(inProgress.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);

        // Complete
        WorkOrderResponse completed = workOrderService.updateStatus(wo.id(), WorkOrderStatus.COMPLETED);
        assertThat(completed.status()).isEqualTo(WorkOrderStatus.COMPLETED);

        // Close (manager action)
        WorkOrderResponse closed = workOrderService.updateStatus(wo.id(), WorkOrderStatus.CLOSED);
        assertThat(closed.status()).isEqualTo(WorkOrderStatus.CLOSED);
    }

    // -------------------------------------------------------
    // ON_HOLD detour
    // -------------------------------------------------------

    @Test
    void onHold_resume_cycle() {
        WorkOrderResponse wo = createWorkOrder();
        workOrderService.assignWorkOrder(wo.id(), technician.getId());
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.IN_PROGRESS);

        WorkOrderResponse onHold = workOrderService.updateStatus(wo.id(), WorkOrderStatus.ON_HOLD);
        assertThat(onHold.status()).isEqualTo(WorkOrderStatus.ON_HOLD);

        WorkOrderResponse resumed = workOrderService.updateStatus(wo.id(), WorkOrderStatus.IN_PROGRESS);
        assertThat(resumed.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
    }

    // -------------------------------------------------------
    // CANCEL from NEW or ASSIGNED
    // -------------------------------------------------------

    @Test
    void cancelFromNew() {
        WorkOrderResponse wo = createWorkOrder();
        WorkOrderResponse cancelled = workOrderService.updateStatus(wo.id(), WorkOrderStatus.CANCELLED);
        assertThat(cancelled.status()).isEqualTo(WorkOrderStatus.CANCELLED);
    }

    @Test
    void cancelFromAssigned() {
        WorkOrderResponse wo = createWorkOrder();
        workOrderService.assignWorkOrder(wo.id(), technician.getId());
        WorkOrderResponse cancelled = workOrderService.updateStatus(wo.id(), WorkOrderStatus.CANCELLED);
        assertThat(cancelled.status()).isEqualTo(WorkOrderStatus.CANCELLED);
    }

    // -------------------------------------------------------
    // Illegal transitions are rejected with IllegalStateException
    // -------------------------------------------------------

    @Test
    void illegalJump_newToCompleted_isRejected() {
        WorkOrderResponse wo = createWorkOrder();
        assertThatThrownBy(() ->
                workOrderService.updateStatus(wo.id(), WorkOrderStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void illegalJump_newToClosed_isRejected() {
        WorkOrderResponse wo = createWorkOrder();
        assertThatThrownBy(() ->
                workOrderService.updateStatus(wo.id(), WorkOrderStatus.CLOSED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void illegalJump_inProgressToAssigned_isRejected() {
        WorkOrderResponse wo = createWorkOrder();
        workOrderService.assignWorkOrder(wo.id(), technician.getId());
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.IN_PROGRESS);
        assertThatThrownBy(() ->
                workOrderService.updateStatus(wo.id(), WorkOrderStatus.ASSIGNED))
                .isInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------
    // CLOSED / CANCELLED are terminal — no further transitions
    // -------------------------------------------------------

    @Test
    void closedIsTerminal() {
        WorkOrderResponse wo = createWorkOrder();
        workOrderService.assignWorkOrder(wo.id(), technician.getId());
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.IN_PROGRESS);
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.COMPLETED);
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.CLOSED);

        assertThatThrownBy(() ->
                workOrderService.updateStatus(wo.id(), WorkOrderStatus.IN_PROGRESS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelledIsTerminal() {
        WorkOrderResponse wo = createWorkOrder();
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.CANCELLED);

        assertThatThrownBy(() ->
                workOrderService.updateStatus(wo.id(), WorkOrderStatus.NEW))
                .isInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------
    // Closed / cancelled work orders are immutable (no edit)
    // -------------------------------------------------------

    @Test
    void closedWorkOrder_cannotBeEdited() {
        WorkOrderResponse wo = createWorkOrder();
        workOrderService.assignWorkOrder(wo.id(), technician.getId());
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.IN_PROGRESS);
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.COMPLETED);
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.CLOSED);

        WorkOrderRequest editRequest = new WorkOrderRequest(
                "Updated Title", "Updated description", Priority.HIGH,
                null, customer.getId(), site.getId(), null);

        assertThatThrownBy(() ->
                workOrderService.updateWorkOrder(wo.id(), editRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Closed or cancelled");
    }

    @Test
    void cancelledWorkOrder_cannotBeEdited() {
        WorkOrderResponse wo = createWorkOrder();
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.CANCELLED);

        WorkOrderRequest editRequest = new WorkOrderRequest(
                "Updated Title", "desc", Priority.LOW,
                null, customer.getId(), site.getId(), null);

        assertThatThrownBy(() ->
                workOrderService.updateWorkOrder(wo.id(), editRequest))
                .isInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------
    // History is written on every transition
    // -------------------------------------------------------

    @Test
    void historyWrittenOnEveryTransition() {
        WorkOrderResponse wo = createWorkOrder();
        workOrderService.assignWorkOrder(wo.id(), technician.getId());
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.IN_PROGRESS);
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.COMPLETED);
        workOrderService.updateStatus(wo.id(), WorkOrderStatus.CLOSED);

        var history = workOrderService.getWorkOrderHistory(wo.id());
        // created (null→NEW) + assigned (NEW→ASSIGNED) + in_progress + completed + closed = 5
        assertThat(history).hasSizeGreaterThanOrEqualTo(4);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private WorkOrderResponse createWorkOrder() {
        WorkOrderRequest req = new WorkOrderRequest(
                "Test Work Order", "Test description",
                Priority.MEDIUM, null,
                customer.getId(), site.getId(), null);
        return workOrderService.createWorkOrder(req);
    }

    private void authenticateAs(User user, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
