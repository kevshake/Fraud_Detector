package com.posgateway.aml.service.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.rules.RuleDefinition;
import com.posgateway.aml.entity.rules.RuleLifecycleStatus;
import com.posgateway.aml.entity.rules.RuleVersion;
import com.posgateway.aml.repository.rules.RuleDefinitionRepository;
import com.posgateway.aml.repository.rules.RuleVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RuleGovernanceServiceTest {

    private final RuleDefinitionRepository ruleRepository = mock(RuleDefinitionRepository.class);
    private final RuleVersionRepository versionRepository = mock(RuleVersionRepository.class);
    private final DroolsRulesService droolsRulesService = mock(DroolsRulesService.class);
    private final AtomicReference<RuleVersion> storedVersion = new AtomicReference<>();
    private RuleGovernanceService service;

    @BeforeEach
    void setUp() {
        service = new RuleGovernanceService(ruleRepository, versionRepository, droolsRulesService,
                new ObjectMapper());
        when(ruleRepository.findByNameAndPspId(anyString(), org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenReturn(Optional.empty());
        when(ruleRepository.save(any(RuleDefinition.class))).thenAnswer(invocation -> {
            RuleDefinition rule = invocation.getArgument(0);
            if (rule.getId() == null) ReflectionTestUtils.setField(rule, "id", 42L);
            return rule;
        });
        when(versionRepository.findFirstByRuleIdOrderByVersionNumberDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(versionRepository.save(any(RuleVersion.class))).thenAnswer(invocation -> {
            RuleVersion version = invocation.getArgument(0);
            if (version.getId() == null) ReflectionTestUtils.setField(version, "id", 7L);
            ReflectionTestUtils.invokeMethod(version, "onCreate");
            storedVersion.set(version);
            return version;
        });
        when(versionRepository.findById(7L)).thenAnswer(invocation -> Optional.ofNullable(storedVersion.get()));
    }

    @Test
    void proposedEnabledRuleStaysInactiveUntilIndependentApproval() {
        User maker = user(10L, "maker");
        User reviewer = user(11L, "reviewer");
        RuleDefinition proposed = rule("Large cash deposit", true);

        RuleDefinition pending = service.proposeCreate(proposed, maker, null, "Add cash typology");

        assertThat(pending.isEnabled()).isFalse();
        assertThat(pending.getLifecycleStatus()).isEqualTo(RuleLifecycleStatus.PENDING_APPROVAL);
        assertThat(pending.getPendingVersionId()).isEqualTo(7L);
        verify(droolsRulesService, never()).reloadRules();

        service.approve(7L, reviewer, null, "Validated expression and test evidence");

        assertThat(pending.isEnabled()).isTrue();
        assertThat(pending.getLifecycleStatus()).isEqualTo(RuleLifecycleStatus.ACTIVE);
        assertThat(pending.getCurrentVersionNumber()).isEqualTo(1);
        assertThat(pending.getPendingVersionId()).isNull();
        verify(droolsRulesService).reloadRules();
    }

    @Test
    void makerCannotApproveOwnRuleVersion() {
        User maker = user(10L, "maker");
        service.proposeCreate(rule("Rapid movement", true), maker, null, "New typology");

        assertThatThrownBy(() -> service.approve(7L, maker, null, "Self approval"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("cannot approve");
        verify(droolsRulesService, never()).reloadRules();
    }

    @Test
    void approvalRequiresDocumentedReason() {
        service.proposeCreate(rule("Dormant account", false), user(10L, "maker"), null, "New typology");

        assertThatThrownBy(() -> service.approve(7L, user(11L, "reviewer"), null, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
    }

    private RuleDefinition rule(String name, boolean enabled) {
        RuleDefinition rule = new RuleDefinition();
        rule.setName(name);
        rule.setDescription("Test rule");
        rule.setRuleType("SPEL");
        rule.setRuleExpression("amount > 10000");
        rule.setAction("ALERT");
        rule.setPriority(100);
        rule.setEnabled(enabled);
        return rule;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
