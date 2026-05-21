package com.beiming.profile.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MemberProfile {
    public final String id;
    public final String authUserId;
    public String usernameSnapshot;
    public String displayName;
    public String minecraftId;
    public String minecraftUuid;
    public String avatarUrl;
    public String skinUrl;
    public String memberGroupId;
    public MemberStatus status;
    public boolean publicVisible;
    public Instant joinedAt;
    public String bio;
    public List<MemberAchievement> achievements = new ArrayList<>();
    public List<MemberWork> works = new ArrayList<>();
    public String activitySummary;
    public String contributionSummary;
    public final Instant createdAt;
    public Instant updatedAt;

    public MemberProfile(String id, String authUserId, Instant now) {
        this.id = id;
        this.authUserId = authUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
