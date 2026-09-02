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

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            CustomerRepository customerRepository,
            SiteRepository siteRepository,
            UserRepository userRepository,
            WorkOrderHistoryRepository workOrderHistoryRepository) {

        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
        this.workOrderHistoryRepository = workOrderHistoryRepository;
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
    // SEARCH / FILTER /
    // PAGINATION
    // =========================
    public Page<WorkOrderResponse> searchWorkOrders(
            String search,
            WorkOrderStatus status,
            String priority,
            Pageable pageable) {

        boolean hasSearch =
                search != null && !search.trim().isEmpty();

        boolean hasStatus = status != null;

        boolean hasPriority =
                priority != null && !priority.trim().isEmpty();

        String cleanSearch =
                hasSearch ? search.trim() : "";

        String cleanPriority =
                hasPriority ? priority.trim() : "";

        Page<WorkOrder> workOrders;

        // Search + Status + Priority
        if (hasSearch && hasStatus && hasPriority) {

            workOrders =
                    workOrderRepository
                            .findByTitleContainingIgnoreCaseAndStatusAndPriority(
                                    cleanSearch,
                                    status,
                                    cleanPriority,
                                    pageable
                            );

            // Search + Status
        } else if (hasSearch && hasStatus) {

            workOrders =
                    workOrderRepository
                            .findByTitleContainingIgnoreCaseAndStatus(
                                    cleanSearch,
                                    status,
                                    pageable
                            );

            // Search + Priority
        } else if (hasSearch && hasPriority) {

            workOrders =
                    workOrderRepository
                            .findByTitleContainingIgnoreCaseAndPriority(
                                    cleanSearch,
                                    cleanPriority,
                                    pageable
                            );

            // Status + Priority
        } else if (hasStatus && hasPriority) {

            workOrders =
                    workOrderRepository
                            .findByStatusAndPriority(
                                    status,
                                    cleanPriority,
                                    pageable
                            );

            // Search only
        } else if (hasSearch) {

            workOrders =
                    workOrderRepository
                            .findByTitleContainingIgnoreCase(
                                    cleanSearch,
                                    pageable
                            );

            // Status only
        } else if (hasStatus) {

            workOrders =
                    workOrderRepository
                            .findByStatus(
                                    status,
                                    pageable
                            );

            // Priority only
        } else if (hasPriority) {

            workOrders =
                    workOrderRepository
                            .findByPriority(
                                    cleanPriority,
                                    pageable
                            );

            // No filters
        } else {

            workOrders =
                    workOrderRepository.findAll(pageable);
        }

        return workOrders.map(this::toResponse);
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
                .findByWorkOrderIdOrderByChangedAtAsc(workOrderId)
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

        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        Site site = siteRepository
                .findById(request.siteId())
                .orElseThrow(() ->
                        new RuntimeException("Site not found"));

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

        if (request.assigneeId() != null) {

            saveHistory(
                    savedWorkOrder,
                    WorkOrderStatus.NEW,
                    WorkOrderStatus.ASSIGNED,
                    "Work order assigned during creation"
            );
        }

        return toResponse(savedWorkOrder);
    }

    // =========================
    // UPDATE WORK ORDER
    // =========================
    @Transactional
    public WorkOrderResponse updateWorkOrder(
            Long id,
            WorkOrderRequest request) {

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found"));

        if (workOrder.getStatus() == WorkOrderStatus.CLOSED
                || workOrder.getStatus() == WorkOrderStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Closed or cancelled work orders cannot be edited"
            );
        }

        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        Site site = siteRepository
                .findById(request.siteId())
                .orElseThrow(() ->
                        new RuntimeException("Site not found"));

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

        if (request.assigneeId() != null) {

            User assignee = userRepository
                    .findById(request.assigneeId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Assignee not found"
                            ));

            workOrder.setAssignee(assignee);

            if (workOrder.getStatus() == WorkOrderStatus.NEW) {

                WorkOrderStatus oldStatus =
                        workOrder.getStatus();

                workOrder.setStatus(
                        WorkOrderStatus.ASSIGNED
                );

                WorkOrder updatedWorkOrder =
                        workOrderRepository.save(workOrder);

                saveHistory(
                        updatedWorkOrder,
                        oldStatus,
                        WorkOrderStatus.ASSIGNED,
                        "Work order assigned during update"
                );

                return toResponse(updatedWorkOrder);
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
    @Transactional
    public WorkOrderResponse updateStatus(
            Long id,
            WorkOrderStatus status) {

        if (status == null) {

            throw new IllegalArgumentException(
                    "Status cannot be null"
            );
        }

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found"));

        WorkOrderStatus oldStatus =
                workOrder.getStatus();

        if (oldStatus == status) {
            return toResponse(workOrder);
        }

        if (!isAllowedTransition(oldStatus, status)) {

            throw new IllegalStateException(
                    "Invalid work order status transition: "
                            + oldStatus
                            + " -> "
                            + status
            );
        }

        workOrder.setStatus(status);

        WorkOrder updatedWorkOrder =
                workOrderRepository.save(workOrder);

        saveHistory(
                updatedWorkOrder,
                oldStatus,
                status,
                "Work order status changed"
        );

        return toResponse(updatedWorkOrder);
    }

    // =========================
    // GUARDED STATUS TRANSITIONS
    // =========================
    private boolean isAllowedTransition(
            WorkOrderStatus from,
            WorkOrderStatus to) {

        return switch (from) {

            case NEW ->
                    to == WorkOrderStatus.ASSIGNED
                            || to == WorkOrderStatus.CANCELLED;

            case ASSIGNED ->
                    to == WorkOrderStatus.IN_PROGRESS
                            || to == WorkOrderStatus.CANCELLED;

            case IN_PROGRESS ->
                    to == WorkOrderStatus.ON_HOLD
                            || to == WorkOrderStatus.COMPLETED;

            case ON_HOLD ->
                    to == WorkOrderStatus.IN_PROGRESS
                            || to == WorkOrderStatus.CANCELLED;

            case COMPLETED ->
                    to == WorkOrderStatus.CLOSED;

            case CLOSED, CANCELLED ->
                    false;
        };
    }

    // =========================
    // ASSIGN WORK ORDER
    // =========================
    @Transactional
    public WorkOrderResponse assignWorkOrder(
            Long id,
            Long assigneeId) {

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found"));

        if (workOrder.getStatus() == WorkOrderStatus.CLOSED
                || workOrder.getStatus() == WorkOrderStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Closed or cancelled work orders cannot be assigned"
            );
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() ->
                        new RuntimeException("Assignee not found"));

        workOrder.setAssignee(assignee);

        WorkOrderStatus oldStatus =
                workOrder.getStatus();

        if (oldStatus == WorkOrderStatus.NEW) {

            workOrder.setStatus(
                    WorkOrderStatus.ASSIGNED
            );
        }

        WorkOrder updatedWorkOrder =
                workOrderRepository.save(workOrder);

        if (oldStatus != updatedWorkOrder.getStatus()) {

            saveHistory(
                    updatedWorkOrder,
                    oldStatus,
                    updatedWorkOrder.getStatus(),
                    "Work order assigned to technician"
            );
        }

        return toResponse(updatedWorkOrder);
    }

    // =========================
    // DELETE WORK ORDER
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
    // SAVE STATUS HISTORY
    // =========================
    private void saveHistory(
            WorkOrder workOrder,
            WorkOrderStatus fromStatus,
            WorkOrderStatus toStatus,
            String note) {

        WorkOrderHistory history =
                new WorkOrderHistory();

        history.setWorkOrder(workOrder);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedAt(LocalDateTime.now());
        history.setNote(note);

        /*
         * changedBy will be connected to the authenticated
         * user later.
         */
        history.setChangedBy(null);

        workOrderHistoryRepository.save(history);
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