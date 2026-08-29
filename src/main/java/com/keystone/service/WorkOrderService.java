package com.keystone.service;

import com.keystone.dto.WorkOrderRequest;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.model.Customer;
import com.keystone.model.Site;
import com.keystone.model.User;
import com.keystone.model.WorkOrder;
import com.keystone.model.WorkOrderStatus;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            CustomerRepository customerRepository,
            SiteRepository siteRepository,
            UserRepository userRepository) {

        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
    }

    // =========================
    // GET ALL WORK ORDERS
    // =========================
    public List<WorkOrderResponse> getAllWorkOrders() {

        return workOrderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // GET WORK ORDER BY ID
    // =========================
    public WorkOrderResponse getWorkOrderById(Long id) {

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found"));

        return toResponse(workOrder);
    }

    // =========================
    // GET WORK ORDER BY CODE
    // =========================
    public WorkOrderResponse getWorkOrderByCode(String code) {

        WorkOrder workOrder = workOrderRepository.findByCode(code)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found"));

        return toResponse(workOrder);
    }

    // =========================
    // CREATE WORK ORDER
    // =========================
    public WorkOrderResponse createWorkOrder(
            WorkOrderRequest request) {

        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        Site site = siteRepository
                .findById(request.siteId())
                .orElseThrow(() ->
                        new RuntimeException("Site not found"));

        // Site must belong to selected customer
        if (!site.getCustomer().getId()
                .equals(customer.getId())) {

            throw new RuntimeException(
                    "Site does not belong to selected customer"
            );
        }

        WorkOrder workOrder = new WorkOrder();

        workOrder.setCode(generateWorkOrderCode());
        workOrder.setTitle(request.title());
        workOrder.setDescription(request.description());
        workOrder.setPriority(request.priority());
        workOrder.setSlaDueDate(request.slaDueDate());
        workOrder.setCustomer(customer);
        workOrder.setSite(site);

        // Optional assignee
        if (request.assigneeId() != null) {

            User assignee = userRepository
                    .findById(request.assigneeId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Assignee not found"
                            ));

            workOrder.setAssignee(assignee);
            workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        }

        WorkOrder savedWorkOrder =
                workOrderRepository.save(workOrder);

        return toResponse(savedWorkOrder);
    }

    // =========================
    // UPDATE WORK ORDER
    // =========================
    public WorkOrderResponse updateWorkOrder(
            Long id,
            WorkOrderRequest request) {

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found"));

        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        Site site = siteRepository
                .findById(request.siteId())
                .orElseThrow(() ->
                        new RuntimeException("Site not found"));

        // Site must belong to selected customer
        if (!site.getCustomer().getId()
                .equals(customer.getId())) {

            throw new RuntimeException(
                    "Site does not belong to selected customer"
            );
        }

        workOrder.setTitle(request.title());
        workOrder.setDescription(request.description());
        workOrder.setPriority(request.priority());
        workOrder.setSlaDueDate(request.slaDueDate());
        workOrder.setCustomer(customer);
        workOrder.setSite(site);

        // Update assignee if provided
        if (request.assigneeId() != null) {

            User assignee = userRepository
                    .findById(request.assigneeId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Assignee not found"
                            ));

            workOrder.setAssignee(assignee);

            // Don't change completed/closed/cancelled status
            if (workOrder.getStatus() == WorkOrderStatus.NEW) {
                workOrder.setStatus(WorkOrderStatus.ASSIGNED);
            }

        } else {

            workOrder.setAssignee(null);
        }

        WorkOrder updatedWorkOrder =
                workOrderRepository.save(workOrder);

        return toResponse(updatedWorkOrder);
    }

    // =========================
    // UPDATE STATUS
    // =========================
    public WorkOrderResponse updateStatus(
            Long id,
            WorkOrderStatus status) {

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found"));

        workOrder.setStatus(status);

        WorkOrder updatedWorkOrder =
                workOrderRepository.save(workOrder);

        return toResponse(updatedWorkOrder);
    }

    // =========================
    // ASSIGN WORK ORDER
    // =========================
    public WorkOrderResponse assignWorkOrder(
            Long id,
            Long assigneeId) {

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found"));

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() ->
                        new RuntimeException("Assignee not found"));

        workOrder.setAssignee(assignee);

        if (workOrder.getStatus() == WorkOrderStatus.NEW) {
            workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        }

        WorkOrder updatedWorkOrder =
                workOrderRepository.save(workOrder);

        return toResponse(updatedWorkOrder);
    }

    // =========================
    // DELETE WORK ORDER
    // =========================
    public void deleteWorkOrder(Long id) {

        if (!workOrderRepository.existsById(id)) {

            throw new RuntimeException(
                    "Work order not found"
            );
        }

        workOrderRepository.deleteById(id);
    }

    // =========================
    // GENERATE WORK ORDER CODE
    // =========================
    private String generateWorkOrderCode() {

        long nextNumber =
                workOrderRepository.count() + 1;

        String code;

        do {
            code = String.format(
                    "WO-%05d",
                    nextNumber++
            );
        } while (
                workOrderRepository.findByCode(code).isPresent()
        );

        return code;
    }

    // =========================
    // ENTITY → RESPONSE DTO
    // =========================
    private WorkOrderResponse toResponse(
            WorkOrder workOrder) {

        Long customerId =
                workOrder.getCustomer() != null
                        ? workOrder.getCustomer().getId()
                        : null;

        String customerName =
                workOrder.getCustomer() != null
                        ? workOrder.getCustomer().getCompanyName()
                        : null;

        Long siteId =
                workOrder.getSite() != null
                        ? workOrder.getSite().getId()
                        : null;

        String siteName =
                workOrder.getSite() != null
                        ? workOrder.getSite().getName()
                        : null;

        Long assigneeId =
                workOrder.getAssignee() != null
                        ? workOrder.getAssignee().getId()
                        : null;

        String assigneeEmail =
                workOrder.getAssignee() != null
                        ? workOrder.getAssignee().getEmail()
                        : null;

        return new WorkOrderResponse(
                workOrder.getId(),
                workOrder.getCode(),
                workOrder.getTitle(),
                workOrder.getDescription(),
                workOrder.getPriority(),
                workOrder.getStatus(),
                workOrder.getSlaDueDate(),
                customerId,
                customerName,
                siteId,
                siteName,
                assigneeId,
                assigneeEmail
        );
    }
}