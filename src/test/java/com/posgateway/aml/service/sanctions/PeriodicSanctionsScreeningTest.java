package com.posgateway.aml.service.sanctions;

import com.posgateway.aml.entity.merchant.BeneficialOwner;
import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.model.ScreeningResult;
import com.posgateway.aml.repository.MerchantRepository;
import com.posgateway.aml.service.aml.AerospikeSanctionsScreeningService;
import com.posgateway.aml.service.case_management.CaseCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for verifying periodic sanctions screening functionality.
 * 
 * Tests verify:
 * 1. Periodic screening runs successfully
 * 2. Merchants are screened against sanctions list
 * 3. Beneficial owners are screened
 * 4. Cases are created when matches are found
 * 5. Merchant screening status is updated
 */
@ExtendWith(MockitoExtension.class)
class PeriodicSanctionsScreeningTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private AerospikeSanctionsScreeningService screeningService;

    @Mock
    private CaseCreationService caseCreationService;

    @InjectMocks
    private PeriodicSanctionsScreeningService periodicScreeningService;

    private Merchant testMerchant;
    private Psp testPsp;

    @BeforeEach
    void setUp() {
        // Create test PSP
        testPsp = new Psp();
        testPsp.setPspId(1L);
        testPsp.setPspCode("TEST_PSP");

        // Create test merchant
        testMerchant = new Merchant();
        testMerchant.setMerchantId(100L);
        testMerchant.setLegalName("Test Merchant Inc");
        testMerchant.setTradingName("Test Merchant");
        testMerchant.setPsp(testPsp);
        testMerchant.setLastScreenedAt(LocalDateTime.now().minusDays(10));
        testMerchant.setNextScreeningDue(LocalDate.now().minusDays(1)); // Due for screening

        // Create test beneficial owner
        BeneficialOwner ubo = new BeneficialOwner();
        ubo.setFullName("John Doe");
        ubo.setDateOfBirth(LocalDate.of(1980, 1, 1));
        ubo.setIsSanctioned(false);

        List<BeneficialOwner> ubos = new ArrayList<>();
        ubos.add(ubo);
        testMerchant.setBeneficialOwners(ubos);
    }

    @Test
    void testPeriodicScreeningRunsSuccessfully() {
        // Arrange
        LocalDate today = LocalDate.now();
        List<Merchant> merchants = List.of(testMerchant);

        when(merchantRepository.findMerchantsNeedingRescreening(today))
                .thenReturn(merchants);

        ScreeningResult noMatchResult = new ScreeningResult();
        noMatchResult.setMatches(new ArrayList<>());

        when(screeningService.screenMerchant(anyString(), anyString()))
                .thenReturn(noMatchResult);
        when(screeningService.screenBeneficialOwner(anyString(), any()))
                .thenReturn(noMatchResult);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(testMerchant);

        // Act
        assertDoesNotThrow(() -> {
            periodicScreeningService.performScheduledScreening();
        });

        // Assert
        verify(merchantRepository).findMerchantsNeedingRescreening(today);
        verify(screeningService).screenMerchant(eq("Test Merchant Inc"), eq("Test Merchant"));
        verify(merchantRepository).save(any(Merchant.class));
    }

    @Test
    void testMerchantScreeningCreatesCaseOnMatch() {
        // Arrange
        ScreeningResult matchResult = new ScreeningResult();
        ScreeningResult.Match match = new ScreeningResult.Match();
        match.setSimilarityScore(0.95);
        match.setMatchedName("Test Merchant Inc");
        match.setListName("OFAC_SDN");
        matchResult.setMatches(List.of(match));
        matchResult.setHighestMatchScore(0.95);

        when(screeningService.screenMerchant(anyString(), anyString()))
                .thenReturn(matchResult);
        when(screeningService.screenBeneficialOwner(anyString(), any()))
                .thenReturn(new ScreeningResult());
        when(merchantRepository.save(any(Merchant.class))).thenReturn(testMerchant);

        // Act
        periodicScreeningService.processMerchant(testMerchant);

        // Assert
        verify(caseCreationService).triggerCaseFromSanctions(
                eq(100L),
                eq(1L),
                eq("SANCTIONS_WATCHLIST"),
                contains("Periodic Screening Hit")
        );
        verify(merchantRepository).save(any(Merchant.class));
        assertEquals("CRITICAL", testMerchant.getRiskLevel());
    }

    @Test
    void testBeneficialOwnerScreeningCreatesCaseOnMatch() {
        // Arrange
        ScreeningResult merchantNoMatch = new ScreeningResult();
        merchantNoMatch.setMatches(new ArrayList<>());

        ScreeningResult uboMatchResult = new ScreeningResult();
        ScreeningResult.Match match = new ScreeningResult.Match();
        match.setSimilarityScore(0.90);
        match.setMatchedName("John Doe");
        match.setListName("UN_SC");
        uboMatchResult.setMatches(List.of(match));
        uboMatchResult.setHighestMatchScore(0.90);

        when(screeningService.screenMerchant(anyString(), anyString()))
                .thenReturn(merchantNoMatch);
        when(screeningService.screenBeneficialOwner(eq("John Doe"), any()))
                .thenReturn(uboMatchResult);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(testMerchant);

        // Act
        periodicScreeningService.processMerchant(testMerchant);

        // Assert
        verify(caseCreationService).triggerCaseFromSanctions(
                eq(100L),
                eq(1L),
                eq("PEP_SANCTIONS_UBO"),
                contains("UBO Hit")
        );
        assertTrue(testMerchant.getBeneficialOwners().get(0).getIsSanctioned());
        assertEquals("CRITICAL", testMerchant.getRiskLevel());
    }

    @Test
    void testNoMatchesDoesNotCreateCase() {
        // Arrange
        ScreeningResult noMatchResult = new ScreeningResult();
        noMatchResult.setMatches(new ArrayList<>());

        when(screeningService.screenMerchant(anyString(), anyString()))
                .thenReturn(noMatchResult);
        when(screeningService.screenBeneficialOwner(anyString(), any()))
                .thenReturn(noMatchResult);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(testMerchant);

        // Act
        periodicScreeningService.processMerchant(testMerchant);

        // Assert
        verify(caseCreationService, never()).triggerCaseFromSanctions(anyLong(), anyLong(), anyString(), anyString());
        assertNotNull(testMerchant.getLastScreenedAt());
    }

    @Test
    void testMerchantScreeningStatusUpdated() {
        // Arrange
        ScreeningResult noMatchResult = new ScreeningResult();
        noMatchResult.setMatches(new ArrayList<>());

        LocalDateTime beforeScreening = testMerchant.getLastScreenedAt();

        when(screeningService.screenMerchant(anyString(), anyString()))
                .thenReturn(noMatchResult);
        when(screeningService.screenBeneficialOwner(anyString(), any()))
                .thenReturn(noMatchResult);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(testMerchant);

        // Act
        periodicScreeningService.processMerchant(testMerchant);

        // Assert
        assertNotNull(testMerchant.getLastScreenedAt());
        assertTrue(testMerchant.getLastScreenedAt().isAfter(beforeScreening));
        verify(merchantRepository).save(testMerchant);
    }

    @Test
    void testMultipleMerchantsScreened() {
        // Arrange
        Merchant merchant1 = new Merchant();
        merchant1.setMerchantId(101L);
        merchant1.setLegalName("Merchant One");
        merchant1.setPsp(testPsp);

        Merchant merchant2 = new Merchant();
        merchant2.setMerchantId(102L);
        merchant2.setLegalName("Merchant Two");
        merchant2.setPsp(testPsp);

        List<Merchant> merchants = List.of(merchant1, merchant2);
        LocalDate today = LocalDate.now();

        when(merchantRepository.findMerchantsNeedingRescreening(today))
                .thenReturn(merchants);

        ScreeningResult noMatchResult = new ScreeningResult();
        noMatchResult.setMatches(new ArrayList<>());

        when(screeningService.screenMerchant(anyString(), anyString()))
                .thenReturn(noMatchResult);
        when(screeningService.screenBeneficialOwner(anyString(), any()))
                .thenReturn(noMatchResult);
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        periodicScreeningService.performScheduledScreening();

        // Assert
        verify(screeningService, times(2)).screenMerchant(anyString(), anyString());
        verify(merchantRepository, times(2)).save(any(Merchant.class));
    }

    @Test
    void testScreeningHandlesExceptionsGracefully() {
        // Arrange
        LocalDate today = LocalDate.now();
        List<Merchant> merchants = List.of(testMerchant);

        when(merchantRepository.findMerchantsNeedingRescreening(today))
                .thenReturn(merchants);
        when(screeningService.screenMerchant(anyString(), anyString()))
                .thenThrow(new RuntimeException("Screening service error"));

        // Act & Assert - Should not throw exception, should log error and continue
        assertDoesNotThrow(() -> {
            periodicScreeningService.performScheduledScreening();
        });

        verify(merchantRepository).findMerchantsNeedingRescreening(today);
    }
}
