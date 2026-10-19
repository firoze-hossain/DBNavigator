package com.roze.dbnavigator.search;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.ui.action.ActionManager;
import com.roze.dbnavigator.ui.action.AnAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SearchEverywhereTest {

    @BeforeAll
    static void initActions() {
        AnAction action = AnAction.builder("settings.path.variables", "Environment strings")
                .description("Configure environment path variables")
                .onAction(ctx -> {})
                .build();
        ActionManager.getInstance().registerAction(action);
    }

    @Test
    void testSearchHitScoringRanking() {
        SearchHit exactTable = new SearchHit.Builder()
                .type(SearchHit.Type.TABLE)
                .name("environments")
                .detail("thunder_db.public [erp-161]")
                .build();

        SearchHit prefixCol = new SearchHit.Builder()
                .type(SearchHit.Type.COLUMN)
                .name("environment_id")
                .detail("thunder_db.public.environment_variables [erp-161]")
                .parentTable("environment_variables")
                .build();

        SearchHit substringTable = new SearchHit.Builder()
                .type(SearchHit.Type.TABLE)
                .name("workspace_access_environments")
                .detail("thunder_db.public [erp-161]")
                .build();

        String query = "environments";
        int scoreExact = exactTable.calculateScore(query);
        int scorePrefix = prefixCol.calculateScore(query);
        int scoreSubstring = substringTable.calculateScore(query);

        assertTrue(scoreExact > scoreSubstring, "Exact match should score higher than substring match");
        assertEquals(SearchHit.Category.DATABASE, exactTable.getCategory());
        assertEquals(SearchHit.Category.DATABASE, prefixCol.getCategory());
    }

    @Test
    void testSearchHitCategoriesAndBadges() {
        SearchHit pkHit = new SearchHit.Builder()
                .type(SearchHit.Type.PRIMARY_KEY)
                .name("environments_pkey")
                .primaryKey(true)
                .build();
        assertEquals("PK", pkHit.getBadge());
        assertEquals(SearchHit.Category.DATABASE, pkHit.getCategory());

        SearchHit seqHit = new SearchHit.Builder()
                .type(SearchHit.Type.SEQUENCE)
                .name("environments_id_seq")
                .build();
        assertEquals("123", seqHit.getBadge());
        assertEquals(SearchHit.Category.DATABASE, seqHit.getCategory());

        SearchHit uniqueIdx = new SearchHit.Builder()
                .type(SearchHit.Type.INDEX)
                .name("environments_idx")
                .uniqueIndex(true)
                .build();
        assertEquals("i_u", uniqueIdx.getBadge());

        SearchHit actionHit = new SearchHit.Builder()
                .type(SearchHit.Type.ACTION)
                .name("Settings > Path Variables")
                .build();
        assertEquals(SearchHit.Category.ACTIONS, actionHit.getCategory());

        SearchHit textHit = new SearchHit.Builder()
                .type(SearchHit.Type.TEXT)
                .name("FROM appointment_tx")
                .build();
        assertEquals(SearchHit.Category.TEXT, textHit.getCategory());
    }

    @Test
    void testActionSearch() {
        AtomicInteger gen = new AtomicInteger(1);
        List<SearchHit> hits = SearchEverywhereService.search(
                "Environment strings",
                SearchHit.Category.ACTIONS,
                true,
                null,
                gen,
                1,
                10
        );

        assertNotNull(hits);
        boolean found = hits.stream().anyMatch(h -> h.getName().contains("Environment strings"));
        assertTrue(found, "Should find 'Environment strings' in Actions search");
    }

    @Test
    void testActionDeduplicationAndNoArbitraryMiddleMatch() {
        // Register duplicate actions and actions with 'app' inside words (like wrapper or mapping)
        ActionManager manager = ActionManager.getInstance();
        manager.registerAction(AnAction.builder("git.apply.patch", "Apply Patch..").onAction(c -> {}).build());
        manager.registerAction(AnAction.builder("vcs.apply.patch", "Apply Patch..").onAction(c -> {}).build());
        manager.registerAction(AnAction.builder("edit.generate.fdw", "Foreign Data Wrapper").onAction(c -> {}).build());
        manager.registerAction(AnAction.builder("edit.generate.user.mapping", "User Mapping").onAction(c -> {}).build());

        AtomicInteger gen = new AtomicInteger(1);
        List<SearchHit> hits = SearchEverywhereService.search(
                "App",
                SearchHit.Category.ACTIONS,
                true,
                null,
                gen,
                1,
                20
        );

        assertNotNull(hits);

        // 1. "Apply Patch.." should be deduplicated (at most 1 occurrence)
        long applyCount = hits.stream().filter(h -> h.getName().equals("Apply Patch..")).count();
        assertTrue(applyCount <= 1, "Duplicate action 'Apply Patch..' should be deduplicated to at most 1, but got " + applyCount);

        // 2. "Foreign Data Wrapper" should NOT match "App" (no arbitrary middle-of-word match)
        boolean hasFdw = hits.stream().anyMatch(h -> h.getName().equalsIgnoreCase("Foreign Data Wrapper"));
        assertFalse(hasFdw, "'Foreign Data Wrapper' must NOT match 'App' because 'app' is inside 'wrapper'");

        // 3. "User Mapping" should NOT match "App"
        boolean hasUserMapping = hits.stream().anyMatch(h -> h.getName().equalsIgnoreCase("User Mapping"));
        assertFalse(hasUserMapping, "'User Mapping' must NOT match 'App' because 'app' is inside 'mapping'");
    }

    @Test
    void testMultiTokenMatching() {
        SearchHit tableHit = new SearchHit.Builder()
                .type(SearchHit.Type.TABLE)
                .name("appointment_tx")
                .detail("public [erp-161]")
                .build();

        // Multi-word query "app tx" or "app_tx" matches "appointment_tx"
        int score = tableHit.calculateScore("app tx");
        assertTrue(score > 1200, "Multi-token search 'app tx' should score bonus points for 'appointment_tx', got " + score);

        int scoreUnderscore = tableHit.calculateScore("app_tx");
        assertTrue(scoreUnderscore > 1200, "'app_tx' should score bonus points for 'appointment_tx', got " + scoreUnderscore);
    }
}
