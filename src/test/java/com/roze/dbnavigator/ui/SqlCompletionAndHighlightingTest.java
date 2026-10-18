package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SqlCompletionAndHighlightingTest {

    private ConnectionProfile createProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("test-pg");
        p.setName("postgres@localhost");
        p.setType(ConnectionProfile.DatabaseType.POSTGRESQL);
        return p;
    }

    @Test
    public void testLiveTemplatesAtStatementStart() {
        ConnectionProfile profile = createProfile();
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "", "sel", CompletionService.Context.STATEMENT_START);

        assertFalse(suggestions.isEmpty(), "Should suggest completions for 'sel'");

        // Verify 'sel' template is suggested
        CompletionService.Suggestion selTmpl = suggestions.stream()
                .filter(s -> s.text().equalsIgnoreCase("sel") && s.kind() == CompletionService.Kind.TEMPLATE)
                .findFirst()
                .orElse(null);

        assertNotNull(selTmpl, "Template 'sel' must be present");
        assertEquals("select all rows from a table", selTmpl.detail());
        assertNotNull(selTmpl.templateExpansion());
        assertTrue(selTmpl.templateExpansion().contains("from"));
        assertTrue(selTmpl.templateExpansion().contains("$END$"));

        // Verify 'select' keyword is also suggested
        boolean hasSelectKw = suggestions.stream()
                .anyMatch(s -> s.text().equalsIgnoreCase("select") && s.kind() == CompletionService.Kind.KEYWORD);
        assertTrue(hasSelectKw, "'select' keyword must be suggested");

        // Verify 'selc' and 'selw' templates are also present
        boolean hasSelc = suggestions.stream().anyMatch(s -> s.text().equalsIgnoreCase("selc"));
        boolean hasSelw = suggestions.stream().anyMatch(s -> s.text().equalsIgnoreCase("selw"));
        assertTrue(hasSelc, "'selc' template must be suggested");
        assertTrue(hasSelw, "'selw' template must be suggested");
    }

    @Test
    public void testFromSuggestedAfterSelectProjection() {
        ConnectionProfile profile = createProfile();
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "select * ", "", CompletionService.Context.AFTER_SELECT_PROJECTION);

        assertFalse(suggestions.isEmpty(), "Should suggest keywords after select projection");

        CompletionService.Suggestion first = suggestions.get(0);
        assertEquals("from", first.text().toLowerCase(), "'from' should be top suggestion after select *");
        assertEquals(CompletionService.Kind.KEYWORD, first.kind());
    }

    @Test
    public void testCascadeSuggestedAfterDropTable() {
        ConnectionProfile profile = createProfile();
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "drop table environments cas", "cas", CompletionService.Context.POST_OBJECT_DDL);

        assertFalse(suggestions.isEmpty(), "Should suggest cascade after drop table");

        CompletionService.Suggestion cascade = suggestions.stream()
                .filter(s -> s.text().equalsIgnoreCase("cascade"))
                .findFirst()
                .orElse(null);

        assertNotNull(cascade, "'cascade' must be suggested in POST_OBJECT_DDL");
        assertEquals(CompletionService.Kind.KEYWORD, cascade.kind());
    }

    @Test
    public void testCascadeHighlightingInSqlHighlighter() {
        String sql = "drop table environments cascade;\ndrop sequence environments_id_seq cascade;";
        StyleSpans<Collection<String>> spans = SqlHighlighter.computeHighlighting(sql);
        assertNotNull(spans);

        // Check that cascade appears as sql-keyword
        boolean foundCascadeKeyword = false;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\bcascade\\b").matcher(sql);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            int currentOffset = 0;
            for (org.fxmisc.richtext.model.StyleSpan<Collection<String>> span : spans) {
                int nextOffset = currentOffset + span.getLength();
                if (currentOffset <= start && nextOffset >= end) {
                    if (span.getStyle().contains("sql-keyword")) {
                        foundCascadeKeyword = true;
                        break;
                    }
                }
                currentOffset = nextOffset;
            }
        }
        assertTrue(foundCascadeKeyword, "'cascade' must be highlighted as 'sql-keyword'");
    }
}
