package com.keystone.service;

import com.keystone.dto.WorkOrderHistoryResponse;
import com.keystone.dto.WorkOrderRequest;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.model.*;
import com.keystone.repository.*;
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

    private final WorkOrderRepository        workOrderRepository;
    private final CustomerRepository         customerRepository;
    private final SiteRepository             siteRepository;
    private final UserRepository             userRepository;
    private final WorkOrderHistoryRepository workOrderHistoryRepository;
    private final SlaService                 slaService;
    private final NotificationService        notificationService;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            CustomerRepository customerRepository,
            SiteRepository siteRepository,
            UserRepository userRepository,
            WorkOrderHistoryRepository workOrderHistoryRepository,
            SlaService slaService,
            NotificationService notificationService) {

        this.workOrderRepository        = workOrderRepository;
        this.customerRepository         = customerRepository;
        this.siteRepository             = siteRepository;
        this.userRepository             = userRepository;
        this.workOrderHistoryRepository = workOrderHistoryRepository;
        this.slaService                 = slaService;
        this.notificationService        = notificationService;
    }

    // -------------------------------------------------------
    // LIST ALL (MANAGER / DISPATCHER)
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> searchWorkOrders(
            String search,
            WorkOrderStatus status,
            String priority,
            Pageable pageable) {

        return doSearch(null, search, status, priority, pageable);
    }

    // -------------------------------------------------------
    // LIST — CUSTOMER PORTAL (scoped to customer)
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> searchCustomerWorkOrders(
            Long customerId,
            String search,
            WorkOrderStatus status,
            String priority,
            Pageable pageable) {

        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }

        return doSearch(customerId, search, status, priority, pageable);
    }

    // -------------------------------------------------------
    // LIST — TECHNICIAN (scoped to assignee)
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> searchTechnicianWorkOrders(
            Long assigneeId,
            String search,
            WorkOrderStatus status,
            String priority,
            Pageable pageable) {

        if (assigneeId == null) {
            throw new IllegalArgumentException("Assignee ID is required");
        }

        boolean hasSearch   = search != null && !search.isBlank();
        boolean hasStatus   = status != null;
        boolean hasPriority = priority != null && !priority.isBlank();

        String s = hasSearch
                ? search.trim()
                : "";

        String p = hasPriority
                ? priority.trim().toUpperCase()
                : "";

        Page<WorkOrder> result;

        if (hasSearch && hasStatus && hasPriority) {

            result = workOrderRepository
                    .findByAssignee_IdAndTitleContainingIgnoreCaseAndStatusAndPriority(
                            assigneeId,
                            s,
                            status,
                            p,
                            pageable
                    );

        } else if (hasSearch && hasStatus) {

            result = workOrderRepository
                    .findByAssignee_IdAndTitleContainingIgnoreCaseAndStatus(
                            assigneeId,
                            s,
                            status,
                            pageable
                    );

        } else if (hasSearch && hasPriority) {

            result = workOrderRepository
                    .findByAssignee_IdAndTitleContainingIgnoreCaseAndPriority(
                            assigneeId,
                            s,
                            p,
                            pageable
                    );

        } else if (hasStatus && hasPriority) {

            result = workOrderRepository
                    .findByAssignee_IdAndStatusAndPriority(
                            assigneeId,
                            status,
                            p,
                            pageable
                    );

        } else if (hasSearch) {

            result = workOrderRepository
                    .findByAssignee_IdAndTitleContainingIgnoreCase(
                            assigneeId,
                            s,
                            pageable
                    );

        } else if (hasStatus) {

            result = workOrderRepository
                    .findByAssignee_IdAndStatus(
                            assigneeId,
                            status,
                            pageable
                    );

        } else if (hasPriority) {

            result = workOrderRepository
                    .findByAssignee_IdAndPriority(
                            assigneeId,
                            p,
                            pageable
                    );

        } else {

            result = workOrderRepository
                    .findByAssignee_Id(
                            assigneeId,
                            pageable
                    );
        }

        return result.map(this::toResponse);
    }

    // -------------------------------------------------------
    // GET BY ID
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrderById(Long id) {

        return toResponse(findOrThrow(id));
    }

    // -------------------------------------------------------
    // GET BY CODE
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrderByCode(String code) {

        WorkOrder wo = workOrderRepository.findByCode(code)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found"));

        return toResponse(wo);
    }

    // -------------------------------------------------------
    // GET HISTORY
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public List<WorkOrderHistoryResponse> getWorkOrderHistory(
            Long workOrderId) {

        if (!workOrderRepository.existsById(workOrderId)) {
            throw new RuntimeException("Work order not found");
        }

        return workOrderHistoryRepository
                .findByWorkOrderIdOrderByChangedAtAsc(workOrderId)
                .stream()
                .map(h -> new WorkOrderHistoryResponse(
                        h.getId(),
                        h.getWorkOrder().getId(),
                        h.getFromStatus(),
                        h.getToStatus(),
                        h.getChangedBy() != null
                                ? h.getChangedBy().getId()
                                : null,
                        h.getChangedBy() != null
                                ? h.getChangedBy().getEmail()
                                : null,
                        h.getChangedAt(),
                        h.getNote()
                ))
                .toList();
    }

    // -------------------------------------------------------
    // CREATE
    // -------------------------------------------------------
    @Transactional
    public WorkOrderResponse createWorkOrder(
            WorkOrderRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Work order request is required"
            );
        }

        Customer customer =
                customerRepository.findById(request.customerId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                )
                        );

        Site site =
                siteRepository.findById(request.siteId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Site not found"
                                )
                        );

        validateSiteBelongsToCustomer(
                site,
                customer
        );

        WorkOrder wo = new WorkOrder();

        wo.setCode(
                generateWorkOrderCode()
        );

        wo.setTitle(
                request.title()
        );

        wo.setDescription(
                request.description()
        );

        wo.setPriority(
                request.priority()
        );

        wo.setCustomer(
                customer
        );

        wo.setSite(
                site
        );

        wo.setStatus(
                WorkOrderStatus.NEW
        );

        wo.setSlaDueDate(
                resolveSlaDate(
                        request.slaDueDate(),
                        request.priority()
                )
        );

        WorkOrderStatus initialStatus =
                WorkOrderStatus.NEW;

        // ---------------------------------------------------
        // OPTIONAL ASSIGNEE
        // ---------------------------------------------------
        if (request.assigneeId() != null) {

            User assignee =
                    resolveTechnician(
                            request.assigneeId()
                    );

            wo.setAssignee(
                    assignee
            );

            wo.setStatus(
                    WorkOrderStatus.ASSIGNED
            );

            initialStatus =
                    WorkOrderStatus.ASSIGNED;
        }

        WorkOrder saved =
                workOrderRepository.save(wo);

        // ---------------------------------------------------
        // HISTORY + NOTIFICATION
        // ---------------------------------------------------
        if (initialStatus == WorkOrderStatus.ASSIGNED) {

            saveHistory(
                    saved,
                    WorkOrderStatus.NEW,
                    WorkOrderStatus.ASSIGNED,
                    "Work order created and assigned"
            );

            notificationService
                    .notifyTechnicianOfAssignment(saved);

        } else {

            saveHistory(
                    saved,
                    null,
                    WorkOrderStatus.NEW,
                    "Work order created"
            );
        }

        return toResponse(saved);
    }

    // -------------------------------------------------------
    // UPDATE
    // FIX: null assigneeId DOES NOT clear existing assignee
    // -------------------------------------------------------
    @Transactional
    public WorkOrderResponse updateWorkOrder(
            Long id,
            WorkOrderRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Work order request is required"
            );
        }

        WorkOrder wo =
                findOrThrow(id);

        assertEditable(wo);

        Customer customer =
                customerRepository.findById(
                                request.customerId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                )
                        );

        Site site =
                siteRepository.findById(
                                request.siteId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Site not found"
                                )
                        );

        validateSiteBelongsToCustomer(
                site,
                customer
        );

        boolean priorityChanged =
                wo.getPriority() != request.priority();

        wo.setTitle(
                request.title()
        );

        wo.setDescription(
                request.description()
        );

        wo.setPriority(
                request.priority()
        );

        wo.setCustomer(
                customer
        );

        wo.setSite(
                site
        );

        // ---------------------------------------------------
        // SLA
        // ---------------------------------------------------
        if (request.slaDueDate() != null) {

            wo.setSlaDueDate(
                    request.slaDueDate()
            );

        } else if (priorityChanged) {

            wo.setSlaDueDate(
                    slaService.calculateDueDate(
                            request.priority().name()
                    )
            );
        }

        // ---------------------------------------------------
        // ASSIGNEE
        // Only update when explicitly provided.
        // null = keep existing.
        // ---------------------------------------------------
        if (request.assigneeId() != null) {

            User assignee =
                    resolveTechnician(
                            request.assigneeId()
                    );

            boolean reassigned =
                    wo.getAssignee() == null
                            || !wo.getAssignee()
                            .getId()
                            .equals(assignee.getId());

            wo.setAssignee(
                    assignee
            );

            // NEW -> ASSIGNED
            if (wo.getStatus()
                    == WorkOrderStatus.NEW) {

                WorkOrderStatus old =
                        wo.getStatus();

                wo.setStatus(
                        WorkOrderStatus.ASSIGNED
                );

                WorkOrder saved =
                        workOrderRepository.save(wo);

                saveHistory(
                        saved,
                        old,
                        WorkOrderStatus.ASSIGNED,
                        "Work order assigned during update"
                );

                if (reassigned) {

                    notificationService
                            .notifyTechnicianOfAssignment(
                                    saved
                            );
                }

                return toResponse(saved);
            }

            // REASSIGN EXISTING WORK ORDER
            if (reassigned) {

                WorkOrder saved =
                        workOrderRepository.save(wo);

                notificationService
                        .notifyTechnicianOfAssignment(
                                saved
                        );

                return toResponse(saved);
            }
        }

        // ---------------------------------------------------
        // NULL ASSIGNEE = KEEP EXISTING ASSIGNEE
        // ---------------------------------------------------

        return toResponse(
                workOrderRepository.save(wo)
        );
    }

    // -------------------------------------------------------
    // UPDATE STATUS
    // GUARDED STATE MACHINE
    // -------------------------------------------------------
    @Transactional
    public WorkOrderResponse updateStatus(
            Long id,
            WorkOrderStatus newStatus) {

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Status is required"
            );
        }

        WorkOrder wo =
                findOrThrow(id);

        WorkOrderStatus old =
                wo.getStatus();

        if (old == newStatus) {
            return toResponse(wo);
        }

        validateTransition(
                old,
                newStatus
        );

        wo.setStatus(
                newStatus
        );

        WorkOrder saved =
                workOrderRepository.save(wo);

        saveHistory(
                saved,
                old,
                newStatus,
                "Status changed"
        );

        return toResponse(saved);
    }

    // -------------------------------------------------------
    // ASSIGN
    // -------------------------------------------------------
    @Transactional
    public WorkOrderResponse assignWorkOrder(
            Long id,
            Long assigneeId) {

        WorkOrder wo =
                findOrThrow(id);

        if (wo.getStatus()
                == WorkOrderStatus.CLOSED
                || wo.getStatus()
                == WorkOrderStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Cannot assign a closed or cancelled work order"
            );
        }

        User assignee =
                resolveTechnician(
                        assigneeId
                );

        boolean reassigned =
                wo.getAssignee() == null
                        || !wo.getAssignee()
                        .getId()
                        .equals(assignee.getId());

        WorkOrderStatus old =
                wo.getStatus();

        wo.setAssignee(
                assignee
        );

        // NEW -> ASSIGNED
        if (old == WorkOrderStatus.NEW) {

            wo.setStatus(
                    WorkOrderStatus.ASSIGNED
            );
        }

        WorkOrder saved =
                workOrderRepository.save(wo);

        // ---------------------------------------------------
        // HISTORY
        // ---------------------------------------------------
        if (old != saved.getStatus()) {

            saveHistory(
                    saved,
                    old,
                    saved.getStatus(),
                    "Work order assigned to technician"
            );
        }

        // ---------------------------------------------------
        // NOTIFICATION
        // ---------------------------------------------------
        if (reassigned) {

            notificationService
                    .notifyTechnicianOfAssignment(
                            saved
                    );
        }

        return toResponse(saved);
    }

    // -------------------------------------------------------
    // DELETE
    // MANAGER ONLY — controller enforces role
    // -------------------------------------------------------
    @Transactional
    public void deleteWorkOrder(Long id) {

        if (!workOrderRepository.existsById(id)) {

            throw new RuntimeException(
                    "Work order not found"
            );
        }

        workOrderRepository.deleteById(id);
    }

    // -------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------

    private WorkOrder findOrThrow(Long id) {

        return workOrderRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Work order not found"
                        )
                );
    }

    private void assertEditable(
            WorkOrder wo) {

        if (wo.getStatus()
                == WorkOrderStatus.CLOSED
                || wo.getStatus()
                == WorkOrderStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Closed or cancelled work order cannot be edited"
            );
        }
    }

    private void validateSiteBelongsToCustomer(
            Site site,
            Customer customer) {

        if (site.getCustomer() == null
                || !site.getCustomer()
                .getId()
                .equals(customer.getId())) {

            throw new IllegalArgumentException(
                    "Site does not belong to the selected customer"
            );
        }
    }

    private User resolveTechnician(
            Long userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Assignee not found"
                                )
                        );

        if (user.getRole() != Role.TECHNICIAN) {

            throw new IllegalArgumentException(
                    "Selected user is not a technician"
            );
        }

        return user;
    }

    private LocalDateTime resolveSlaDate(
            LocalDateTime explicit,
            Priority priority) {

        if (explicit != null) {
            return explicit;
        }

        return slaService.calculateDueDate(
                priority.name()
        );
    }

    // -------------------------------------------------------
    // GUARDED STATE MACHINE
    // -------------------------------------------------------
    private void validateTransition(
            WorkOrderStatus from,
            WorkOrderStatus to) {

        boolean allowed =
                switch (from) {

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

        if (!allowed) {

            throw new IllegalStateException(
                    "Invalid status transition from "
                            + from
                            + " to "
                            + to
            );
        }
    }

    // -------------------------------------------------------
    // SAVE HISTORY
    // -------------------------------------------------------
    private void saveHistory(
            WorkOrder wo,
            WorkOrderStatus from,
            WorkOrderStatus to,
            String note) {

        WorkOrderHistory h =
                new WorkOrderHistory();

        h.setWorkOrder(
                wo
        );

        h.setFromStatus(
                from
        );

        h.setToStatus(
                to
        );

        h.setChangedAt(
                LocalDateTime.now()
        );

        h.setNote(
                note
        );

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && auth.getName() != null) {

            userRepository
                    .findByEmail(auth.getName())
                    .ifPresent(
                            h::setChangedBy
                    );
        }

        workOrderHistoryRepository.save(
                h
        );
    }

    // -------------------------------------------------------
    // GENERATE WORK ORDER CODE
    // -------------------------------------------------------
    private String generateWorkOrderCode() {

        long next =
                workOrderRepository.count() + 1;

        String code;

        do {

            code =
                    String.format(
                            "WO-%05d",
                            next++
                    );

        } while (
                workOrderRepository
                        .findByCode(code)
                        .isPresent()
        );

        return code;
    }

    // -------------------------------------------------------
    // CENTRAL SEARCH
    // customerId == null = all customers
    // -------------------------------------------------------
    private Page<WorkOrderResponse> doSearch(
            Long customerId,
            String search,
            WorkOrderStatus status,
            String priority,
            Pageable pageable) {

        boolean hasSearch =
                search != null
                        && !search.isBlank();

        boolean hasStatus =
                status != null;

        boolean hasPriority =
                priority != null
                        && !priority.isBlank();

        String s =
                hasSearch
                        ? search.trim()
                        : "";

        String p =
                hasPriority
                        ? priority.trim().toUpperCase()
                        : "";

        Page<WorkOrder> result;

        // ---------------------------------------------------
        // MANAGER / DISPATCHER
        // ---------------------------------------------------
        if (customerId == null) {

            if (hasSearch
                    && hasStatus
                    && hasPriority) {

                result =
                        workOrderRepository
                                .findByTitleContainingIgnoreCaseAndStatusAndPriority(
                                        s,
                                        status,
                                        p,
                                        pageable
                                );

            } else if (hasSearch
                    && hasStatus) {

                result =
                        workOrderRepository
                                .findByTitleContainingIgnoreCaseAndStatus(
                                        s,
                                        status,
                                        pageable
                                );

            } else if (hasSearch
                    && hasPriority) {

                result =
                        workOrderRepository
                                .findByTitleContainingIgnoreCaseAndPriority(
                                        s,
                                        p,
                                        pageable
                                );

            } else if (hasStatus
                    && hasPriority) {

                result =
                        workOrderRepository
                                .findByStatusAndPriority(
                                        status,
                                        p,
                                        pageable
                                );

            } else if (hasSearch) {

                result =
                        workOrderRepository
                                .findByTitleContainingIgnoreCase(
                                        s,
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
                                        p,
                                        pageable
                                );

            } else {

                result =
                        workOrderRepository
                                .findAll(pageable);
            }

        } else {

            // ------------------------------------------------
            // CUSTOMER PORTAL — CUSTOMER SCOPED
            // ------------------------------------------------

            if (hasSearch
                    && hasStatus
                    && hasPriority) {

                result =
                        workOrderRepository
                                .findByCustomer_IdAndTitleContainingIgnoreCaseAndStatusAndPriority(
                                        customerId,
                                        s,
                                        status,
                                        p,
                                        pageable
                                );

            } else if (hasSearch
                    && hasStatus) {

                result =
                        workOrderRepository
                                .findByCustomer_IdAndTitleContainingIgnoreCaseAndStatus(
                                        customerId,
                                        s,
                                        status,
                                        pageable
                                );

            } else if (hasSearch
                    && hasPriority) {

                result =
                        workOrderRepository
                                .findByCustomer_IdAndTitleContainingIgnoreCaseAndPriority(
                                        customerId,
                                        s,
                                        p,
                                        pageable
                                );

            } else if (hasStatus
                    && hasPriority) {

                result =
                        workOrderRepository
                                .findByCustomer_IdAndStatusAndPriority(
                                        customerId,
                                        status,
                                        p,
                                        pageable
                                );

            } else if (hasSearch) {

                result =
                        workOrderRepository
                                .findByCustomer_IdAndTitleContainingIgnoreCase(
                                        customerId,
                                        s,
                                        pageable
                                );

            } else if (hasStatus) {

                result =
                        workOrderRepository
                                .findByCustomer_IdAndStatus(
                                        customerId,
                                        status,
                                        pageable
                                );

            } else if (hasPriority) {

                result =
                        workOrderRepository
                                .findByCustomer_IdAndPriority(
                                        customerId,
                                        p,
                                        pageable
                                );

            } else {

                result =
                        workOrderRepository
                                .findByCustomer_Id(
                                        customerId,
                                        pageable
                                );
            }
        }

        return result.map(this::toResponse);
    }

    // -------------------------------------------------------
    // ENTITY -> RESPONSE DTO
    // -------------------------------------------------------
    public WorkOrderResponse toResponse(
            WorkOrder wo) {

        return new WorkOrderResponse(

                wo.getId(),

                wo.getCode(),

                wo.getTitle(),

                wo.getDescription(),

                wo.getPriority(),

                wo.getStatus(),

                wo.getSlaDueDate(),

                wo.getCustomer() != null
                        ? wo.getCustomer().getId()
                        : null,

                wo.getCustomer() != null
                        ? wo.getCustomer().getCompanyName()
                        : null,

                wo.getSite() != null
                        ? wo.getSite().getId()
                        : null,

                wo.getSite() != null
                        ? wo.getSite().getName()
                        : null,

                wo.getAssignee() != null
                        ? wo.getAssignee().getId()
                        : null,

                wo.getAssignee() != null
                        ? wo.getAssignee().getEmail()
                        : null
        );
    }
}