package com.posgateway.aml.service.case_management;

import com.posgateway.aml.entity.CaseRequiredSkill;
import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.UserSkill;
import com.posgateway.aml.entity.compliance.CaseQueue;
import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.model.UserRole;
import com.posgateway.aml.model.CaseStatus;
import com.posgateway.aml.repository.CaseRequiredSkillRepository;
import com.posgateway.aml.repository.ComplianceCaseRepository;
import com.posgateway.aml.repository.UserRepository;
import com.posgateway.aml.repository.UserSkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Case Assignment Service
 * Handles automatic and manual case assignment with workload balancing and
 * skill-based routing
 */
@Service
public class CaseAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(CaseAssignmentService.class);

    private final ComplianceCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final CaseRequiredSkillRepository caseRequiredSkillRepository;

    @Value("${case.assignment.max-cases-per-user:20}")
    private int maxCasesPerUser;

    @Value("${case.assignment.skill-weight:0.6}")
    private double skillWeight; // Weight for skill score vs workload (0.0-1.0)

    @Autowired
    public CaseAssignmentService(ComplianceCaseRepository caseRepository,
            UserRepository userRepository,
            UserSkillRepository userSkillRepository,
            CaseRequiredSkillRepository caseRequiredSkillRepository) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.caseRequiredSkillRepository = caseRequiredSkillRepository;
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
                        this::getCurrentWorkload));

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
     * Assign case based on skill matching and workload
     * 
     * Algorithm:
     * 1. Get required skills for the case queue
     * 2. Find users with matching skills (proficiency >= required)
     * 3. Score users: skill_match_score * skill_weight + workload_score * (1 -
     * skill_weight)
     * 4. Assign to highest scoring user with capacity
     * 
     * @param complianceCase The case to assign
     * @param requiredRole   The required user role
     * @return The assigned user
     */
    @Transactional
    public User assignCaseBySkill(ComplianceCase complianceCase, UserRole requiredRole) {
        CaseQueue queue = complianceCase.getQueue();

        // If no queue or no skill requirements, fall back to workload-based assignment
        if (queue == null || !caseRequiredSkillRepository.existsByQueueId(queue.getId())) {
            logger.debug("No skill requirements for case {}, falling back to workload assignment",
                    complianceCase.getCaseReference());
            return assignCaseByWorkload(complianceCase, requiredRole);
        }

        // Get skill requirements for the queue
        List<CaseRequiredSkill> requiredSkills = caseRequiredSkillRepository
                .findByQueueIdOrderByWeightDesc(queue.getId());

        // Get all available users for the role
        List<User> availableUsers = userRepository.findByRole_NameAndEnabled(requiredRole.name(), true);

        if (availableUsers.isEmpty()) {
            throw new IllegalStateException("No users available for role: " + requiredRole);
        }

        // Score each user based on skills and workload
        Map<User, Double> userScores = new HashMap<>();

        for (User user : availableUsers) {
            double skillScore = calculateSkillMatchScore(user, requiredSkills);
            double workloadScore = calculateWorkloadScore(user);

            // Combined score: skill match weighted + workload weighted
            double combinedScore = (skillScore * skillWeight) + (workloadScore * (1 - skillWeight));
            userScores.put(user, combinedScore);

            logger.debug("User {} - Skill Score: {}, Workload Score: {}, Combined: {}",
                    user.getUsername(), skillScore, workloadScore, combinedScore);
        }

        // Filter to only users who meet mandatory skill requirements
        List<CaseRequiredSkill> mandatorySkills = requiredSkills.stream()
                .filter(CaseRequiredSkill::getRequired)
                .toList();

        List<User> qualifiedUsers = availableUsers.stream()
                .filter(user -> meetsAllMandatorySkills(user, mandatorySkills))
                .filter(user -> getCurrentWorkload(user) < maxCasesPerUser)
                .toList();

        User assignedUser;
        if (qualifiedUsers.isEmpty()) {
            // No fully qualified users - try users meeting at least some skills
            logger.warn("No users fully qualified for case {} queue {}, selecting best available",
                    complianceCase.getCaseReference(), queue.getQueueName());

            // Select the user with highest score who has capacity
            assignedUser = userScores.entrySet().stream()
                    .filter(entry -> getCurrentWorkload(entry.getKey()) < maxCasesPerUser)
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElseGet(() -> {
                        // All at capacity - get user with highest score and lowest workload
                        return userScores.entrySet().stream()
                                .min(Comparator.comparingInt(e -> getCurrentWorkload(e.getKey())))
                                .map(Map.Entry::getKey)
                                .orElseThrow(() -> new IllegalStateException("No users available"));
                    });
        } else {
            // Select the qualified user with highest score
            assignedUser = qualifiedUsers.stream()
                    .max(Comparator.comparingDouble(userScores::get))
                    .orElse(qualifiedUsers.get(0));
        }

        assignCaseToUser(complianceCase, assignedUser, null);
        logger.info("Skill-based assignment: Case {} assigned to {} (score: {})",
                complianceCase.getCaseReference(),
                assignedUser.getUsername(),
                String.format("%.2f", userScores.get(assignedUser)));

        return assignedUser;
    }

    /**
     * Calculate skill match score for a user (0.0 - 1.0)
     */
    private double calculateSkillMatchScore(User user, List<CaseRequiredSkill> requiredSkills) {
        if (requiredSkills.isEmpty()) {
            return 1.0; // No skills required = perfect match
        }

        double totalWeight = requiredSkills.stream()
                .mapToDouble(s -> s.getWeight().doubleValue())
                .sum();

        if (totalWeight == 0) {
            totalWeight = requiredSkills.size(); // Fallback if no weights
        }

        double earnedScore = 0.0;

        for (CaseRequiredSkill requiredSkill : requiredSkills) {
            Optional<UserSkill> userSkillOpt = userSkillRepository.findByUserIdAndSkillTypeId(
                    user.getId(), requiredSkill.getSkillType().getId());

            if (userSkillOpt.isPresent()) {
                UserSkill userSkill = userSkillOpt.get();
                int userLevel = userSkill.getProficiencyLevel();
                int requiredLevel = requiredSkill.getMinProficiency();
                int maxLevel = requiredSkill.getSkillType().getProficiencyLevels();

                if (userLevel >= requiredLevel) {
                    // User meets requirement - calculate proficiency bonus
                    // Score: base weight + bonus for exceeding minimum
                    double baseScore = requiredSkill.getWeight().doubleValue();
                    double proficiencyBonus = (double) (userLevel - requiredLevel) / maxLevel * 0.2 * baseScore;
                    earnedScore += baseScore + proficiencyBonus;
                } else if (!requiredSkill.getRequired()) {
                    // Preferred skill not met - partial credit
                    double partialScore = (double) userLevel / requiredLevel * requiredSkill.getWeight().doubleValue()
                            * 0.5;
                    earnedScore += partialScore;
                }
                // Required skill not met = 0 score for this skill
            }
            // User doesn't have skill = 0 score for this skill
        }

        return Math.min(earnedScore / totalWeight, 1.0);
    }

    /**
     * Calculate workload score (0.0 - 1.0, higher = more available)
     */
    private double calculateWorkloadScore(User user) {
        int currentWorkload = getCurrentWorkload(user);
        if (currentWorkload >= maxCasesPerUser) {
            return 0.0;
        }
        return 1.0 - ((double) currentWorkload / maxCasesPerUser);
    }

    /**
     * Check if user meets all mandatory skill requirements
     */
    private boolean meetsAllMandatorySkills(User user, List<CaseRequiredSkill> mandatorySkills) {
        for (CaseRequiredSkill requiredSkill : mandatorySkills) {
            Optional<UserSkill> userSkillOpt = userSkillRepository.findByUserIdAndSkillTypeId(
                    user.getId(), requiredSkill.getSkillType().getId());

            if (userSkillOpt.isEmpty()) {
                return false; // User doesn't have the skill
            }

            if (userSkillOpt.get().getProficiencyLevel() < requiredSkill.getMinProficiency()) {
                return false; // User doesn't meet minimum proficiency
            }
        }
        return true;
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
    public int getCurrentWorkload(User user) {
        return (int) caseRepository.countByAssignedTo_IdAndStatusIn(
                user.getId(),
                List.of(CaseStatus.ASSIGNED, CaseStatus.IN_PROGRESS, CaseStatus.PENDING_REVIEW));
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
                        this::getCurrentWorkload));
    }

    /**
     * Get skill-based assignment recommendations for a case
     */
    public List<UserAssignmentRecommendation> getAssignmentRecommendations(ComplianceCase complianceCase,
            UserRole requiredRole) {
        CaseQueue queue = complianceCase.getQueue();
        List<CaseRequiredSkill> requiredSkills = queue != null
                ? caseRequiredSkillRepository.findByQueueIdOrderByWeightDesc(queue.getId())
                : List.of();

        List<User> availableUsers = userRepository.findByRole_NameAndEnabled(requiredRole.name(), true);

        return availableUsers.stream()
                .map(user -> {
                    double skillScore = calculateSkillMatchScore(user, requiredSkills);
                    double workloadScore = calculateWorkloadScore(user);
                    double combinedScore = (skillScore * skillWeight) + (workloadScore * (1 - skillWeight));
                    boolean meetsAllRequired = meetsAllMandatorySkills(user,
                            requiredSkills.stream().filter(CaseRequiredSkill::getRequired).toList());

                    return new UserAssignmentRecommendation(
                            user.getId(),
                            user.getUsername(),
                            user.getFullName(),
                            skillScore,
                            workloadScore,
                            combinedScore,
                            getCurrentWorkload(user),
                            meetsAllRequired);
                })
                .sorted(Comparator.comparingDouble(UserAssignmentRecommendation::combinedScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * DTO for assignment recommendations
     */
    public record UserAssignmentRecommendation(
            Long userId,
            String username,
            String fullName,
            double skillScore,
            double workloadScore,
            double combinedScore,
            int currentWorkload,
            boolean meetsAllRequirements) {
    }
}
