package com.roze.dbnavigator.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ParameterHistoryStoreTest {

    @BeforeEach
    public void setUp() {
        ParameterHistoryStore.clear();
    }

    @Test
    public void testPutAndGetParameterValues() {
        assertNull(ParameterHistoryStore.get("attendanceStatus"));

        ParameterHistoryStore.put("attendanceStatus", "1");
        ParameterHistoryStore.put("admissionStatuses", "136,138");

        assertEquals("1", ParameterHistoryStore.get("attendanceStatus"));
        assertEquals("136,138", ParameterHistoryStore.get("admissionStatuses"));

        // Case-insensitive retrieval
        assertEquals("1", ParameterHistoryStore.get("attendancestatus"));
        assertEquals("136,138", ParameterHistoryStore.get("ADMISSIONSTATUSES"));
    }

    @Test
    public void testPutAllAndGetAll() {
        Map<String, String> values = Map.of(
                "p1", "val1",
                "p2", "val2"
        );
        ParameterHistoryStore.putAll(values);

        Map<String, String> all = ParameterHistoryStore.getAll();
        assertEquals(2, all.size());
        assertEquals("val1", all.get("p1"));
        assertEquals("val2", all.get("p2"));
    }

    @Test
    public void testUpdateExistingParameter() {
        ParameterHistoryStore.put("status", "1");
        assertEquals("1", ParameterHistoryStore.get("status"));

        ParameterHistoryStore.put("status", "2");
        assertEquals("2", ParameterHistoryStore.get("status"));
    }
}
