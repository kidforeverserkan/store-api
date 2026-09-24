package com.kidforeverserkan.store.auth;

// GET /auth/me: the signed-in user plus whether it is the shared public demo
// account, so the storefront can show that account as read-only instead of
// offering edits the API would reject.
public record CurrentUserDto(long id, String name, String email, boolean demoAccount) {
}
