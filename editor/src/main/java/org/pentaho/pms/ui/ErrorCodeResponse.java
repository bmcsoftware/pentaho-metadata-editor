package org.pentaho.pms.ui;

/**
 * ©Copyright 2024 BMC Software. Inc.
 */
public class ErrorCodeResponse {
    private String errorMessage;
    private String errorCode;

    public ErrorCodeResponse(String errorMessage, String errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
