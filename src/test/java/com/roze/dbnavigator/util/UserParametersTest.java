package com.roze.dbnavigator.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.Settings;
import com.roze.dbnavigator.db.AppSettingsStore.UserParameterPattern;
import com.roze.dbnavigator.model.ConnectionProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UserParametersTest {

    private Settings defaultSettings;

    @BeforeEach
    public void setUp() {
        defaultSettings = new Settings();
    }

    @Test
    public void testDetectStandardColonParameter() {
        String sql = "SELECT * FROM users WHERE id = :id AND name = :user_name";
        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, defaultSettings, null);

        assertEquals(2, params.size());
        assertEquals("id", params.get(0).name());
        assertEquals("id", params.get(0).guessedColumn());
        assertEquals("user_name", params.get(1).name());
        assertEquals("name", params.get(1).guessedColumn());
    }

    @Test
    public void testDetectBraceParameter() {
        String sql = "SELECT * FROM orders WHERE order_id = ${orderId}";
        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, defaultSettings, null);

        assertEquals(1, params.size());
        assertEquals("orderId", params.get(0).name());
        assertEquals("order_id", params.get(0).guessedColumn());
    }

    @Test
    public void testDetectHashParameter() {
        String sql = "SELECT * FROM items WHERE item_num = #itemNum#";
        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, defaultSettings, null);

        assertEquals(1, params.size());
        assertEquals("itemNum", params.get(0).name());
        assertEquals("item_num", params.get(0).guessedColumn());
    }

    @Test
    public void testDetectPythonNamedParameter() {
        String sql = "SELECT * FROM metrics WHERE val > %(threshold)s";
        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, defaultSettings, null);

        assertEquals(1, params.size());
        assertEquals("threshold", params.get(0).name());
    }

    @Test
    public void testDetectPostgresQuotedParameter() {
        String sql = "SELECT * FROM tenant WHERE code = :'tenant_code'";
        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, defaultSettings, null);

        assertEquals(1, params.size());
        assertEquals("tenant_code", params.get(0).name());
    }

    @Test
    public void testSubstituteBasic() {
        String sql = "SELECT * FROM users WHERE id = :id AND name = :name AND age = :age";
        Map<String, String> values = Map.of(
                "id", "42",
                "name", "Alice",
                "age", ""
        );

        String substituted = SqlParameters.substitute(sql, values, defaultSettings, null);
        assertEquals("SELECT * FROM users WHERE id = 42 AND name = 'Alice' AND age = NULL", substituted);
    }

    @Test
    public void testSubstituteInsideSqlStringsDisabledByDefault() {
        Settings settings = new Settings();
        settings.setSubstituteInsideSqlStrings(false);

        String sql = "SELECT 'Hello :name world' FROM dual";
        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, settings, null);
        assertTrue(params.isEmpty(), "Parameters inside SQL string literals should not be detected when disabled");

        String substituted = SqlParameters.substitute(sql, Map.of("name", "Bob"), settings, null);
        assertEquals("SELECT 'Hello :name world' FROM dual", substituted);
    }

    @Test
    public void testSubstituteInsideSqlStringsEnabled() {
        Settings settings = new Settings();
        settings.setSubstituteInsideSqlStrings(true);

        String sql = "SELECT 'Hello :name world' FROM dual";
        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, settings, null);
        assertEquals(1, params.size(), "Parameter inside string literal should be detected when enabled");
        assertEquals("name", params.get(0).name());

        String substituted = SqlParameters.substitute(sql, Map.of("name", "Bob"), settings, null);
        assertEquals("SELECT 'Hello 'Bob' world' FROM dual", substituted);
    }

    @Test
    public void testExtractPrecedingCommentTitleLineComment() {
        String sql = "-- Top 10 Active Customers\nSELECT * FROM customers LIMIT 10;";
        String title = SqlParameters.extractPrecedingCommentTitle(sql, "");
        assertEquals("Top 10 Active Customers", title);
    }

    @Test
    public void testExtractPrecedingCommentTitleBlockComment() {
        String sql = "/* All Departments Overview */\nSELECT * FROM departments;";
        String title = SqlParameters.extractPrecedingCommentTitle(sql, "");
        assertEquals("All Departments Overview", title);
    }

    @Test
    public void testExtractPrecedingCommentTitleWithTreatAfterText() {
        String sql = "-- Title: Active Orders\nSELECT * FROM orders;";
        String title = SqlParameters.extractPrecedingCommentTitle(sql, "Title:");
        assertEquals("Active Orders", title);
    }

    @Test
    public void testExtractPrecedingCommentTitleTruncation() {
        String sql = "-- This is a very long query description that comfortably exceeds fifty characters\nSELECT 1;";
        String title = SqlParameters.extractPrecedingCommentTitle(sql, "");
        assertNotNull(title);
        assertTrue(title.length() <= 50);
        assertTrue(title.endsWith("…"));
    }

    @Test
    public void testDisableUserParameters() {
        Settings settings = new Settings();
        settings.setEnableUserParameters(false);

        String sql = "SELECT * FROM users WHERE id = :id";
        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, settings, null);
        assertTrue(params.isEmpty());

        String substituted = SqlParameters.substitute(sql, Map.of("id", "42"), settings, null);
        assertEquals("SELECT * FROM users WHERE id = :id", substituted);
    }

    @Test
    public void testCustomUserParameterPattern() {
        Settings settings = new Settings();
        List<UserParameterPattern> patterns = new ArrayList<>(Settings.defaultUserParameterPatterns());
        patterns.add(new UserParameterPattern("@@name@@", "everywhere", "All languages", true, false));
        settings.setUserParameterPatterns(patterns);

        String sql = "SELECT * FROM configs WHERE key = @@app_key@@";
        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, settings, null);
        assertEquals(1, params.size());
        assertEquals("app_key", params.get(0).name());

        String substituted = SqlParameters.substitute(sql, Map.of("app_key", "secret123"), settings, null);
        assertEquals("SELECT * FROM configs WHERE key = 'secret123'", substituted);
    }

    @Test
    public void testLanguageFiltering() {
        Settings settings = new Settings();
        ConnectionProfile pgProfile = new ConnectionProfile();
        pgProfile.setType(ConnectionProfile.DatabaseType.POSTGRESQL);

        ConnectionProfile myProfile = new ConnectionProfile();
        myProfile.setType(ConnectionProfile.DatabaseType.MYSQL);

        String sql = "SELECT * FROM test WHERE id = :'pg_id'";

        // For PostgreSQL profile, :'name' pattern (configured for PostgreSQL) should match
        List<SqlParameters.Parameter> pgParams = SqlParameters.detect(sql, settings, pgProfile);
        assertEquals(1, pgParams.size());
        assertEquals("pg_id", pgParams.get(0).name());

        // For MySQL profile, :'name' should NOT match because its language scope is PostgreSQL
        List<SqlParameters.Parameter> myParams = SqlParameters.detect(sql, settings, myProfile);
        assertTrue(myParams.isEmpty());
    }

    @Test
    public void testAppSettingsStoreSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        Settings settings = new Settings();
        settings.setShowTimestampForQueryOutput(true);
        settings.setEnableDbmsOutput(true);
        settings.setShowResultsInEditor(true);
        settings.setCreateTitleFromComment(false);
        settings.setTitleAfterCommentText("SQL:");
        settings.setShowServicesOutput("In case of error");
        settings.setFocusServicesInWindowMode(true);
        settings.setOpenNewServicesTabForSessions(true);
        settings.setActivateServicesForSelectedFileOnly(true);

        settings.setEnableUserParameters(true);
        settings.setEnableUserParametersInLiteralsWithInjection(false);
        settings.setSubstituteInsideSqlStrings(true);

        List<UserParameterPattern> customPatterns = new ArrayList<>();
        customPatterns.add(new UserParameterPattern("::name::", "everywhere", "All languages", true, true));
        settings.setUserParameterPatterns(customPatterns);

        String json = mapper.writeValueAsString(settings);
        assertNotNull(json);

        Settings restored = mapper.readValue(json, Settings.class);
        assertTrue(restored.isShowTimestampForQueryOutput());
        assertTrue(restored.isEnableDbmsOutput());
        assertTrue(restored.isShowResultsInEditor());
        assertFalse(restored.isCreateTitleFromComment());
        assertEquals("SQL:", restored.getTitleAfterCommentText());
        assertEquals("In case of error", restored.getShowServicesOutput());
        assertTrue(restored.isFocusServicesInWindowMode());
        assertTrue(restored.isOpenNewServicesTabForSessions());
        assertTrue(restored.isActivateServicesForSelectedFileOnly());

        assertTrue(restored.isEnableUserParameters());
        assertFalse(restored.isEnableUserParametersInLiteralsWithInjection());
        assertTrue(restored.isSubstituteInsideSqlStrings());

        assertEquals(1, restored.getUserParameterPatterns().size());
        UserParameterPattern p = restored.getUserParameterPatterns().get(0);
        assertEquals("::name::", p.getPattern());
        assertEquals("everywhere", p.getScope());
        assertEquals("All languages", p.getLanguages());
        assertTrue(p.isInScripts());
        assertTrue(p.isInLiterals());
        assertTrue(p.isEnabled());
    }

    @Test
    public void testPostgresCastDoesNotCreateParameter() {
        String sql = """
                SELECT
                    b.id AS invoiceId,
                    b.uhid AS uhid,
                    b.bill_date_time::date AS billDate,
                    b.total_amount AS totalAmount,
                    COALESCE(b.vat, 0) AS vat,
                    COALESCE(b.unallocated_vat, 0) AS unallocatedVat,
                    COALESCE(b.vat, 0) + COALESCE(b.unallocated_vat, 0) AS totalVat
                FROM bbill b
                WHERE b.bill_date_time::date BETWEEN :fromDate AND :toDate
                  AND b.status NOT IN (1, 2, 21, 34)
                """;

        ConnectionProfile pgProfile = new ConnectionProfile();
        pgProfile.setType(ConnectionProfile.DatabaseType.POSTGRESQL);

        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, defaultSettings, pgProfile);

        // Exactly 2 parameters: fromDate and toDate (NOT 'date' from ::date)
        assertEquals(2, params.size(), "Should only detect fromDate and toDate, not ::date cast");
        assertEquals("fromDate", params.get(0).name());
        assertEquals("toDate", params.get(1).name());

        assertNotNull(params.get(0).contextSnippet());
        assertTrue(params.get(0).contextSnippet().contains("::date BETWEEN _ AND"),
                "Snippet should match DataGrip context: " + params.get(0).contextSnippet());

        assertNotNull(params.get(1).contextSnippet());
        assertTrue(params.get(1).contextSnippet().contains(":fromDate AND _"),
                "Snippet should match DataGrip context: " + params.get(1).contextSnippet());

        // Test substitution
        Map<String, String> values = Map.of(
                "fromDate", "'2026-01-12'",
                "toDate", "'2026-01-31'"
        );
        String substituted = SqlParameters.substitute(sql, values, defaultSettings, pgProfile);
        assertTrue(substituted.contains("b.bill_date_time::date BETWEEN '2026-01-12' AND '2026-01-31'"));
    }

    @Test
    public void testUserQueryAttendanceAndAdmissionStatusesSubstitution() {
        String sql = """
                SELECT
                    a.id AS attendanceId,
                    a.created_at AS attendanceCreatedAt,
                    a.hms_bed_id AS hmsBedId,
                    c.id AS clinicNatureId,
                    t.uhid AS uhid,
                    t.hms_admission_id AS admissionId,
                    t.admission_time AS admissionTime,
                    t.admission_date AS admissionDate,
                    t.process_date_time AS processDateTime,
                    t.discharge_date_time AS dischargeDateTime,
                    t.assigned_doctor_id AS doctorId,
                    r.b_item_code AS roomCharge
                FROM hms_admission_attendance a
                INNER JOIN hms_admission t ON t.hms_admission_id = a.hms_admission_id
                LEFT JOIN hms_bed b ON a.hms_bed_id = b.id
                LEFT JOIN hms_room r ON r.id = b.hms_room_id
                LEFT JOIN hms_room_type rt ON rt.id = r.hms_room_type_id
                LEFT JOIN hms_clinic_nature c ON c.id = rt.hms_clinic_nature_id
                WHERE a.status = :attendanceStatus
                AND t.status in (:admissionStatuses)
                AND t.ehr_episode_type_id = 2
                AND (t.discharge_date_time IS NULL OR t.discharge_date_time >= a.created_at);
                """;

        ConnectionProfile pgProfile = new ConnectionProfile();
        pgProfile.setType(ConnectionProfile.DatabaseType.POSTGRESQL);

        List<SqlParameters.Parameter> params = SqlParameters.detect(sql, defaultSettings, pgProfile);
        assertEquals(2, params.size());
        assertEquals("attendanceStatus", params.get(0).name());
        assertEquals("admissionStatuses", params.get(1).name());

        // Context snippet matching DataGrip
        assertEquals("a.status = _", params.get(0).contextSnippet());
        assertEquals("(_)", params.get(1).contextSnippet());

        // Test 1: User types "136,138" without spaces
        Map<String, String> values1 = Map.of(
                "attendanceStatus", "1",
                "admissionStatuses", "136,138"
        );
        String substituted1 = SqlParameters.substitute(sql, values1, defaultSettings, pgProfile);
        assertTrue(substituted1.contains("WHERE a.status = 1"), "attendanceStatus should be 1");
        assertTrue(substituted1.contains("AND t.status in (136, 138)"),
                "admissionStatuses should substitute as numeric list (136, 138), not string ('136,138')");
        assertFalse(substituted1.contains("'136,138'"), "Must not quote comma-separated numeric list");

        // Test 2: User types "136, 138" with space
        Map<String, String> values2 = Map.of(
                "attendanceStatus", "1",
                "admissionStatuses", "136, 138"
        );
        String substituted2 = SqlParameters.substitute(sql, values2, defaultSettings, pgProfile);
        assertTrue(substituted2.contains("AND t.status in (136, 138)"));

        // Test 3: User accidentally types "(136, 138)"
        Map<String, String> values3 = Map.of(
                "attendanceStatus", "1",
                "admissionStatuses", "(136, 138)"
        );
        String substituted3 = SqlParameters.substitute(sql, values3, defaultSettings, pgProfile);
        assertTrue(substituted3.contains("AND t.status in (136, 138)"),
                "Should strip redundant parentheses when inside IN (...) clause");
    }

    @Test
    public void testInClauseStringListSubstitution() {
        String sql = "SELECT * FROM orders WHERE status IN (:statuses)";
        Map<String, String> values = Map.of("statuses", "ACTIVE, PENDING");
        String substituted = SqlParameters.substitute(sql, values, defaultSettings, null);
        assertEquals("SELECT * FROM orders WHERE status IN ('ACTIVE', 'PENDING')", substituted);

        Map<String, String> quotedValues = Map.of("statuses", "'ACTIVE', 'PENDING'");
        String quotedSubstituted = SqlParameters.substitute(sql, quotedValues, defaultSettings, null);
        assertEquals("SELECT * FROM orders WHERE status IN ('ACTIVE', 'PENDING')", quotedSubstituted);
    }

    @Test
    public void testNonInClauseStringWithCommas() {
        String sql = "SELECT * FROM addresses WHERE city = :city AND note = :note";
        Map<String, String> values = Map.of(
                "city", "Springfield",
                "note", "123 Main St, Apt 4"
        );
        String substituted = SqlParameters.substitute(sql, values, defaultSettings, null);
        assertEquals("SELECT * FROM addresses WHERE city = 'Springfield' AND note = '123 Main St, Apt 4'", substituted);
    }
}
