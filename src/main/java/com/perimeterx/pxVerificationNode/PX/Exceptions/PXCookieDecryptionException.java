package com.perimeterx.pxVerificationNode.PX.Exceptions;

public class PXCookieDecryptionException extends Exception {

    public PXCookieDecryptionException(Throwable cause) {
        super(cause);
    }

    public PXCookieDecryptionException(String message) {
        super(message);
    }

    public PXCookieDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
