package com.lakeserl.payment_service.gateway.momo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Momo HMAC-SHA256 signature utility.
 */
class MomoSignatureUtilTest {

    private static final String TEST_KEY = "MOMO_TEST_SECRET_KEY_2024";

    @Test
    @DisplayName("hmacSHA256 produces a 64-char hex string")
    void hmacSHA256_producesCorrectLength() {
        String hash = MomoSignatureUtil.hmacSHA256(TEST_KEY, "test data");
        assertFalse(hash.isEmpty());
        assertEquals(64, hash.length(), "HMAC-SHA256 hex string should be 64 chars");
    }

    @Test
    @DisplayName("hmacSHA256 is deterministic for same key + data")
    void hmacSHA256_deterministic() {
        String data = "accessKey=F8BBA842ECF85&amount=50000&orderId=ORDER001";
        String hash1 = MomoSignatureUtil.hmacSHA256(TEST_KEY, data);
        String hash2 = MomoSignatureUtil.hmacSHA256(TEST_KEY, data);
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hmacSHA256 differs with different keys")
    void hmacSHA256_differentKeys() {
        String data = "accessKey=F8BBA842ECF85&amount=50000";
        String hash1 = MomoSignatureUtil.hmacSHA256(TEST_KEY, data);
        String hash2 = MomoSignatureUtil.hmacSHA256("DIFFERENT_KEY", data);
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hmacSHA256 returns empty for null key or data")
    void hmacSHA256_nullInputs() {
        assertEquals("", MomoSignatureUtil.hmacSHA256(null, "data"));
        assertEquals("", MomoSignatureUtil.hmacSHA256(TEST_KEY, null));
    }

    @Test
    @DisplayName("buildCreateSignatureData produces correct format")
    void buildCreateSignatureData_correctFormat() {
        String result = MomoSignatureUtil.buildCreateSignatureData(
                "ACCESS_KEY", 50000L, "", "http://ipn.url",
                "ORDER001", "Test order", "PARTNER",
                "http://redirect", "REQ001", "payWithMethod");

        assertTrue(result.startsWith("accessKey=ACCESS_KEY&"));
        assertTrue(result.contains("&amount=50000&"));
        assertTrue(result.contains("&orderId=ORDER001&"));
        assertTrue(result.contains("&requestType=payWithMethod"));
    }

    @Test
    @DisplayName("verify returns true for valid HMAC-SHA256 signature")
    void verify_validSignature() {
        String rawData = MomoSignatureUtil.buildCreateSignatureData(
                "ACCESS_KEY", 50000L, "", "http://ipn.url",
                "ORDER001", "Test order", "PARTNER",
                "http://redirect", "REQ001", "payWithMethod");

        String signature = MomoSignatureUtil.hmacSHA256(TEST_KEY, rawData);

        assertTrue(MomoSignatureUtil.verify(signature, TEST_KEY, rawData));
    }

    @Test
    @DisplayName("verify returns false for tampered signature")
    void verify_tamperedSignature() {
        String rawData = "accessKey=ACCESS_KEY&amount=50000&orderId=ORDER001";
        String fakeSignature = "0000000000000000000000000000000000000000000000000000000000000000";

        assertFalse(MomoSignatureUtil.verify(fakeSignature, TEST_KEY, rawData));
    }

    @Test
    @DisplayName("verify returns false when signature is null or empty")
    void verify_nullOrEmpty() {
        assertFalse(MomoSignatureUtil.verify(null, TEST_KEY, "data"));
        assertFalse(MomoSignatureUtil.verify("", TEST_KEY, "data"));
    }

    @Test
    @DisplayName("verify is case-insensitive for signature comparison")
    void verify_caseInsensitive() {
        String data = "accessKey=KEY&amount=100000";
        String signature = MomoSignatureUtil.hmacSHA256(TEST_KEY, data);

        assertTrue(MomoSignatureUtil.verify(signature.toUpperCase(), TEST_KEY, data));
    }

    @Test
    @DisplayName("verify detects amount tampering")
    void verify_amountTampered() {
        String originalData = "accessKey=KEY&amount=50000&orderId=ORDER001";
        String signature = MomoSignatureUtil.hmacSHA256(TEST_KEY, originalData);

        String tamperedData = "accessKey=KEY&amount=99999&orderId=ORDER001";
        assertFalse(MomoSignatureUtil.verify(signature, TEST_KEY, tamperedData));
    }
}
