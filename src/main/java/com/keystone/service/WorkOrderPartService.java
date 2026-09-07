package com.keystone.service;

import com.keystone.model.Part;
import com.keystone.model.User;
import com.keystone.model.WorkOrder;
import com.keystone.model.WorkOrderPart;
import com.keystone.repository.PartRepository;
import com.keystone.repository.WorkOrderPartRepository;
import com.keystone.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkOrderPartService {

    private final WorkOrderPartRepository workOrderPartRepository;
    private final PartRepository partRepository;
    private final WorkOrderRepository workOrderRepository;

    public WorkOrderPartService(
            WorkOrderPartRepository workOrderPartRepository,
            PartRepository partRepository,
            WorkOrderRepository workOrderRepository) {

        this.workOrderPartRepository = workOrderPartRepository;
        this.partRepository = partRepository;
        this.workOrderRepository = workOrderRepository;
    }

    // =========================
    // ADD PART TO WORK ORDER
    // =========================
    @Transactional
    public WorkOrderPart addPart(
            Long workOrderId,
            Long partId,
            Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        WorkOrder workOrder =
                workOrderRepository.findById(workOrderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Work order not found"
                                ));

        Part part =
                partRepository.findById(partId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Part not found"
                                ));

        if (workOrder.getAssignee() == null) {
            throw new IllegalStateException(
                    "Work order is not assigned to a technician"
            );
        }

        if (part.getStockQuantity() == null) {
            throw new IllegalStateException(
                    "Part stock quantity is not available"
            );
        }

        if (part.getStockQuantity() < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock for part: "
                            + part.getName()
            );
        }

        // =========================
        // DECREMENT STOCK
        // =========================
        part.setStockQuantity(
                part.getStockQuantity() - quantity
        );

        partRepository.save(part);

        // =========================
        // SAVE PART USAGE
        // =========================
        WorkOrderPart workOrderPart =
                new WorkOrderPart();

        workOrderPart.setWorkOrder(workOrder);
        workOrderPart.setPart(part);
        workOrderPart.setQuantity(quantity);
        workOrderPart.setUnitPrice(part.getUnitPrice());

        return workOrderPartRepository.save(
                workOrderPart
        );
    }

    // =========================
    // GET PARTS FOR WORK ORDER
    // =========================
    public List<WorkOrderPart> getPartsForWorkOrder(
            Long workOrderId) {

        if (!workOrderRepository.existsById(workOrderId)) {
            throw new RuntimeException(
                    "Work order not found"
            );
        }

        return workOrderPartRepository
                .findByWorkOrderId(workOrderId);
    }

    // =========================
    // CREATE PART
    // =========================
    @Transactional
    public Part createPart(
            String name,
            Integer stockQuantity,
            Integer unitPrice) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Part name is required"
            );
        }

        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative"
            );
        }

        if (unitPrice == null || unitPrice < 0) {
            throw new IllegalArgumentException(
                    "Unit price cannot be negative"
            );
        }

        if (partRepository
                .findByNameIgnoreCase(name.trim())
                .isPresent()) {

            throw new IllegalStateException(
                    "Part already exists"
            );
        }

        Part part = new Part();

        part.setName(name.trim());
        part.setStockQuantity(stockQuantity);
        part.setUnitPrice(unitPrice);

        return partRepository.save(part);
    }

    // =========================
    // GET ALL PARTS
    // =========================
    public List<Part> getAllParts() {

        return partRepository.findAll();
    }
}