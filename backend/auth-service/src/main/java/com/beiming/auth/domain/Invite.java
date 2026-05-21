package com.beiming.auth.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Invite {
    public final String id;
    public final String codeHash;
    public final String codePrefix;
    public final InviteType type;
    public Role role;
    public List<Permission> permissions;
    public final int maxUses;
    public int usedCount;
    public InviteStatus status;
    public final Instant expiresAt;
    public final String note;
    public final String createdByUserId;
    public final Instant createdAt;
    public final List<InviteUse> uses = new ArrayList<>();

    public Invite(String id, String codeHash, String codePrefix, InviteType type, Role role, List<Permission> permissions,
            int maxUses, InviteStatus status, Instant expiresAt, String note, String createdByUserId, Instant createdAt) {
        this.id = id;
        this.codeHash = codeHash;
        this.codePrefix = codePrefix;
        this.type = type;
        this.role = role;
        this.permissions = new ArrayList<>(permissions);
        this.maxUses = maxUses;
        this.status = status;
        this.expiresAt = expiresAt;
        this.note = note;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }
}
