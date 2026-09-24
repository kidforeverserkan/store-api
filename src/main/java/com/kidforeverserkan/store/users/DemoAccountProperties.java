package com.kidforeverserkan.store.users;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * The shared public demo account (store.demo.account-email, set with the
 * DEMO_ACCOUNT_EMAIL env var). Its login is published so visitors can try
 * the store, so they must not be able to change its details or password or
 * delete it. Left empty, no account is protected.
 */
@Configuration
@ConfigurationProperties(prefix = "store.demo")
@Data
public class DemoAccountProperties {

    private String accountEmail;

    public boolean isDemoAccount(User user) {
        return accountEmail != null
                && !accountEmail.isBlank()
                && accountEmail.trim().equalsIgnoreCase(user.getEmail());
    }
}
