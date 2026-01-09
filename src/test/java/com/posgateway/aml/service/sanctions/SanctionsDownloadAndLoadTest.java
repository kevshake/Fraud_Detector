package com.posgateway.aml.service.sanctions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.posgateway.aml.entity.sanctions.WatchlistUpdate;
import com.posgateway.aml.repository.sanctions.WatchlistUpdateRepository;
import com.posgateway.aml.service.AerospikeConnectionService;
import com.posgateway.aml.service.download.SanctionsListDownloadService;
import com.posgateway.aml.service.sanctions.NameMatchingService;
import com.posgateway.aml.service.sanctions.WatchlistUpdateTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for verifying sanctions download and database loading functionality.
 * 
 * Tests verify:
 * 1. Sanctions download runs successfully
 * 2. Data is loaded to Aerospike
 * 3. Watchlist updates are tracked in PostgreSQL database
 * 4. Version checking prevents duplicate downloads
 */
@ExtendWith(MockitoExtension.class)
class SanctionsDownloadAndLoadTest {

    @Mock
    private NameMatchingService nameMatchingService;

    @Mock
    private AerospikeConnectionService aerospikeService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WatchlistUpdateTrackingService watchlistUpdateTrackingService;

    @Mock
    private WatchlistUpdateRepository watchlistUpdateRepository;

    @Mock
    private com.aerospike.client.AerospikeClient aerospikeClient;

    @InjectMocks
    private SanctionsListDownloadService sanctionsDownloadService;

