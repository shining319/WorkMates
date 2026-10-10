package com.workmates.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void okHasDataAndNoError() {
        Result<String> result = Result.ok("hello");

        assertTrue(result.isSuccess());
        assertEquals("hello", result.getData());
        assertNull(result.getErrorCode());
        assertNull(result.getPage());
    }

    @Test
    void pagedOkCarriesPageInfo() {
        PageInfo page = new PageInfo(0, 20, 57, 3);

        Result<List<Integer>> result = Result.ok(List.of(1, 2), page);

        assertTrue(result.isSuccess());
        assertEquals(page, result.getPage());
    }

    @Test
    void failCarriesErrorCodeName() {
        Result<Void> result = Result.fail(ErrorCode.POSITION_FILLED, "filled");

        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("POSITION_FILLED", result.getErrorCode());
        assertEquals("filled", result.getErrorMsg());
    }

    @Test
    void jsonHasTheDocumentedShape() throws Exception {
        JsonNode json = objectMapper.valueToTree(Result.ok(List.of(1), new PageInfo(0, 20, 57, 3)));

        assertTrue(json.get("success").asBoolean());
        assertEquals(1, json.get("data").get(0).asInt());
        assertTrue(json.get("errorCode").isNull());
        assertTrue(json.get("errorMsg").isNull());
        assertEquals(57, json.get("page").get("totalElements").asInt());
        assertEquals(3, json.get("page").get("totalPages").asInt());
        assertEquals(5, json.size());
    }
}
