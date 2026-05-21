package com.beiming.auth.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserAccount {
    public final String id;
    public final String username;
    public String displayName;
    public String email;
    public String avatarUrl;
    public String passwordHash;
    public Role role;
    public List<Permission> permissions;
    public UserStatus status;
    public String minecraftId;
    public String minecraftUuid;
    public final Instant createdAt;
    public Instant updatedAt;
    public Instant lastLoginAt;

    public UserAccount(String id, String username, String displayName, String email, String passwordHash, Role role,
            List<Permission> permissions, UserStatus status, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.permissions = new ArrayList<>(permissions);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
}
