package com.example.a2aspring.controller;

import io.a2a.server.auth.User;

import java.util.Objects;

/**
 * Represents an authenticated user in the A2A system.
 * Replaces anonymous inner class for better testability and clarity.
 */
public final class AuthenticatedUser implements User {

    private final String username;

    public AuthenticatedUser(String username) {
        this.username = Objects.requireNonNull(username, "Username cannot be null");
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticatedUser that = (AuthenticatedUser) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{username='" + username + "'}";
    }
}
