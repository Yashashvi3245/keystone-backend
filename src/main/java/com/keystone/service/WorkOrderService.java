package com.keystone.service;

import com.keystone.dto.WorkOrderHistoryResponse;
import com.keystone.dto.WorkOrderRequest;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.model.Customer;
import com.keystone.model.Site;
import com.keystone.model.User;
import com.keystone.model.WorkOrder;
import com.keystone.model.WorkOrderHistory;
import com.keystone.model.WorkOrderStatus;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderHistoryRepository;
import com.keystone.repository.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final WorkOrderHistoryRepository workOrderHistoryRepository;
    private final SlaService slaService;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            CustomerRepository customerRepository,
            SiteRepository siteRepository,
            UserRepository userRepository,
            WorkOrderHistoryRepository workOrderHistoryRepository,
            SlaService slaService) {

        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
        this.workOrderHistoryRepository = workOrderHistoryRepository;
        this.slaService = slaService;
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
    // SEARCH / FILTER / PAGINATION
    // =========================
    public Page<WorkOrderResponse> searchWorkOrders(
            String search,
            WorkOrderStatus status,
            String priority,
            Pageable pageable) {

        boolean hasSearch =
                search != null && !search.trim().isEmpty();

        boolean hasStatus =
                status != null;

        boolean hasPriority =
                priority != null && !priority.trim().isEmpty();

        String cleanSearch =
                hasSearch ? search.trim() : "";

        String cleanPriority =
                hasPriority
                        ? priority.trim().toUpperCase()
                        : "";

        Page<WorkOrder> result;

        if (hasSearch && hasStatus && hasPriority) {

            result =
                    workOrderRepository
                            .findByTitleContainingIgnoreCaseAndStatusAndPriority(
                                    cleanSearch,
                                    status,
                                    cleanPriority,
                                    pageable
                            );

        } else if (hasSearch && hasStatus) {

            result =
                    workOrderRepository
                            .findByTitleContainingIgnoreCaseAndStatus(
                                    cleanSearch,
                                    status,
                                    pageable
                            );

        } else if (hasSearch && hasPriority) {

            result =
                    workOrderRepository
                            .findByTitleContainingIgnoreCaseAndPriority(
                                    cleanSearch,
                                    cleanPriority,
                                    pageable
                            );

        } else if (hasStatus && hasPriority) {

            result =
                    workOrderRepository
                            .findByStatusAndPriority(
                                    status,
                                    cleanPriority,
                                    pageable
                            );

        } else if (hasSearch) {

            result =
                    workOrderRepository
                            .findByTitleContainingIgnoreCase(
                                    cleanSearch,
                                    pageable
                            );

        } else if (hasStatus) {

            result =
                    workOrderRepository
                            .findByStatus(
                                    status,
                                    pageable
                            );

        } else if (hasPriority) {

            result =
                    workOrderRepository
                            .findByPriority(
                                    cleanPriority,
                                    pageable
                            );

        } else {

            result =
                    workOrderRepository.findAll(pageable);
        }

        return result.map(this::toResponse);
    }

    // =========================
    // GET BY ID
    // =========================
    public WorkOrderResponse getWorkOrderById(Long id) {

        WorkOrder workOrder =
                workOrderRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Work order not found"
                                ));

        return toResponse(workOrder);
    }

    // =========================
    // GET BY CODE
    // =========================
    public WorkOrderResponse getWorkOrderByCode(
            String code) {

        WorkOrder workOrder =
                workOrderRepository.findByCode(code)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Work order not found"
                                ));

        return toResponse(workOrder);
    }

    // =========================
    // GET WORK ORDER HISTORY
    // =========================
    public List<WorkOrderHistoryResponse> getWorkOrderHistory(
            Long workOrderId) {

        if (!workOrderRepository.existsById(workOrderId)) {

            throw new RuntimeException(
                    "Work order not found"
            );
        }

        return workOrderHistoryRepository
                .findByWorkOrderIdOrderByChangedAtAsc(
                        workOrderId
                )
                .stream()
                .map(history -> {

                    Long changedById =
                            history.getChangedBy() != null
                                    ? history.getChangedBy().getId()
                                    : null;

                    String changedByEmail =
                            history.getChangedBy() != null
                                    ? history.getChangedBy().getEmail()
                                    : null;

                    return new WorkOrderHistoryResponse(
                            history.getId(),
                            history.getWorkOrder().getId(),
                            history.getFromStatus(),
                            history.getToStatus(),
                            changedById,
                            changedByEmail,
                            history.getChangedAt(),
                            history.getNote()
                    );
                })
                .toList();
    }

    // =========================
    // CREATE WORK ORDER
    // =========================
    @Transactional
    public WorkOrderResponse createWorkOrder(
            WorkOrderRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Work order request is required"
            );
        }

        Customer customer =
                customerRepository
                        .findById(request.customerId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                ));

        Site site =
                siteRepository
                        .findById(request.siteId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Site not found"
                                ));

        // =========================
        // SITE / CUSTOMER VALIDATION
        // =========================
        if (site.getCustomer() == null
                || !site.getCustomer()
                .getId()
                .equals(customer.getId())) {

            throw new IllegalArgumentException(
                    "Site does not belong to selected customer"
            );
        }

        // =========================
        // BASIC VALIDATION
        // =========================
        if (request.title() == null
                || request.title().isBlank()) {

            throw new IllegalArgumentException(
                    "Title is required"
            );
        }

        if (request.description() == null
                || request.description().isBlank()) {

            throw new IllegalArgumentException(
                    "Description is required"
            );
        }

        if (request.priority() == null) {

            throw new IllegalArgumentException(
                    "Priority is required"
            );
        }

        WorkOrder workOrder =
                new WorkOrder();

        // =========================
        // BASIC DATA
        // =========================
        workOrder.setCode(
                generateWorkOrderCode()
        );

        workOrder.setTitle(
                request.title()
        );

        workOrder.setDescription(
                request.description()
        );

        workOrder.setPriority(
                request.priority()
        );

        workOrder.setCustomer(
                customer
        );

        workOrder.setSite(
                site
        );

        // =========================
        // DEFAULT STATUS
        // =========================
        workOrder.setStatus(
                WorkOrderStatus.NEW
        );

        // =========================
        // SLA
        // =========================
        if (request.slaDueDate() != null) {

            workOrder.setSlaDueDate(
                    request.slaDueDate()
            );

        } else {

            workOrder.setSlaDueDate(
                    slaService.calculateDueDate(
                            request.priority().name()
                    )
            );
        }

        // =========================
        // OPTIONAL ASSIGNEE
        // =========================
        if (request.assigneeId() != null) {

            User assignee =
                    userRepository
                            .findById(
                                    request.assigneeId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Assignee not found"
                                    ));

            if (assignee.getRole() == null
                    || !"TECHNICIAN".equalsIgnoreCase(
                    assignee.getRole().name())) {

                throw new IllegalArgumentException(
                        "Selected user is not a technician"
                );
            }

            workOrder.setAssignee(
                    assignee
            );

            workOrder.setStatus(
                    WorkOrderStatus.ASSIGNED
            );
        }

        WorkOrder savedWorkOrder =
                workOrderRepository.save(
                        workOrder
                );

        // =========================
        // INITIAL HISTORY
        // =========================
        if (request.assigneeId() != null) {

            saveHistory(
                    savedWorkOrder,
                    WorkOrderStatus.NEW,
                    WorkOrderStatus.ASSIGNED,
                    "Work order assigned during creation"
            );

        } else {

            saveHistory(
                    savedWorkOrder,
                    null,
                    WorkOrderStatus.NEW,
                    "Work order created"
            );
        }

        return toResponse(
                savedWorkOrder
        );
    }

    // =========================
    // UPDATE WORK ORDER
    // =========================
    @Transactional
    public WorkOrderResponse updateWorkOrder(
            Long id,
            WorkOrderRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Work order request is required"
            );
        }

        WorkOrder workOrder =
                workOrderRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Work order not found"
                                ));

        // =========================
        // IMMUTABILITY
        // =========================
        if (workOrder.getStatus()
                == WorkOrderStatus.CLOSED
                || workOrder.getStatus()
                == WorkOrderStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Closed or cancelled work order cannot be edited"
            );
        }

        Customer customer =
                customerRepository
                        .findById(request.customerId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                ));

        Site site =
                siteRepository
                        .findById(request.siteId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Site not found"
                                ));

        // =========================
        // SITE / CUSTOMER VALIDATION
        // =========================
        if (site.getCustomer() == null
                || !site.getCustomer()
                .getId()
                .equals(customer.getId())) {

            throw new IllegalArgumentException(
                    "Site does not belong to selected customer"
            );
        }

        // =========================
        // BASIC VALIDATION
        // =========================
        if (request.title() == null
                || request.title().isBlank()) {

            throw new IllegalArgumentException(
                    "Title is required"
            );
        }

        if (request.description() == null
                || request.description().isBlank()) {

            throw new IllegalArgumentException(
                    "Description is required"
            );
        }

        if (request.priority() == null) {

            throw new IllegalArgumentException(
                    "Priority is required"
            );
        }

        // =========================
        // CHECK PRIORITY CHANGE
        // =========================
        boolean priorityChanged =
                workOrder.getPriority() != request.priority();

        // =========================
        // UPDATE DATA
        // =========================
        workOrder.setTitle(
                request.title()
        );

        workOrder.setDescription(
                request.description()
        );

        workOrder.setPriority(
                request.priority()
        );

        workOrder.setCustomer(
                customer
        );

        workOrder.setSite(
                site
        );

        // =========================
        // SLA
        // =========================
        if (request.slaDueDate() != null) {

            workOrder.setSlaDueDate(
                    request.slaDueDate()
            );

        } else if (priorityChanged) {

            workOrder.setSlaDueDate(
                    slaService.calculateDueDate(
                            request.priority().name()
                    )
            );
        }

        // =========================
        // ASSIGNEE
        // =========================
        if (request.assigneeId() != null) {

            User assignee =
                    userRepository
                            .findById(
                                    request.assigneeId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Assignee not found"
                                    ));

            if (assignee.getRole() == null
                    || !"TECHNICIAN".equalsIgnoreCase(
                    assignee.getRole().name())) {

                throw new IllegalArgumentException(
                        "Selected user is not a technician"
                );
            }

            workOrder.setAssignee(
                    assignee
            );

            // NEW -> ASSIGNED
            if (workOrder.getStatus()
                    == WorkOrderStatus.NEW) {

                WorkOrderStatus oldStatus =
                        workOrder.getStatus();

                workOrder.setStatus(
                        WorkOrderStatus.ASSIGNED
                );

                WorkOrder updated =
                        workOrderRepository.save(
                                workOrder
                        );

                saveHistory(
                        updated,
                        oldStatus,
                        WorkOrderStatus.ASSIGNED,
                        "Work order assigned during update"
                );

                return toResponse(updated);
            }

        } else {

            workOrder.setAssignee(null);
        }

        WorkOrder updatedWorkOrder =
                workOrderRepository.save(
                        workOrder
                );

        return toResponse(
                updatedWorkOrder
        );
    }

    // =========================
    // UPDATE STATUS
    // =========================
    @Transactional
    public WorkOrderResponse updateStatus(
            Long id,
            WorkOrderStatus status) {

        if (status == null) {

            throw new IllegalArgumentException(
                    "Status is required"
            );
        }

        WorkOrder workOrder =
                workOrderRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Work order not found"
                                ));

        WorkOrderStatus oldStatus =
                workOrder.getStatus();

        if (oldStatus == status) {
            return toResponse(workOrder);
        }

        // =========================
        // GUARDED STATE MACHINE
        // =========================
        validateTransition(
                oldStatus,
                status
        );

        workOrder.setStatus(
                status
        );

        WorkOrder updatedWorkOrder =
                workOrderRepository.save(
                        workOrder
                );

        saveHistory(
                updatedWorkOrder,
                oldStatus,
                status,
                "Status changed"
        );

        return toResponse(
                updatedWorkOrder
        );
    }

    // =========================
    // ASSIGN WORK ORDER
    // =========================
    @Transactional
    public WorkOrderResponse assignWorkOrder(
            Long id,
            Long assigneeId) {

        WorkOrder workOrder =
                workOrderRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Work order not found"
                                ));

        // Cannot assign closed/cancelled
        if (workOrder.getStatus()
                == WorkOrderStatus.CLOSED
                || workOrder.getStatus()
                == WorkOrderStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Cannot assign closed or cancelled work order"
            );
        }

        User assignee =
                userRepository
                        .findById(assigneeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Assignee not found"
                                ));

        // Must be technician
        if (assignee.getRole() == null
                || !"TECHNICIAN".equalsIgnoreCase(
                assignee.getRole().name())) {

            throw new IllegalArgumentException(
                    "Selected user is not a technician"
            );
        }

        WorkOrderStatus oldStatus =
                workOrder.getStatus();

        workOrder.setAssignee(
                assignee
        );

        // NEW -> ASSIGNED
        if (oldStatus == WorkOrderStatus.NEW) {

            workOrder.setStatus(
                    WorkOrderStatus.ASSIGNED
            );
        }

        WorkOrder updatedWorkOrder =
                workOrderRepository.save(
                        workOrder
                );

        // Save history only if status changed
        if (oldStatus != updatedWorkOrder.getStatus()) {

            saveHistory(
                    updatedWorkOrder,
                    oldStatus,
                    updatedWorkOrder.getStatus(),
                    "Work order assigned to technician"
            );
        }

        return toResponse(
                updatedWorkOrder
        );
    }

    // =========================
    // DELETE
    // =========================
    @Transactional
    public void deleteWorkOrder(Long id) {

        if (!workOrderRepository.existsById(id)) {

            throw new RuntimeException(
                    "Work order not found"
            );
        }

        workOrderRepository.deleteById(id);
    }

    // =========================
    // STATE MACHINE
    // =========================
    private void validateTransition(
            WorkOrderStatus from,
            WorkOrderStatus to) {

        if (from == null) {
            return;
        }

        boolean allowed;

        switch (from) {

            case NEW:

                allowed =
                        to == WorkOrderStatus.ASSIGNED
                                || to == WorkOrderStatus.CANCELLED;

                break;

            case ASSIGNED:

                allowed =
                        to == WorkOrderStatus.IN_PROGRESS
                                || to == WorkOrderStatus.CANCELLED;

                break;

            case IN_PROGRESS:

                allowed =
                        to == WorkOrderStatus.ON_HOLD
                                || to == WorkOrderStatus.COMPLETED;

                break;

            case ON_HOLD:

                allowed =
                        to == WorkOrderStatus.IN_PROGRESS
                                || to == WorkOrderStatus.CANCELLED;

                break;

            case COMPLETED:

                allowed =
                        to == WorkOrderStatus.CLOSED;

                break;

            case CLOSED:

            case CANCELLED:

                allowed = false;

                break;

            default:

                allowed = false;
        }

        if (!allowed) {

            throw new IllegalStateException(
                    "Invalid status transition from "
                            + from
                            + " to "
                            + to
            );
        }
    }

    // =========================
    // SAVE STATUS HISTORY
    // =========================
    private void saveHistory(
            WorkOrder workOrder,
            WorkOrderStatus fromStatus,
            WorkOrderStatus toStatus,
            String note) {

        WorkOrderHistory history =
                new WorkOrderHistory();

        history.setWorkOrder(
                workOrder
        );

        history.setFromStatus(
                fromStatus
        );

        history.setToStatus(
                toStatus
        );

        history.setChangedAt(
                LocalDateTime.now()
        );

        history.setNote(
                note
        );

        // =========================
        // CURRENT AUTHENTICATED USER
        // =========================
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null) {

            String email =
                    authentication.getName();

            userRepository
                    .findByEmail(email)
                    .ifPresent(history::setChangedBy);
        }

        workOrderHistoryRepository.save(
                history
        );
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
                workOrderRepository
                        .findByCode(code)
                        .isPresent()
        );

        return code;
    }

    // =========================
    // ENTITY -> RESPONSE DTO
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