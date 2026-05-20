package com.lakeserl.payment_service.gateway.vnpay;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class VNPaySignatureUtilTest {

    private static final String TEST_KEY = "VNPAY_TEST_SECRET_KEY_2024";
    private static final String TEST_DATA = "vnp_Amount=1000000&vnp_Command=pay&vnp_TmnCode=DEMO";

    @Test
    @DisplayName("hmacSHA512 produces a 128-char hex string")
    void hmacSHA512_producesCorrectLength() {
        String hash = VNPaySignatureUtil.hmacSHA512(TEST_KEY, TEST_DATA);
        assertFalse(hash.isEmpty());
        assertEquals(128, hash.length(), "HMAC-SHA512 hex string should be 128 chars");
    }

    @Test
    @DisplayName("hmacSHA512 is deterministic for same key + data")
    void hmacSHA512_deterministic() {
        String hash1 = VNPaySignatureUtil.hmacSHA512(TEST_KEY, TEST_DATA);
        String hash2 = VNPaySignatureUtil.hmacSHA512(TEST_KEY, TEST_DATA);
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hmacSHA512 differs with different keys")
    void hmacSHA512_differentKeys() {
        String hash1 = VNPaySignatureUtil.hmacSHA512(TEST_KEY, TEST_DATA);
        String hash2 = VNPaySignatureUtil.hmacSHA512("DIFFERENT_KEY", TEST_DATA);
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hmacSHA512 returns empty for null key or data")
    void hmacSHA512_nullInputs() {
        assertEquals("", VNPaySignatureUtil.hmacSHA512(null, TEST_DATA));
        assertEquals("", VNPaySignatureUtil.hmacSHA512(TEST_KEY, null));
    }

    @Test
    @DisplayName("buildHashData sorts keys alphabetically and excludes SecureHash")
    void buildHashData_sortsAndExcludesHash() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", "DEMO");
        params.put("vnp_Amount", "1000000");
        params.put("vnp_SecureHash", "should_be_excluded");
        params.put("vnp_Command", "pay");
        params.put("vnp_SecureHashType", "SHA512");

        String hashData = VNPaySignatureUtil.buildHashData(params);

        assertEquals("vnp_Amount=1000000&vnp_Command=pay&vnp_TmnCode=DEMO", hashData);
        assertFalse(hashData.contains("SecureHash"));
    }

    @Test
    @DisplayName("buildHashData skips empty values")
    void buildHashData_skipsEmptyValues() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_BankCode", ""); // empty — should be skipped
        params.put("vnp_Command", "pay");

        String hashData = VNPaySignatureUtil.buildHashData(params);

        assertEquals("vnp_Amount=1000000&vnp_Command=pay", hashData);
    }

    @Test
    @DisplayName("verify returns true for valid signature")
    void verify_validSignature() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "DEMO");

        // Compute the expected signature
        String hashData = VNPaySignatureUtil.buildHashData(params);
        String correctSignature = VNPaySignatureUtil.hmacSHA512(TEST_KEY, hashData);
        params.put("vnp_SecureHash", correctSignature);

        assertTrue(VNPaySignatureUtil.verify(params, TEST_KEY));
    }

    @Test
    @DisplayName("verify returns false for tampered signature")
    void verify_tamperedSignature() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "DEMO");
        params.put("vnp_SecureHash", "0000000000000000000000000000000000000000000000000000000000000000"
                + "0000000000000000000000000000000000000000000000000000000000000000");

        assertFalse(VNPaySignatureUtil.verify(params, TEST_KEY));
    }

    @Test
    @DisplayName("verify returns false when vnp_SecureHash is missing")
    void verify_missingSignature() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_Command", "pay");

        assertFalse(VNPaySignatureUtil.verify(params, TEST_KEY));
    }

    @Test
    @DisplayName("verify returns false for amount tampered after signing")
    void verify_amountTampered() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "ORDER001");

        String hashData = VNPaySignatureUtil.buildHashData(params);
        String signature = VNPaySignatureUtil.hmacSHA512(TEST_KEY, hashData);

        // Tamper with amount after signing
        params.put("vnp_Amount", "9999999");
        params.put("vnp_SecureHash", signature);

        assertFalse(VNPaySignatureUtil.verify(params, TEST_KEY));
    }

    @Test
    @DisplayName("verify is case-insensitive for hash comparison")
    void verify_caseInsensitive() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "500000");
        params.put("vnp_TmnCode", "TEST");

        String hashData = VNPaySignatureUtil.buildHashData(params);
        String signature = VNPaySignatureUtil.hmacSHA512(TEST_KEY, hashData);

        // Send uppercase hash — should still verify
        params.put("vnp_SecureHash", signature.toUpperCase());

        assertTrue(VNPaySignatureUtil.verify(params, TEST_KEY));
    }
}
