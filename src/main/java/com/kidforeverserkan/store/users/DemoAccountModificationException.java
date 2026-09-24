package com.kidforeverserkan.store.users;

// Someone tried to change or delete the shared public demo account.
public class DemoAccountModificationException extends RuntimeException {
    public DemoAccountModificationException() {
        super("The public demo account can't be changed or deleted.");
    }
}
