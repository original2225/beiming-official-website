package com.beiming.profile.domain;

import java.time.Instant;

public class MemberGroup {
    public final String id;
    public String name;
    public String description;
    public int sortOrder;
    public boolean enabled;
    public final Instant createdAt;
    public Instant updatedAt;

    public MemberGroup(String id, String name, String description, int sortOrder, boolean enabled, Instant now) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
