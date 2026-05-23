package com.lakeserl.review_service.exception;

public class DuplicateVoteException extends RuntimeException {
    public DuplicateVoteException(String message) { super(message); }
}
