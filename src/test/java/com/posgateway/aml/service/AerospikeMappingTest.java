package com.posgateway.aml.service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Record;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.posgateway.aml.model.ScreeningResult;
import com.posgateway.aml.model.ScreeningResult.EntityType;
import com.posgateway.aml.service.aml.AerospikeSanctionsScreeningService;
import com.posgateway.aml.service.sanctions.NameMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AerospikeMappingTest {

    @Mock
    private AerospikeConnectionService aerospikeConnectionService;

    @Mock
    private NameMatchingService nameMatchingService;

    @Mock
    private AerospikeClient aerospikeClient;

    @InjectMocks
    private AerospikeSanctionsScreeningService screeningService;

    @BeforeEach
    void setUp() {
        // Inject values usually set by @Value
        ReflectionTestUtils.setField(screeningService, "namespace", "test");
        ReflectionTestUtils.setField(screeningService, "similarityThreshold", 0.8);
        ReflectionTestUtils.setField(screeningService, "cacheEnabled", false);
    }

    @Test
    void testAerospikeRecordMapping() {
        // Arrange
        String searchName = "John Doe";
        String phonetic = "JND";

        when(aerospikeConnectionService.getClient()).thenReturn(aerospikeClient);
        when(nameMatchingService.generatePhoneticCode(searchName)).thenReturn(phonetic);
        when(nameMatchingService.generateAlternatePhoneticCode(searchName)).thenReturn(phonetic); // Same to simplify
        when(nameMatchingService.calculateSimilarityScore(any(), any())).thenReturn(0.95);

        // Mock Aerospike Record
        Map<String, Object> bins = new HashMap<>();
        bins.put("full_name", "Johnathan Doe");
        bins.put("entity_type", "PERSON");
        bins.put("aliases", Arrays.asList("Johnny", "J. Doe"));
        bins.put("list_name", "OFAC");
        bins.put("date_of_birth", 631152000L); // 1990-01-01 in seconds? (approx) -> 7305 days * 86400 = 631152000
        bins.put("nationality", Arrays.asList("US"));
        bins.put("sanction_type", "Block");
        bins.put("program", Arrays.asList("SDNTK"));

        Record record = new Record(bins, 0, 0);

        // Mock RecordSet
        RecordSet recordSet = mock(RecordSet.class);
        when(recordSet.next()).thenReturn(true).thenReturn(false); // One result then stop
        when(recordSet.getRecord()).thenReturn(record);

        when(aerospikeClient.query(any(QueryPolicy.class), any(Statement.class))).thenReturn(recordSet);

        // Act
        ScreeningResult result = screeningService.screenName(searchName, EntityType.PERSON);

        // Assert
        assertNotNull(result);
        assertEquals(searchName, result.getScreenedName());
        assertEquals(1, result.getMatches().size());

        ScreeningResult.Match match = result.getMatches().get(0);
        assertEquals("Johnathan Doe", match.getMatchedName());
        assertEquals(EntityType.PERSON, match.getEntityType());
        assertEquals("OFAC", match.getListName());
        assertEquals(Arrays.asList("Johnny", "J. Doe"), match.getAliases());
        assertEquals(Arrays.asList("US"), match.getNationality());

        // Check Date Logic (Epoch Seconds to LocalDate)
        // 631152000 / 86400 = 7305 days.
        // LocalDate.ofEpochDay(7305) -> 1990-01-01
        assertEquals(LocalDate.of(1990, 1, 1), match.getDateOfBirth());

        System.out.println("Aerospike Mapping Test Passed: DTO read successfully from mocked Record.");
    }
}
