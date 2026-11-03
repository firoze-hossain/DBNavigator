package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SqlSchemaValidatorTest {

    private ConnectionProfile profile;
    private final String catalog = "erpdb_abc";

    @BeforeEach
    void setUp() {
        profile = new ConnectionProfile();
        profile.setId("test-postgres-profile");
        profile.setName("postgres@182.163.126.223");
        profile.setType(ConnectionProfile.DatabaseType.POSTGRESQL);

        CompletionService.clearAllCaches();
    }

    @Test
    void testNoDiagnosticsWhenMetadataNotLoaded() {
        String sql = "SELECT b FROM Bbill b WHERE b.episodeTypeID = :ehrEpisodeTypeId";
        List<SqlSchemaValidator.Diagnostic> diags = SqlSchemaValidator.validate(sql, profile, catalog);
        assertTrue(diags.isEmpty(), "Should not report false positives when metadata has not loaded yet");
    }

    @Test
    void testSpringJpqlQueryValidationMatchesDataGripBehavior() {
        // Setup schema metadata: database has 'bbill' table (lowercase in postgres), but NOT 'hmsadmission'
        List<String> tables = List.of("bbill", "users", "orders");
        Map<String, List<String>> columns = Map.of(
                "bbill", List.of("id", "admission_date", "admission_id", "advance_allocation_amount",
                        "bill_date_time", "created_at", "created_by", "status")
        );

        CompletionService.setCachedMetadataForTesting(profile, catalog, tables, columns);

        String sql = """
                SELECT b FROM Bbill b
                WHERE b.episodeTypeID = :ehrEpisodeTypeId
                  AND b.status = :status
                  AND b.createdBy = :userId
                  AND b.createdAt BETWEEN :startOfDay AND :endOfDay
                ORDER BY b.createdAt DESC;

                SELECT a FROM HmsAdmission a
                WHERE a.ehrEpisodeTypeId = 3
                """;

        List<SqlSchemaValidator.Diagnostic> diags = SqlSchemaValidator.validate(sql, profile, catalog);
        assertFalse(diags.isEmpty());

        // 1. 'Bbill' must resolve successfully to 'bbill' (case-insensitive schema matching)
        assertTrue(diags.stream().anyMatch(d ->
                d.kind() == SqlSchemaValidator.DiagnosticKind.RESOLVED_TABLE &&
                d.token().equals("Bbill") &&
                d.message().contains("bbill")
        ), "Bbill should resolve to bbill in PostgreSQL");

        // 2. 'b.episodeTypeID' must be marked as UNRESOLVED_COLUMN because DB has no 'episodeTypeID' column
        assertTrue(diags.stream().anyMatch(d ->
                d.kind() == SqlSchemaValidator.DiagnosticKind.UNRESOLVED_COLUMN &&
                d.token().equals("episodeTypeID") &&
                d.message().equals("Unable to resolve column 'episodeTypeID'")
        ), "episodeTypeID should be flagged as unresolved column");

        // 3. 'b.createdBy' must be marked as UNRESOLVED_COLUMN because DB column is 'created_by' (snake_case)
        assertTrue(diags.stream().anyMatch(d ->
                d.kind() == SqlSchemaValidator.DiagnosticKind.UNRESOLVED_COLUMN &&
                d.token().equals("createdBy") &&
                d.message().equals("Unable to resolve column 'createdBy'")
        ), "createdBy should be flagged as unresolved column (differs from created_by)");

        // 4. 'b.createdAt' must be marked as UNRESOLVED_COLUMN because DB column is 'created_at'
        assertTrue(diags.stream().anyMatch(d ->
                d.kind() == SqlSchemaValidator.DiagnosticKind.UNRESOLVED_COLUMN &&
                d.token().equals("createdAt") &&
                d.message().equals("Unable to resolve column 'createdAt'")
        ), "createdAt should be flagged as unresolved column");

        // 5. 'b.status' exists in 'bbill' columns, so it must NOT be marked as unresolved
        assertFalse(diags.stream().anyMatch(d ->
                d.kind() == SqlSchemaValidator.DiagnosticKind.UNRESOLVED_COLUMN &&
                d.token().equals("status")
        ), "b.status is a valid column on bbill and must NOT be marked unresolved");

        // 6. 'HmsAdmission' does NOT exist in tables list, so it must be marked as UNRESOLVED_TABLE
        assertTrue(diags.stream().anyMatch(d ->
                d.kind() == SqlSchemaValidator.DiagnosticKind.UNRESOLVED_TABLE &&
                d.token().equals("HmsAdmission") &&
                d.message().equals("Unable to resolve table 'HmsAdmission'")
        ), "HmsAdmission should be flagged as unresolved table");

        // 7. 'a.ehrEpisodeTypeId' uses alias 'a' of unresolved table 'HmsAdmission'
        assertTrue(diags.stream().anyMatch(d ->
                d.kind() == SqlSchemaValidator.DiagnosticKind.UNRESOLVED_COLUMN &&
                d.token().equals("ehrEpisodeTypeId")
        ), "ehrEpisodeTypeId should be flagged as unresolved column on unresolved alias");
    }
}