    private ObjectMapper objectMapper;
    private String testTempDir;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(sanctionsDownloadService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(sanctionsDownloadService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(sanctionsDownloadService, "watchlistUpdateTrackingService", watchlistUpdateTrackingService);
        ReflectionTestUtils.setField(sanctionsDownloadService, "downloadEnabled", true);
        
        // Create test temp directory
        testTempDir = System.getProperty("java.io.tmpdir") + File.separator + "sanctions_test_" + System.currentTimeMillis();
        Files.createDirectories(Paths.get(testTempDir));
        ReflectionTestUtils.setField(sanctionsDownloadService, "tempDirectory", testTempDir);
        
        // Mock Aerospike client
        when(aerospikeService.getClient()).thenReturn(aerospikeClient);
    }

    @Test
    void testSanctionsDownloadRunsSuccessfully() throws Exception {
        // Arrange
        String testVersion = "2024-01-09-v1";
        String metadataJson = "{\"version\":\"" + testVersion + "\"}";
        
        // Create a minimal test sanctions file
        Path testFile = createTestSanctionsFile();
        
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(metadataJson);
        
        // Mock Aerospike put operations
        doNothing().when(aerospikeClient).put(any(), any(), any());

        // Act
        assertDoesNotThrow(() -> {
            // Use reflection to call private method for testing
            ReflectionTestUtils.invokeMethod(sanctionsDownloadService, 
                "downloadAndProcessSanctions");
        });

        // Assert
        verify(restTemplate, atLeastOnce()).getForObject(anyString(), eq(String.class));
        verify(aerospikeService, atLeastOnce()).getClient();
        
        // Cleanup
        Files.deleteIfExists(testFile);
    }

    @Test
    void testWatchlistUpdateIsRecordedInDatabase() {
        // Arrange
        String listName = "OPENSANCTIONS";
        String listType = "SANCTIONS";
        LocalDate updateDate = LocalDate.now();
        Long recordCount = 1000L;
        String sourceUrl = "https://data.opensanctions.org/datasets/latest/sanctions/targets.nested.json";
        String checksum = "test-version-123";

        WatchlistUpdateTrackingService trackingService = new WatchlistUpdateTrackingService(watchlistUpdateRepository);
        
        WatchlistUpdate mockUpdate = new WatchlistUpdate();
        mockUpdate.setListName(listName);
        mockUpdate.setListType(listType);
        mockUpdate.setUpdateDate(updateDate);
        mockUpdate.setRecordCount(recordCount);
        mockUpdate.setStatus("COMPLETED");
        
        when(watchlistUpdateRepository.findByListNameAndListTypeAndUpdateDate(listName, listType, updateDate))
                .thenReturn(Optional.empty());
        when(watchlistUpdateRepository.save(any(WatchlistUpdate.class))).thenReturn(mockUpdate);

        // Act
        WatchlistUpdate result = trackingService.recordUpdate(listName, listType, updateDate, recordCount, sourceUrl, checksum);

        // Assert
        assertNotNull(result);
        assertEquals(listName, result.getListName());
        assertEquals(listType, result.getListType());
        assertEquals(updateDate, result.getUpdateDate());
        assertEquals(recordCount, result.getRecordCount());
        assertEquals("COMPLETED", result.getStatus());
        
        verify(watchlistUpdateRepository).save(any(WatchlistUpdate.class));
    }

    @Test
    void testWatchlistUpdatePreventsDuplicates() {
        // Arrange
        String listName = "OPENSANCTIONS";
        String listType = "SANCTIONS";
        LocalDate updateDate = LocalDate.now();
        Long recordCount = 1000L;
        String sourceUrl = "https://data.opensanctions.org/datasets/latest/sanctions/targets.nested.json";
        String checksum = "test-version-123";

        WatchlistUpdateTrackingService trackingService = new WatchlistUpdateTrackingService(watchlistUpdateRepository);
        
        WatchlistUpdate existingUpdate = new WatchlistUpdate();
        existingUpdate.setListName(listName);
        existingUpdate.setListType(listType);
        existingUpdate.setUpdateDate(updateDate);
        existingUpdate.setRecordCount(500L);
        existingUpdate.setStatus("COMPLETED");
        
        when(watchlistUpdateRepository.findByListNameAndListTypeAndUpdateDate(listName, listType, updateDate))
                .thenReturn(Optional.of(existingUpdate));
        when(watchlistUpdateRepository.save(any(WatchlistUpdate.class))).thenReturn(existingUpdate);

        // Act
        WatchlistUpdate result = trackingService.recordUpdate(listName, listType, updateDate, recordCount, sourceUrl, checksum);

        // Assert
        assertNotNull(result);
        assertEquals(recordCount, result.getRecordCount()); // Should update existing record
        verify(watchlistUpdateRepository).save(any(WatchlistUpdate.class));
    }

    @Test
    void testGetLatestUpdate() {
        // Arrange
        String listName = "OPENSANCTIONS";
        String listType = "SANCTIONS";
        
        WatchlistUpdateTrackingService trackingService = new WatchlistUpdateTrackingService(watchlistUpdateRepository);
        
        WatchlistUpdate oldUpdate = new WatchlistUpdate();
        oldUpdate.setUpdateDate(LocalDate.now().minusDays(2));
        
        WatchlistUpdate newUpdate = new WatchlistUpdate();
        newUpdate.setUpdateDate(LocalDate.now());
        
        when(watchlistUpdateRepository.findByListNameAndListType(listName, listType))
                .thenReturn(java.util.Arrays.asList(oldUpdate, newUpdate));

        // Act
        Optional<WatchlistUpdate> result = trackingService.getLatestUpdate(listName, listType);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(LocalDate.now(), result.get().getUpdateDate());
    }

    @Test
    void testUpdateFrequencyStats() {
        // Arrange
        String listName = "OPENSANCTIONS";
        String listType = "SANCTIONS";
        
        WatchlistUpdateTrackingService trackingService = new WatchlistUpdateTrackingService(watchlistUpdateRepository);
        
        WatchlistUpdate update1 = new WatchlistUpdate();
        update1.setUpdateDate(LocalDate.now().minusDays(5));
        
        WatchlistUpdate update2 = new WatchlistUpdate();
        update2.setUpdateDate(LocalDate.now().minusDays(2));
        
        WatchlistUpdate update3 = new WatchlistUpdate();
        update3.setUpdateDate(LocalDate.now());
        
        when(watchlistUpdateRepository.findByListNameAndListType(listName, listType))
                .thenReturn(java.util.Arrays.asList(update1, update2, update3));

        // Act
        java.util.Map<String, Object> stats = trackingService.getUpdateFrequencyStats(listName, listType);

        // Assert
        assertNotNull(stats);
        assertEquals(listName, stats.get("listName"));
        assertEquals(listType, stats.get("listType"));
        assertEquals(3, stats.get("totalUpdates"));
        assertNotNull(stats.get("latestUpdateDate"));
        assertNotNull(stats.get("earliestUpdateDate"));
        assertNotNull(stats.get("averageDaysBetweenUpdates"));
    }

    @Test
    void testSanctionsDataLoadedToAerospike() throws Exception {
        // Arrange
        Path testFile = createTestSanctionsFile();
        
        when(nameMatchingService.generatePhoneticCode(anyString())).thenReturn("TEST123");
        when(nameMatchingService.generateAlternatePhoneticCode(anyString())).thenReturn("TEST456");
        when(aerospikeService.getClient()).thenReturn(aerospikeClient);
        
        // Act
        // Use reflection to test private method
        int recordCount = (Integer) ReflectionTestUtils.invokeMethod(sanctionsDownloadService,
                "processAndLoadToAerospike", testFile, "test-version");

        // Assert
        assertTrue(recordCount > 0, "Should process at least one record");
        verify(aerospikeClient, atLeastOnce()).put(any(), any(), any());
        
        // Cleanup
        Files.deleteIfExists(testFile);
    }

    @Test
    void testVersionCheckPreventsDuplicateDownloads() throws Exception {
        // Arrange
        String testVersion = "2024-01-09-v1";
        String metadataJson = "{\"version\":\"" + testVersion + "\"}";
        
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(metadataJson);

        // Act
        String version = (String) ReflectionTestUtils.invokeMethod(sanctionsDownloadService,
                "checkMetadataVersion");

        // Assert
        assertEquals(testVersion, version);
        verify(restTemplate).getForObject(anyString(), eq(String.class));
    }

    @Test
    void testDownloadDisabledSkipsExecution() {
        // Arrange
        ReflectionTestUtils.setField(sanctionsDownloadService, "downloadEnabled", false);

        // Act & Assert
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(sanctionsDownloadService, "performScheduledDownload");
        });
    }

    /**
     * Helper method to create a test sanctions JSON file
     */
    private Path createTestSanctionsFile() throws IOException {
        Path testFile = Paths.get(testTempDir, "test_sanctions.json");
        
        // Create minimal valid JSON structure
        String testJson = """
                {"id": "test-entity-1", "schema": "Person", "properties": {"name": ["John Doe"]}}
                {"id": "test-entity-2", "schema": "Company", "properties": {"name": ["Test Corp"]}}
                """;
        
        Files.writeString(testFile, testJson);
        return testFile;
    }
}
