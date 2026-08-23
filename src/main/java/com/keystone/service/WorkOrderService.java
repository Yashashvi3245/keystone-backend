package com.keystone.service;

import com.keystone.model.Customer;
import com.keystone.model.Site;
import com.keystone.model.User;
import com.keystone.model.WorkOrder;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public List<WorkOrder> getAllWorkOrders() {
        return workOrderRepository.findAll();
    }

    public Optional<WorkOrder> getWorkOrderById(Long id) {
        return workOrderRepository.findById(id);
    }

    public Optional<WorkOrder> getWorkOrderByCode(String code) {
        return workOrderRepository.findByCode(code);
    }

    public WorkOrder createWorkOrder(
            WorkOrder workOrder,
            Long customerId,
            Long siteId,
            Long assigneeId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() ->
                        new RuntimeException("Site not found"));

        workOrder.setCustomer(customer);
        workOrder.setSite(site);

        if (assigneeId != null) {
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() ->
                            new RuntimeException("Assignee not found"));

            workOrder.setAssignee(assignee);
        }

        return workOrderRepository.save(workOrder);
    }

    public void deleteWorkOrder(Long id) {
        workOrderRepository.deleteById(id);
    }
}