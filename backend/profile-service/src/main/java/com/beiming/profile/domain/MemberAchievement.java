package com.beiming.profile.domain;

import java.time.Instant;

public record MemberAchievement(String id, String title, String description, Instant occurredAt) {
}
