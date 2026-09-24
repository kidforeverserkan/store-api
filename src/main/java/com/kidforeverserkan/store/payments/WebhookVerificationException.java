package com.kidforeverserkan.store.payments;

// A webhook request whose Stripe signature is missing or doesn't verify.
// That's a bad request from the caller, not a server-side payment failure.
public class WebhookVerificationException extends RuntimeException {
    public WebhookVerificationException(String message) {
        super(message);
    }
}
