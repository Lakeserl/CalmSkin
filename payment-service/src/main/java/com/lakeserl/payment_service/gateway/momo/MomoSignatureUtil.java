package com.lakeserl.payment_service.gateway.momo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Momo HMAC-SHA256 signature utility.
 * <p>
 * Momo uses HMAC-SHA256 over a specific concatenation of sorted fields:
 * {@code accessKey=…&amount=…&extraData=…&ipnUrl=…&orderId=…&orderInfo=…
 * &partnerCode=…&redirectUrl=…&requestId=…&requestType=…}
 * <p>
 * Verification uses {@link MessageDigest#isEqual(byte[], byte[])} — timing-safe.
 */
public final class MomoSignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private MomoSignatureUtil() {
    }

    /**
     * Compute HMAC-SHA256 over {@code data} using {@code key}.
     *
     * @return lowercase hex string, or empty string on error
     */
    public static String hmacSHA256(String key, String data) {
        if (key == null || data == null) {
            return "";
        }
        try {
            Mac hmac256 = Mac.getInstance(HMAC_SHA256);
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, HMAC_SHA256);
            hmac256.init(secretKeySpec);
            byte[] result = hmac256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Build the Momo signature raw data for payment creation.
     * Fields must appear in alphabetical order.
     */
    public static String buildCreateSignatureData(
            String accessKey, Long amount, String extraData, String ipnUrl,
            String orderId, String orderInfo, String partnerCode,
            String redirectUrl, String requestId, String requestType) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + (extraData != null ? extraData : "")
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
    }

    /**
     * Build the Momo signature raw data for webhook/IPN verification.
     * Fields must appear in alphabetical order matching Momo's IPN format.
     */
    public static String buildIpnSignatureData(
            String accessKey, Long amount, String extraData, String message,
            String orderId, String orderInfo, String orderType,
            String partnerCode, String payType, String requestId,
            Long responseTime, int resultCode, String transId) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + (extraData != null ? extraData : "")
                + "&message=" + message
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&orderType=" + (orderType != null ? orderType : "")
                + "&partnerCode=" + partnerCode
                + "&payType=" + (payType != null ? payType : "")
                + "&requestId=" + requestId
                + "&responseTime=" + responseTime
                + "&resultCode=" + resultCode
                + "&transId=" + transId;
    }

    /**
     * Timing-safe verification of an HMAC-SHA256 signature.
     */
    public static boolean verify(String expectedSignature, String secretKey, String data) {
        if (expectedSignature == null || expectedSignature.isEmpty()) {
            return false;
        }
        String computed = hmacSHA256(secretKey, data);
        if (computed.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                expectedSignature.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
