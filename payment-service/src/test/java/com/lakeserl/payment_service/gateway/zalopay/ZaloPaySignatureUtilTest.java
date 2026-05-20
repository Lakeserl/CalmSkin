package com.lakeserl.payment_service.gateway.zalopay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ZaloPay HMAC-SHA256 signature utility.
 */
class ZaloPaySignatureUtilTest {

    private static final String TEST_KEY1 = "ZALOPAY_TEST_KEY1_2024";
    private static final String TEST_KEY2 = "ZALOPAY_TEST_KEY2_2024";

    @Test
    @DisplayName("hmacSHA256 produces a 64-char hex string")
    void hmacSHA256_producesCorrectLength() {
        String hash = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY1, "test data");
        assertFalse(hash.isEmpty());
        assertEquals(64, hash.length(), "HMAC-SHA256 hex string should be 64 chars");
    }

    @Test
    @DisplayName("hmacSHA256 is deterministic for same key + data")
    void hmacSHA256_deterministic() {
        String data = "2553|220815_001|user1|50000|1660540000|{}|[]";
        String hash1 = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY1, data);
        String hash2 = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY1, data);
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hmacSHA256 differs with different keys")
    void hmacSHA256_differentKeys() {
        String data = "2553|220815_001|user1|50000|1660540000|{}|[]";
        String hash1 = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY1, data);
        String hash2 = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY2, data);
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hmacSHA256 returns empty for null key or data")
    void hmacSHA256_nullInputs() {
        assertEquals("", ZaloPaySignatureUtil.hmacSHA256(null, "data"));
        assertEquals("", ZaloPaySignatureUtil.hmacSHA256(TEST_KEY1, null));
    }

    @Test
    @DisplayName("buildCreateMacData produces correct pipe-delimited format")
    void buildCreateMacData_correctFormat() {
        String result = ZaloPaySignatureUtil.buildCreateMacData(
                "2553", "220815_001", "user1", 50000L,
                1660540000L, "{}", "[]");

        assertEquals("2553|220815_001|user1|50000|1660540000|{}|[]", result);
    }

    @Test
    @DisplayName("buildCreateMacData handles null embed_data and item")
    void buildCreateMacData_nullFields() {
        String result = ZaloPaySignatureUtil.buildCreateMacData(
                "2553", "220815_001", "user1", 50000L,
                1660540000L, null, null);

        assertEquals("2553|220815_001|user1|50000|1660540000||[]", result);
    }

    @Test
    @DisplayName("verify returns true for valid MAC using key2")
    void verify_validMac() {
        String callbackData = "{\"app_id\":2553,\"app_trans_id\":\"220815_001\"}";
        String mac = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY2, callbackData);

        assertTrue(ZaloPaySignatureUtil.verify(mac, TEST_KEY2, callbackData));
    }

    @Test
    @DisplayName("verify returns false for tampered MAC")
    void verify_tamperedMac() {
        String callbackData = "{\"app_id\":2553,\"app_trans_id\":\"220815_001\"}";
        String fakeMac = "0000000000000000000000000000000000000000000000000000000000000000";

        assertFalse(ZaloPaySignatureUtil.verify(fakeMac, TEST_KEY2, callbackData));
    }

    @Test
    @DisplayName("verify returns false when MAC is null or empty")
    void verify_nullOrEmpty() {
        assertFalse(ZaloPaySignatureUtil.verify(null, TEST_KEY2, "data"));
        assertFalse(ZaloPaySignatureUtil.verify("", TEST_KEY2, "data"));
    }

    @Test
    @DisplayName("verify is case-insensitive for MAC comparison")
    void verify_caseInsensitive() {
        String data = "{\"amount\":50000}";
        String mac = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY2, data);

        assertTrue(ZaloPaySignatureUtil.verify(mac.toUpperCase(), TEST_KEY2, data));
    }

    @Test
    @DisplayName("verify detects data tampering")
    void verify_dataTampered() {
        String originalData = "{\"amount\":50000}";
        String mac = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY2, originalData);

        String tamperedData = "{\"amount\":99999}";
        assertFalse(ZaloPaySignatureUtil.verify(mac, TEST_KEY2, tamperedData));
    }

    @Test
    @DisplayName("key1 and key2 produce different MACs for same data")
    void differentKeys_differentMacs() {
        String data = "same data for both keys";
        String mac1 = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY1, data);
        String mac2 = ZaloPaySignatureUtil.hmacSHA256(TEST_KEY2, data);
        assertNotEquals(mac1, mac2, "key1 and key2 should produce different MACs");
    }
}
