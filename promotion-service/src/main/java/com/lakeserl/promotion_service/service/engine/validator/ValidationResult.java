package com.lakeserl.promotion_service.service.engine.validator;

/** Result of one validator: passed, or a failure code + message. */
public record ValidationResult(
        boolean passed,
        String failureCode,
        String failureMessage
) {

    private static final ValidationResult PASSED = new ValidationResult(true, null, null);

    public static ValidationResult pass() {
        return PASSED;
    }

    public static ValidationResult failed(String code, String message) {
        return new ValidationResult(false, code, message);
    }
}
