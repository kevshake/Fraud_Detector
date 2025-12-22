package com.posgateway.aml.service.case_management;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.model.CasePriority;
import com.posgateway.aml.model.UserRole;
import com.posgateway.aml.model.CaseStatus;
import com.posgateway.aml.repository.ComplianceCaseRepository;
import com.posgateway.aml.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Case Assignment Service
 * Handles automatic and manual case assignment with workload balancing
 */
@Service
public class CaseAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(CaseAssignmentService.class);

    private final ComplianceCaseRepository caseRepository;
    private final UserRepository userRepository;

    @Value("${case.assignment.max-cases-per-user:20}")
    private int maxCasesPerUser;

    @Autowired
    public CaseAssignmentService(ComplianceCaseRepository caseRepository,
                                 UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
    }

    /**
     * Assign case automatically based on workload
     */
    @Transactional
    public User assignCaseByWorkload(ComplianceCase complianceCase, UserRole requiredRole) {
        List<User> availableUsers = userRepository.findByRole_NameAndEnabled(requiredRole.name(), true);

        if (availableUsers.isEmpty()) {
            throw new IllegalStateException("No users available for role: " + requiredRole);
        }

        // Calculate workload for each user
        Map<User, Integer> workloads = availableUsers.stream()
                .collect(Collectors.toMap(
                        user -> user,
                        this::getCurrentWorkload
                ));

        // Filter users with capacity
        List<User> usersWithCapacity = workloads.entrySet().stream()
                .filter(entry -> entry.getValue() < maxCasesPerUser)
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        User assignedUser;
        if (usersWithCapacity.isEmpty()) {
            // All users at capacity - assign to user with lowest workload
            assignedUser = workloads.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElseThrow(() -> new IllegalStateException("No users available"));
        } else {
            // Assign to user with lowest workload
            assignedUser = usersWithCapacity.get(0);
        }

        assignCaseToUser(complianceCase, assignedUser, null);
        return assignedUser;
    }

    /**
     * Assign case using round-robin algorithm
     */
    @Transactional
    public User assignCaseRoundRobin(ComplianceCase complianceCase, UserRole requiredRole) {
        List<User> availableUsers = userRepository.findByRole_NameAndEnabled(requiredRole.name(), true);

        if (availableUsers.isEmpty()) {
            throw new IllegalStateException("No users available for role: " + requiredRole);
        }

        // Get user with least recent assignment
        User assignedUser = availableUsers.stream()
                .min(Comparator.comparing(this::getLastAssignmentTime))
                .orElse(availableUsers.get(0));

        assignCaseToUser(complianceCase, assignedUser, null);
        return assignedUser;
    }

    /**
     * Manually assign case to a specific user
     */
    @Transactional
    public void assignCaseToUser(ComplianceCase complianceCase, User assignee, User assigner) {
        complianceCase.setAssignedTo(assignee);
        complianceCase.setAssignedBy(assigner != null ? assigner.getId() : null);
        complianceCase.setAssignedAt(LocalDateTime.now());

        if (complianceCase.getStatus() == CaseStatus.NEW) {
            complianceCase.setStatus(CaseStatus.ASSIGNED);
        }

        caseRepository.save(complianceCase);
        logger.info("Assigned case {} to user {}", complianceCase.getCaseReference(), assignee.getUsername());
    }

    /**
     * Reassign case to a different user
     */
    @Transactional
    public void reassignCase(Long caseId, Long newAssigneeId, User reassigner) {
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        User newAssignee = userRepository.findById(newAssigneeId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        assignCaseToUser(complianceCase, newAssignee, reassigner);
        logger.info("Reassigned case {} from {} to {}", 
                complianceCase.getCaseReference(),
                complianceCase.getAssignedTo() != null ? complianceCase.getAssignedTo().getUsername() : "unassigned",
                newAssignee.getUsername());
    }

    /**
     * Get current workload for a user
     */
    private int getCurrentWorkload(User user) {
        return (int) caseRepository.countByAssignedTo_IdAndStatusIn(
                user.getId(),
                List.of(CaseStatus.ASSIGNED, CaseStatus.IN_PROGRESS, CaseStatus.PENDING_REVIEW)
        );
    }

    /**
     * Get last assignment time for a user
     */
    private LocalDateTime getLastAssignmentTime(User user) {
        return caseRepository.findTop1ByAssignedTo_IdOrderByAssignedAtDesc(user.getId())
                .map(ComplianceCase::getAssignedAt)
                .orElse(LocalDateTime.MIN);
    }

    /**
     * Get workload distribution for a role
     */
    public Map<String, Integer> getWorkloadDistribution(UserRole role) {
        List<User> users = userRepository.findByRole_NameAndEnabled(role.name(), true);
        return users.stream()
                .collect(Collectors.toMap(
                        User::getUsername,
                        this::getCurrentWorkload
                ));
    }
}

