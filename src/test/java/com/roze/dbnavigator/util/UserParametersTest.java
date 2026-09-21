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
}
