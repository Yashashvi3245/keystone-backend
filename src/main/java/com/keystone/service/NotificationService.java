package com.keystone.service;

import com.keystone.model.Notification;
import com.keystone.model.User;
import com.keystone.model.WorkOrder;
import com.keystone.repository.NotificationRepository;
import com.keystone.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------
    // Task 10: Notify technician on assignment
    // -------------------------------------------------------
    @Transactional
    public void notifyTechnicianOfAssignment(WorkOrder workOrder) {

        if (workOrder == null || workOrder.getAssignee() == null) return;

        User technician = workOrder.getAssignee();

        String message = "You have been assigned to work order "
                + workOrder.getCode() + " — " + workOrder.getTitle();

        boolean alreadyNotified = notificationRepository
                .existsByUserIdAndMessage(technician.getId(), message);

        if (!alreadyNotified) {
            Notification n = new Notification();
            n.setUser(technician);
            n.setMessage(message);
            n.setRead(false);
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void notifyManagersOfSlaBreach(
            WorkOrder workOrder) {

        if (workOrder == null) {
            return;
        }

        List<User> managers =
                userRepository.findAll()
                        .stream()
                        .filter(user ->
                                user.getRole() != null
                                        && "MANAGER".equalsIgnoreCase(
                                        user.getRole().name()
                                )
                        )
                        .toList();

        String message =
                "SLA breached for work order "
                        + workOrder.getCode()
                        + " - "
                        + workOrder.getTitle();

        for (User manager : managers) {

            boolean alreadyNotified =
                    notificationRepository
                            .existsByUserIdAndMessage(
                                    manager.getId(),
                                    message
                            );

            if (alreadyNotified) {
                continue;
            }

            Notification notification =
                    new Notification();

            notification.setUser(manager);
            notification.setMessage(message);
            notification.setRead(false);
            notification.setCreatedAt(
                    LocalDateTime.now()
            );

            notificationRepository.save(
                    notification
            );
        }
    }

    public List<Notification> getNotifications(
            Long userId) {

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                );
    }

    public List<Notification> getUnreadNotifications(
            Long userId) {

        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(
                        userId
                );
    }

    public long getUnreadCount(
            Long userId) {

        return notificationRepository
                .countByUserIdAndReadFalse(
                        userId
                );
    }

    @Transactional
    public Notification markAsRead(
            Long notificationId,
            Long userId) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                ));

        if (notification.getUser() == null
                || !notification.getUser()
                .getId()
                .equals(userId)) {

            throw new RuntimeException(
                    "You are not authorized to update this notification"
            );
        }

        notification.setRead(true);

        return notificationRepository.save(
                notification
        );
    }
}