package cn.beiming.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class ContentModule {
    @Bean
    ContentStore contentStore() {
        return new ContentStore();
    }

    @Bean
    TestAuthContextProvider testAuthContextProvider() {
        return new TestAuthContextProvider();
    }

    @Bean
    TestProfileSnapshotProvider testProfileSnapshotProvider() {
        return new TestProfileSnapshotProvider();
    }

    @Bean
    TestNotificationClient testNotificationClient() {
        return new TestNotificationClient();
    }
}

@RestController
@RequestMapping("/api/v1/content")
class ContentController {
    private final ContentStore store;
    private final TestAuthContextProvider auth;
    private final TestProfileSnapshotProvider profile;
    private final TestNotificationClient notification;

    ContentController(ContentStore store, TestAuthContextProvider auth, TestProfileSnapshotProvider profile, TestNotificationClient notification) {
        this.store = store;
        this.auth = auth;
        this.profile = profile;
        this.notification = notification;
    }

    @GetMapping("/home")
    Map<String, Object> publicHome() {
        return ok(store.publicHome());
    }

    @GetMapping("/items")
    Map<String, Object> publicItems(@RequestParam Map<String, String> query) {
        return ok(store.publicItems(query));
    }

    @GetMapping("/items/{contentId}")
    Map<String, Object> publicItem(@PathVariable String contentId) {
        return ok(store.publicItem(contentId));
    }

    @GetMapping("/items/by-slug/{slug}")
    Map<String, Object> publicItemBySlug(@PathVariable String slug) {
        return ok(store.publicItem(store.requireContentIdBySlug(slug)));
    }

    @GetMapping("/categories")
    Map<String, Object> publicCategories() {
        return ok(mapOf("items", store.publicCategories()));
    }

    @GetMapping("/tags")
    Map<String, Object> publicTags() {
        return ok(mapOf("items", store.publicTags()));
    }

    @GetMapping("/topics")
    Map<String, Object> publicTopics(@RequestParam Map<String, String> query) {
        return ok(store.publicTopics(query));
    }

    @GetMapping("/topics/{topicId}")
    Map<String, Object> publicTopic(@PathVariable String topicId) {
        return ok(store.publicTopic(topicId));
    }

    @GetMapping("/topics/by-slug/{slug}")
    Map<String, Object> publicTopicBySlug(@PathVariable String slug) {
        return ok(store.publicTopic(store.requireTopicIdBySlug(slug)));
    }

    @GetMapping("/seo")
    Map<String, Object> publicSeo(@RequestParam String route) {
        return ok(store.publicSeo(route));
    }

    @GetMapping("/admin/items")
    Map<String, Object> adminItems(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminItems(query));
    }

    @GetMapping("/admin/items/{contentId}")
    Map<String, Object> adminItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String contentId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminItem(contentId));
    }

    @PostMapping("/admin/items")
    ResponseEntity<Map<String, Object>> createItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createItem(actor, profile, body)));
    }

    @PatchMapping("/admin/items/{contentId}")
    Map<String, Object> patchItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String contentId,
                                  @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchItem(actor, profile, contentId, body));
    }

    @PatchMapping("/admin/items/{contentId}/submit-review")
    Map<String, Object> submitReview(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String contentId,
                                     @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.submitReview(actor, contentId, body));
    }

    @PatchMapping("/admin/items/{contentId}/approve")
    Map<String, Object> approve(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String contentId,
                                @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.approve(actor, notification, contentId, body));
    }

    @PatchMapping("/admin/items/{contentId}/reject")
    Map<String, Object> reject(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String contentId,
                               @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.reject(actor, notification, contentId, body));
    }

    @PatchMapping("/admin/items/{contentId}/request-changes")
    Map<String, Object> requestChanges(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String contentId,
                                       @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.requestChanges(actor, notification, contentId, body));
    }

    @PatchMapping("/admin/items/{contentId}/publish")
    Map<String, Object> publishItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String contentId,
                                    @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.publish(actor, notification, contentId, body));
    }

    @PatchMapping("/admin/items/{contentId}/offline")
    Map<String, Object> offlineItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String contentId,
                                    @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.offline(actor, contentId, body));
    }

    @PatchMapping("/admin/items/{contentId}/archive")
    Map<String, Object> archiveItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String contentId,
                                    @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.archive(actor, contentId, body));
    }

    @PatchMapping("/admin/items/{contentId}/delete")
    Map<String, Object> deleteItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String contentId,
                                   @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.softDelete(actor, contentId, body));
    }

    @GetMapping("/admin/items/{contentId}/versions")
    Map<String, Object> itemVersions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String contentId,
                                     @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.itemVersions(contentId, query));
    }

    @GetMapping("/admin/items/{contentId}/versions/{version}")
    Map<String, Object> itemVersion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String contentId,
                                    @PathVariable int version) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.itemVersion(contentId, version));
    }

    @PatchMapping("/admin/items/{contentId}/versions/{version}/restore")
    Map<String, Object> restoreItemVersion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable String contentId,
                                           @PathVariable int version,
                                           @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.restoreItemVersion(actor, contentId, version, body));
    }

    @GetMapping("/admin/items/{contentId}/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String contentId,
                                  @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(contentId, query));
    }

    @GetMapping("/admin/home")
    Map<String, Object> adminHome(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminHome());
    }

    @PutMapping("/admin/home")
    Map<String, Object> saveHome(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.saveHome(actor, body));
    }

    @PostMapping("/admin/home/preview")
    Map<String, Object> previewHome(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody Map<String, Object> body) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.previewHome(body));
    }

    @PatchMapping("/admin/home/publish")
    Map<String, Object> publishHome(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.publishHome(actor, body));
    }

    @PatchMapping("/admin/home/rollback")
    Map<String, Object> rollbackHome(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.rollbackHome(actor, body));
    }

    @GetMapping("/admin/categories")
    Map<String, Object> adminCategories(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(mapOf("items", store.adminCategories(query)));
    }

    @PostMapping("/admin/categories")
    ResponseEntity<Map<String, Object>> createCategory(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createCategory(actor, body)));
    }

    @PatchMapping("/admin/categories/{categoryId}")
    Map<String, Object> patchCategory(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String categoryId,
                                      @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchCategory(actor, categoryId, body));
    }

    @PatchMapping("/admin/categories/{categoryId}/archive")
    Map<String, Object> archiveCategory(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable String categoryId,
                                        @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.archiveCategory(actor, categoryId, body));
    }

    @GetMapping("/admin/tags")
    Map<String, Object> adminTags(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(mapOf("items", store.adminTags(query)));
    }

    @PostMapping("/admin/tags")
    ResponseEntity<Map<String, Object>> createTag(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createTag(actor, body)));
    }

    @PatchMapping("/admin/tags/{tagId}")
    Map<String, Object> patchTag(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String tagId,
                                 @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchTag(actor, tagId, body));
    }

    @PatchMapping("/admin/tags/{tagId}/archive")
    Map<String, Object> archiveTag(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String tagId,
                                   @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.archiveTag(actor, tagId, body));
    }

    @GetMapping("/admin/topics")
    Map<String, Object> adminTopics(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminTopics(query));
    }

    @GetMapping("/admin/topics/{topicId}")
    Map<String, Object> adminTopic(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String topicId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminTopic(topicId));
    }

    @PostMapping("/admin/topics")
    ResponseEntity<Map<String, Object>> createTopic(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createTopic(actor, body)));
    }

    @PatchMapping("/admin/topics/{topicId}")
    Map<String, Object> patchTopic(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String topicId,
                                   @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchTopic(actor, topicId, body));
    }

    @PatchMapping("/admin/topics/{topicId}/publish")
    Map<String, Object> publishTopic(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String topicId,
                                     @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.publishTopic(actor, topicId, body));
    }

    @PatchMapping("/admin/topics/{topicId}/offline")
    Map<String, Object> offlineTopic(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String topicId,
                                     @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.offlineTopic(actor, topicId, body));
    }

    @PatchMapping("/admin/topics/{topicId}/archive")
    Map<String, Object> archiveTopic(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String topicId,
                                     @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.archiveTopic(actor, topicId, body));
    }

    @PatchMapping("/admin/topics/{topicId}/delete")
    Map<String, Object> deleteTopic(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String topicId,
                                    @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.deleteTopic(actor, topicId, body));
    }

    @GetMapping("/admin/seo")
    Map<String, Object> adminSeoList(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminSeoList(query));
    }

    @GetMapping("/admin/seo/{seoId}")
    Map<String, Object> adminSeo(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String seoId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminSeo(seoId));
    }

    @PutMapping("/admin/seo/by-route")
    Map<String, Object> saveSeo(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.saveSeo(actor, body));
    }

    @PatchMapping("/admin/seo/{seoId}/disable")
    Map<String, Object> disableSeo(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String seoId,
                                   @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.disableSeo(actor, seoId, body));
    }

    @GetMapping("/admin/ops/summary")
    Map<String, Object> opsSummary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.opsSummary());
    }

    static Map<String, Object> ok(Object data) {
        return okData(data);
    }

    static Map<String, Object> okData(Object data) {
        return mapOf("code", 0, "message", "success", "data", data);
    }

    static Map<String, Object> envelope(int code, String message, Object data) {
        return mapOf("code", code, "message", message, "data", data);
    }

    static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

class ContentStore {
    private final Map<String, ContentItem> items = new LinkedHashMap<>();
    private final Map<String, ContentCategoryRecord> categories = new LinkedHashMap<>();
    private final Map<String, ContentTagRecord> tags = new LinkedHashMap<>();
    private final Map<String, TopicRecord> topics = new LinkedHashMap<>();
    private final Map<String, SeoRecord> seo = new LinkedHashMap<>();
    private final Map<Integer, HomeConfig> publishedHomeVersions = new LinkedHashMap<>();
    private final Map<String, List<ContentItemVersionRecord>> itemVersions = new LinkedHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final List<ContentAudit> audits = new ArrayList<>();
    private HomeConfig draftHome;
    private HomeConfig publishedHome;
    private boolean failNextAudit;
    private boolean failNextHomeRead;
    private int nextHomeVersion = 2;

    synchronized void reset() {
        items.clear();
        categories.clear();
        tags.clear();
        topics.clear();
        seo.clear();
        publishedHomeVersions.clear();
        itemVersions.clear();
        idempotency.clear();
        audits.clear();
        draftHome = null;
        publishedHome = null;
        failNextAudit = false;
        failNextHomeRead = false;
        nextHomeVersion = 2;
    }

    synchronized void seedTestData(TestProfileSnapshotProvider profile) {
        reset();
        ContentCategoryRecord news = new ContentCategoryRecord("cat_news", "News", "news", "News category", 1, false, now(), now());
        ContentCategoryRecord archivedCategory = new ContentCategoryRecord("cat_archived", "Archived", "archived-category", "Old", 99, true, now(), now());
        categories.put(news.categoryId, news);
        categories.put(archivedCategory.categoryId, archivedCategory);
        ContentTagRecord guide = new ContentTagRecord("tag_guide", "Guide", "guide", false, now(), now());
        ContentTagRecord archivedTag = new ContentTagRecord("tag_archived", "Archived", "archived-tag", true, now(), now());
        tags.put(guide.tagId, guide);
        tags.put(archivedTag.tagId, archivedTag);

        seedItem("guide-article", "ARTICLE", "APPROVED", "PUBLIC", "Guide Article", "Guide summary", "Body <script>alert(1)</script>", news.categoryId, List.of(guide.tagId), true, "user", null);
        seedItem("announcement-public", "ANNOUNCEMENT", "APPROVED", "PUBLIC", "Announcement", "Public announcement", "Announcement body", news.categoryId, List.of(guide.tagId), true, "user", null);
        seedItem("photo-public", "PHOTO", "APPROVED", "PUBLIC", "Photo Moment", "Photo summary", "Photo body", news.categoryId, List.of(guide.tagId), true, "user", null);
        seedItem("member-work-public", "MEMBER_WORK", "APPROVED", "PUBLIC", "Member Work", "Work summary", "Work body", news.categoryId, List.of(guide.tagId), true, "user", profile.snapshot("memberActive"));
        seedItem("draft-only", "ARTICLE", "DRAFT", "PUBLIC", "Draft Only", "Draft", "Draft body", news.categoryId, List.of(guide.tagId), false, "user", null);
        seedItem("pending-only", "ARTICLE", "PENDING_REVIEW", "PUBLIC", "Pending Only", "Pending", "Pending body", news.categoryId, List.of(guide.tagId), false, "user", null);
        seedItem("pending-reject", "ARTICLE", "PENDING_REVIEW", "PUBLIC", "Pending Reject", "Pending", "Pending body", news.categoryId, List.of(guide.tagId), false, "user", null);
        seedItem("pending-changes", "ARTICLE", "PENDING_REVIEW", "PUBLIC", "Pending Changes", "Pending", "Pending body", news.categoryId, List.of(guide.tagId), false, "user", null);
        seedItem("rejected-only", "ARTICLE", "REJECTED", "PUBLIC", "Rejected Only", "Rejected", "Rejected body", news.categoryId, List.of(guide.tagId), false, "user", null);
        seedItem("needs-changes-only", "ARTICLE", "NEEDS_CHANGES", "PUBLIC", "Needs Changes Only", "Needs", "Needs body", news.categoryId, List.of(guide.tagId), false, "user", null);
        seedItem("offline-only", "ARTICLE", "OFFLINE", "PUBLIC", "Offline Only", "Offline", "Offline body", news.categoryId, List.of(guide.tagId), true, "user", null);
        seedItem("archived-only", "ARTICLE", "ARCHIVED", "PUBLIC", "Archived Only", "Archived", "Archived body", news.categoryId, List.of(guide.tagId), false, "user", null);
        seedItem("deleted-only", "ARTICLE", "DELETED", "PUBLIC", "Deleted Only", "Deleted", "Deleted body", news.categoryId, List.of(guide.tagId), false, "user", null).deletedAt = now();
        seedItem("private-only", "ARTICLE", "APPROVED", "PRIVATE", "Private Only", "Private", "Private body", news.categoryId, List.of(guide.tagId), true, "user", null);
        seedItem("member-only", "ARTICLE", "APPROVED", "MEMBER_ONLY", "Member Only", "Member", "Member body", news.categoryId, List.of(guide.tagId), true, "user", null);
        ContentItem future = seedItem("future-only", "ARTICLE", "APPROVED", "PUBLIC", "Future Only", "Future", "Future body", news.categoryId, List.of(guide.tagId), true, "user", null);
        future.visibleFrom = now().plus(1, ChronoUnit.DAYS);
        ContentItem expired = seedItem("expired-only", "ARTICLE", "APPROVED", "PUBLIC", "Expired Only", "Expired", "Expired body", news.categoryId, List.of(guide.tagId), true, "user", null);
        expired.visibleUntil = now().minus(1, ChronoUnit.DAYS);
        seedItem("delete-draft", "ARTICLE", "DRAFT", "PUBLIC", "Delete Draft", "Delete", "Delete body", news.categoryId, List.of(guide.tagId), false, "user", null);

        TopicRecord spring = new TopicRecord("topic_spring", "spring-topic", "Spring Topic", "Spring summary", "/topics/spring.png", "APPROVED", "PUBLIC", new ArrayList<>(List.of(contentIdBySlug("guide-article"), contentIdBySlug("offline-only"))), seoPayload(null, "/topics/spring-topic"), now().minus(1, ChronoUnit.HOURS), now(), now(), null);
        topics.put(spring.topicId, spring);
        TopicRecord draft = new TopicRecord("topic_draft", "draft-topic", "Draft Topic", "Draft summary", "/topics/draft.png", "DRAFT", "PUBLIC", new ArrayList<>(List.of(contentIdBySlug("guide-article"))), null, null, now(), now(), null);
        topics.put(draft.topicId, draft);

        SeoRecord newsSeo = new SeoRecord("seo_news", "/news", "News SEO", "News description", List.of("beiming", "news"), "/seo.png", "INDEX_FOLLOW", "https://example.com/news", true, now());
        seo.put(newsSeo.seoId, newsSeo);

        publishedHome = new HomeConfig("home_pub", 1, List.of(
                section("home_hero", "HERO", "Hero", List.of(contentIdBySlug("guide-article"))),
                section("home_server", "SERVER_ENTRY", "Server", List.of()),
                section("home_resource", "RESOURCE_ENTRY", "Resource", List.of()),
                section("home_topics", "TOPICS", "Topics", List.of("topic_spring"))
        ), seoPayload(null, "/"), now());
        publishedHomeVersions.put(1, publishedHome.copy());
        draftHome = new HomeConfig("home_draft", 0, publishedHome.sections, publishedHome.seo, null);
        audit("system", "CONTENT_SEEDED", "seed", "content", "SUCCESS");
    }

    private ContentItem seedItem(String slug, String type, String status, String visibility, String title, String summary, String body, String categoryId, List<String> tagIds, boolean published, String authorUserId, Map<String, Object> memberSnapshot) {
        ContentItem item = new ContentItem("content_" + slug.replace("/", "_"), type, status, visibility, slug, title, summary, body, "/covers/" + slug + ".png", categoryId, new ArrayList<>(tagIds), authorUserId, authorUserId == null ? null : "User", memberSnapshot, seoPayload(null, "/content/" + slug), "admin note", null, null, null, null, published ? now().minus(2, ChronoUnit.HOURS) : null, null, null, "seed", "seed", now(), now(), null);
        items.put(item.contentId, item);
        recordVersion(item, "seed", "CREATED", "seed", null);
        return item;
    }

    synchronized Map<String, Object> publicHome() {
        if (failNextHomeRead) {
            failNextHomeRead = false;
            throw new ContentException(51400, HttpStatus.INTERNAL_SERVER_ERROR, "content internal error");
        }
        if (publishedHome == null) {
            return mapOf("homeConfigId", "default", "version", 0, "sections", List.of(), "degraded", true, "degradeReasons", List.of("NO_PUBLISHED_HOME_CONFIG"), "publishedAt", now().toString(), "seo", defaultSeo("/"));
        }
        Map<String, Object> view = homeView(publishedHome);
        return view;
    }

    synchronized Map<String, Object> publicItems(Map<String, String> query) {
        Query page = query(query, Set.of("publishedAt_desc", "publishedAt_asc", "updatedAt_desc", "title_asc"), "publishedAt_desc");
        List<Map<String, Object>> rows = items.values().stream()
                .filter(this::publicVisible)
                .filter(item -> match(query.get("type"), item.type))
                .filter(item -> match(query.get("categoryId"), item.categoryId))
                .filter(item -> query.get("tag") == null || item.tagIds.stream().map(this::tag).anyMatch(tag -> tag.slug.equals(query.get("tag")) || tag.name.equals(query.get("tag"))))
                .filter(item -> keyword(item, query.get("keyword")))
                .sorted(itemComparator(page.sort))
                .map(this::summaryMap)
                .toList();
        return page(rows, page.page, page.pageSize);
    }

    synchronized Map<String, Object> publicItem(String contentId) {
        ContentItem item = requireItem(contentId);
        if (!publicVisible(item)) {
            throw new ContentException(43412, HttpStatus.CONFLICT, "content is not publicly accessible");
        }
        return detailMap(item);
    }

    synchronized List<Map<String, Object>> publicCategories() {
        return categories.values().stream().filter(category -> !category.archived).sorted(Comparator.comparingInt(category -> category.sortOrder)).map(this::categoryMap).toList();
    }

    synchronized List<Map<String, Object>> publicTags() {
        return tags.values().stream().filter(tag -> !tag.archived).sorted(Comparator.comparing(tag -> tag.name)).map(this::tagMap).toList();
    }

    synchronized Map<String, Object> publicTopics(Map<String, String> query) {
        Query page = query(query, Set.of("publishedAt_desc", "updatedAt_desc", "title_asc"), "publishedAt_desc");
        List<Map<String, Object>> rows = topics.values().stream()
                .filter(this::publicTopicVisible)
                .filter(topic -> keyword(topic.title + " " + topic.summary, query.get("keyword")))
                .sorted(topicComparator(page.sort))
                .map(this::topicMap)
                .toList();
        return page(rows, page.page, page.pageSize);
    }

    synchronized Map<String, Object> publicTopic(String topicId) {
        TopicRecord topic = requireTopic(topicId);
        if (!publicTopicVisible(topic)) {
            throw new ContentException(43414, HttpStatus.CONFLICT, "topic state conflict");
        }
        return topicMap(topic);
    }

    synchronized Map<String, Object> publicSeo(String route) {
        validateRoute(route);
        return seo.values().stream().filter(record -> record.enabled && record.route.equals(route)).findFirst().map(this::seoMap).orElse(defaultSeo(route));
    }

    synchronized Map<String, Object> adminItems(Map<String, String> query) {
        Query page = query(query, Set.of("createdAt_desc", "updatedAt_desc", "publishedAt_desc", "title_asc"), "createdAt_desc");
        List<Map<String, Object>> rows = items.values().stream()
                .filter(item -> match(query.get("type"), item.type))
                .filter(item -> match(query.get("status"), item.status))
                .filter(item -> match(query.get("visibility"), item.visibility))
                .filter(item -> match(query.get("categoryId"), item.categoryId))
                .filter(item -> query.get("tagId") == null || item.tagIds.contains(query.get("tagId")))
                .filter(item -> match(query.get("createdBy"), item.createdBy))
                .filter(item -> keyword(item, query.get("keyword")) || keyword(item.adminNote, query.get("keyword")))
                .sorted(itemComparator(page.sort))
                .map(this::adminItemMap)
                .toList();
        return page(rows, page.page, page.pageSize);
    }

    synchronized Map<String, Object> adminItem(String contentId) {
        return adminItemMap(requireItem(contentId));
    }

    synchronized Map<String, Object> createItem(AuthUser actor, TestProfileSnapshotProvider profile, Map<String, Object> body) {
        String key = optionalString(body, "idempotencyKey");
        String idemKey = idempotencyKey(actor, "create-item", key);
        if (key != null && idempotency.containsKey(idemKey)) {
            return replay(idemKey, body);
        }
        validateCreateBody(body);
        String slug = requiredString(body, "slug");
        if (contentIdBySlug(slug) != null) {
            throw new ContentException(43411, HttpStatus.CONFLICT, "slug conflict");
        }
        checkAudit();
        ContentItem item = itemFromBody("content_" + UUID.randomUUID(), actor, body);
        item.status = "DRAFT";
        item.createdBy = actor.userId;
        item.updatedBy = actor.userId;
        item.memberSnapshot = memberSnapshot(profile, item.type, optionalString(body, "memberId"));
        items.put(item.contentId, item);
        recordVersion(item, actor.userId, "CREATED", body.get("reason"), null);
        audit(actor.userId, "CONTENT_ITEM_CREATED", body.get("reason"), item.contentId, "SUCCESS");
        Map<String, Object> result = adminItemMap(item);
        remember(idemKey, body, result);
        return result;
    }

    synchronized Map<String, Object> patchItem(AuthUser actor, TestProfileSnapshotProvider profile, String contentId, Map<String, Object> body) {
        ContentItem item = requireItem(contentId);
        if (Set.of("ARCHIVED", "DELETED").contains(item.status)) {
            throw new ContentException(43410, HttpStatus.CONFLICT, "content state conflict");
        }
        requireReason(body);
        if (body.containsKey("slug")) {
            String slug = requiredString(body, "slug");
            validateSlug(slug);
            String existing = contentIdBySlug(slug);
            if (existing != null && !existing.equals(contentId)) {
                throw new ContentException(43411, HttpStatus.CONFLICT, "slug conflict");
            }
        }
        validateCategoryAndTags(body);
        checkAudit();
        if (body.containsKey("slug")) item.slug = requiredString(body, "slug");
        if (body.containsKey("title")) item.title = requiredString(body, "title");
        if (body.containsKey("summary")) item.summary = optionalString(body, "summary");
        if (body.containsKey("body")) item.body = requiredString(body, "body");
        if (body.containsKey("coverUrl")) item.coverUrl = optionalUrl(body, "coverUrl");
        if (body.containsKey("categoryId")) item.categoryId = optionalString(body, "categoryId");
        if (body.containsKey("tagIds")) item.tagIds = tagIds(body);
        if (body.containsKey("visibility")) item.visibility = enumValue(body, "visibility", Set.of("PUBLIC", "MEMBER_ONLY", "PRIVATE"));
        if (body.containsKey("memberId")) item.memberSnapshot = memberSnapshot(profile, item.type, optionalString(body, "memberId"));
        item.updatedBy = actor.userId;
        item.updatedAt = now();
        recordVersion(item, actor.userId, "UPDATED", body.get("reason"), null);
        audit(actor.userId, "CONTENT_ITEM_UPDATED", body.get("reason"), item.contentId, "SUCCESS");
        return adminItemMap(item);
    }

    synchronized Map<String, Object> submitReview(AuthUser actor, String contentId, Map<String, Object> body) {
        ContentItem item = requireItem(contentId);
        requireReason(body);
        if ("PENDING_REVIEW".equals(item.status)) return adminItemMap(item);
        if (!Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES").contains(item.status)) {
            throw new ContentException(43410, HttpStatus.CONFLICT, "content state conflict");
        }
        checkAudit();
        item.status = "PENDING_REVIEW";
        item.submittedAt = now();
        item.updatedAt = now();
        audit(actor.userId, "CONTENT_ITEM_SUBMITTED", body.get("reason"), item.contentId, "SUCCESS");
        return adminItemMap(item);
    }

    synchronized Map<String, Object> approve(AuthUser actor, TestNotificationClient notification, String contentId, Map<String, Object> body) {
        ContentItem item = requireItem(contentId);
        String opinion = requiredString(body, "reviewOpinion");
        requireReason(body);
        if ("APPROVED".equals(item.status)) return adminItemMap(item);
        if (!"PENDING_REVIEW".equals(item.status)) throw new ContentException(43410, HttpStatus.CONFLICT, "content state conflict");
        notifyRequired(notification, item, "CONTENT_REVIEW_APPROVED");
        checkAudit();
        item.status = "APPROVED";
        item.reviewOpinion = opinion;
        item.reviewedAt = now();
        item.updatedAt = now();
        item.notificationStatus = item.authorUserId == null ? "NO_AUTHOR_TO_NOTIFY" : "DELIVERED";
        audit(actor.userId, "CONTENT_ITEM_APPROVED", body.get("reason"), item.contentId, "SUCCESS");
        return adminItemMap(item);
    }

    synchronized Map<String, Object> reject(AuthUser actor, TestNotificationClient notification, String contentId, Map<String, Object> body) {
        return reviewTo(actor, notification, contentId, body, "REJECTED", "CONTENT_ITEM_REJECTED", "CONTENT_REVIEW_REJECTED");
    }

    synchronized Map<String, Object> requestChanges(AuthUser actor, TestNotificationClient notification, String contentId, Map<String, Object> body) {
        return reviewTo(actor, notification, contentId, body, "NEEDS_CHANGES", "CONTENT_ITEM_CHANGES_REQUESTED", "CONTENT_REVIEW_CHANGES_REQUESTED");
    }

    private Map<String, Object> reviewTo(AuthUser actor, TestNotificationClient notification, String contentId, Map<String, Object> body, String targetStatus, String auditAction, String notifyType) {
        ContentItem item = requireItem(contentId);
        String opinion = requiredString(body, "reviewOpinion");
        requireReason(body);
        if (targetStatus.equals(item.status)) return adminItemMap(item);
        if (!"PENDING_REVIEW".equals(item.status)) throw new ContentException(43410, HttpStatus.CONFLICT, "content state conflict");
        notifyRequired(notification, item, notifyType);
        checkAudit();
        item.status = targetStatus;
        item.reviewOpinion = opinion;
        item.reviewedAt = now();
        item.updatedAt = now();
        item.notificationStatus = item.authorUserId == null ? "NO_AUTHOR_TO_NOTIFY" : "DELIVERED";
        audit(actor.userId, auditAction, body.get("reason"), item.contentId, "SUCCESS");
        return adminItemMap(item);
    }

    synchronized Map<String, Object> publish(AuthUser actor, TestNotificationClient notification, String contentId, Map<String, Object> body) {
        ContentItem item = requireItem(contentId);
        requireReason(body);
        if (!Set.of("APPROVED", "OFFLINE").contains(item.status)) throw new ContentException(43410, HttpStatus.CONFLICT, "content state conflict");
        boolean changed = "OFFLINE".equals(item.status) || item.publishedAt == null;
        if (!changed) return adminItemMap(item);
        checkAudit();
        item.status = "APPROVED";
        if (item.publishedAt == null) item.publishedAt = now();
        item.visibleFrom = optionalInstant(body, "visibleFrom", item.visibleFrom);
        item.visibleUntil = optionalInstant(body, "visibleUntil", item.visibleUntil);
        try {
            notification.optional(item.authorUserId, "CONTENT_PUBLISHED");
            item.notificationStatus = item.authorUserId == null ? "NO_AUTHOR_TO_NOTIFY" : "DELIVERED";
        } catch (ContentException exception) {
            item.notificationStatus = "FAILED:" + exception.getMessage();
        }
        item.updatedAt = now();
        recordVersion(item, actor.userId, "PUBLISHED", body.get("reason"), null);
        audit(actor.userId, "CONTENT_ITEM_PUBLISHED", body.get("reason"), item.contentId, "SUCCESS");
        return adminItemMap(item);
    }

    synchronized Map<String, Object> itemVersions(String contentId, Map<String, String> query) {
        requireItem(contentId);
        Query page = query(query, Set.of("version_desc", "version_asc"), "version_desc");
        Comparator<ContentItemVersionRecord> comparator = Comparator.comparingInt(record -> record.version);
        if ("version_desc".equals(page.sort)) comparator = comparator.reversed();
        List<Map<String, Object>> rows = itemVersions.getOrDefault(contentId, List.of()).stream()
                .sorted(comparator)
                .map(this::versionMap)
                .toList();
        return page(rows, page.page, page.pageSize);
    }

    synchronized Map<String, Object> itemVersion(String contentId, int version) {
        requireItem(contentId);
        return versionMap(requireVersion(contentId, version));
    }

    synchronized Map<String, Object> restoreItemVersion(AuthUser actor, String contentId, int version, Map<String, Object> body) {
        ContentItem item = requireItem(contentId);
        requireReason(body);
        if (Set.of("ARCHIVED", "DELETED").contains(item.status)) {
            throw new ContentException(43418, HttpStatus.CONFLICT, "content version state conflict");
        }
        ContentItemVersionRecord record = requireVersion(contentId, version);
        String existing = contentIdBySlug(record.snapshot.slug);
        if (existing != null && !existing.equals(contentId)) {
            throw new ContentException(43411, HttpStatus.CONFLICT, "slug conflict");
        }
        checkAudit();
        ContentItem snapshot = record.snapshot;
        item.visibility = snapshot.visibility;
        item.slug = snapshot.slug;
        item.title = snapshot.title;
        item.summary = snapshot.summary;
        item.body = snapshot.body;
        item.coverUrl = snapshot.coverUrl;
        item.categoryId = snapshot.categoryId;
        item.tagIds = new ArrayList<>(snapshot.tagIds);
        item.memberSnapshot = copyMap(snapshot.memberSnapshot);
        item.seo = copyMap(snapshot.seo);
        item.adminNote = snapshot.adminNote;
        item.visibleFrom = snapshot.visibleFrom;
        item.visibleUntil = snapshot.visibleUntil;
        item.status = "DRAFT";
        item.submittedAt = null;
        item.reviewedAt = null;
        item.publishedAt = null;
        item.reviewOpinion = null;
        item.notificationStatus = null;
        item.deletedAt = null;
        item.updatedBy = actor.userId;
        item.updatedAt = now();
        int newVersion = recordVersion(item, actor.userId, "RESTORED", body.get("reason"), version);
        audit(actor.userId, "CONTENT_ITEM_VERSION_RESTORED", body.get("reason"), item.contentId, "SUCCESS",
                mapOf("sourceVersion", version, "newVersion", newVersion));
        return adminItemMap(item);
    }

    synchronized Map<String, Object> offline(AuthUser actor, String contentId, Map<String, Object> body) {
        ContentItem item = requireItem(contentId);
        requireReason(body);
        if ("OFFLINE".equals(item.status)) return adminItemMap(item);
        if (!"APPROVED".equals(item.status) || item.publishedAt == null) throw new ContentException(43410, HttpStatus.CONFLICT, "content state conflict");
        checkAudit();
        item.status = "OFFLINE";
        item.updatedAt = now();
        audit(actor.userId, "CONTENT_ITEM_OFFLINE", body.get("reason"), item.contentId, "SUCCESS");
        return adminItemMap(item);
    }

    synchronized Map<String, Object> archive(AuthUser actor, String contentId, Map<String, Object> body) {
        ContentItem item = requireItem(contentId);
        requireReason(body);
        if ("ARCHIVED".equals(item.status)) return adminItemMap(item);
        if ("APPROVED".equals(item.status) && item.publishedAt != null) throw new ContentException(43410, HttpStatus.CONFLICT, "content state conflict");
        if (!Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "OFFLINE").contains(item.status)) throw new ContentException(43410, HttpStatus.CONFLICT, "content state conflict");
        checkAudit();
        item.status = "ARCHIVED";
        item.updatedAt = now();
        audit(actor.userId, "CONTENT_ITEM_ARCHIVED", body.get("reason"), item.contentId, "SUCCESS");
        return adminItemMap(item);
    }

    synchronized Map<String, Object> softDelete(AuthUser actor, String contentId, Map<String, Object> body) {
        ContentItem item = requireItem(contentId);
        requireReason(body);
        if ("DELETED".equals(item.status)) return adminItemMap(item);
        if ("APPROVED".equals(item.status) && item.publishedAt != null) throw new ContentException(43410, HttpStatus.CONFLICT, "content state conflict");
        checkAudit();
        item.status = "DELETED";
        item.deletedAt = now();
        item.updatedAt = now();
        audit(actor.userId, "CONTENT_ITEM_DELETED", body.get("reason"), item.contentId, "SUCCESS");
        return adminItemMap(item);
    }

    synchronized Map<String, Object> auditLogs(String contentId, Map<String, String> query) {
        requireItem(contentId);
        Query page = query(query, Set.of("createdAt_desc"), "createdAt_desc");
        List<Map<String, Object>> rows = audits.stream().filter(audit -> audit.targetId.equals(contentId))
                .sorted(Comparator.comparing((ContentAudit audit) -> audit.createdAt).reversed())
                .map(this::auditMap).toList();
        return page(rows, page.page, page.pageSize);
    }

    synchronized Map<String, Object> adminHome() {
        return mapOf("draft", draftHome == null ? null : adminHomeMap(draftHome), "published", publishedHome == null ? null : homeView(publishedHome), "versions", publishedHomeVersions.keySet());
    }

    synchronized Map<String, Object> saveHome(AuthUser actor, Map<String, Object> body) {
        String key = optionalString(body, "idempotencyKey");
        String idemKey = idempotencyKey(actor, "save-home", key);
        if (key != null && idempotency.containsKey(idemKey)) return replay(idemKey, body);
        requireReason(body);
        List<Map<String, Object>> sections = sections(body);
        if (sections.size() > 20) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        checkAudit();
        draftHome = new HomeConfig("home_draft_" + UUID.randomUUID(), 0, sections, map(body.get("seo")), null);
        Map<String, Object> result = mapOf("draft", adminHomeMap(draftHome), "published", publishedHome == null ? null : homeView(publishedHome));
        audit(actor.userId, "CONTENT_HOME_DRAFT_SAVED", body.get("reason"), draftHome.homeConfigId, "SUCCESS");
        remember(idemKey, body, result);
        return result;
    }

    synchronized Map<String, Object> previewHome(Map<String, Object> body) {
        List<Map<String, Object>> sections = sections(body);
        if (sections.size() > 20) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        HomeConfig preview = new HomeConfig("preview", 0, sections, map(body.get("seo")), null);
        Map<String, Object> result = homeView(preview);
        result.put("createdPublishedVersion", false);
        return result;
    }

    synchronized Map<String, Object> publishHome(AuthUser actor, Map<String, Object> body) {
        requireReason(body);
        if (draftHome == null) throw new ContentException(43404, HttpStatus.NOT_FOUND, "home config not found");
        checkAudit();
        if (publishedHome != null && Objects.equals(publishedHome.sections, draftHome.sections)) {
            return mapOf("published", homeView(publishedHome));
        }
        publishedHome = new HomeConfig("home_pub_" + UUID.randomUUID(), nextHomeVersion++, draftHome.sections, draftHome.seo, now());
        publishedHomeVersions.put(publishedHome.version, publishedHome.copy());
        audit(actor.userId, "CONTENT_HOME_PUBLISHED", body.get("reason"), publishedHome.homeConfigId, "SUCCESS");
        return mapOf("published", homeView(publishedHome));
    }

    synchronized Map<String, Object> rollbackHome(AuthUser actor, Map<String, Object> body) {
        requireReason(body);
        Integer version = integer(body, "version");
        HomeConfig target = publishedHomeVersions.get(version);
        if (target == null) throw new ContentException(43404, HttpStatus.NOT_FOUND, "home config not found");
        checkAudit();
        publishedHome = target.copy();
        audit(actor.userId, "CONTENT_HOME_ROLLED_BACK", body.get("reason"), publishedHome.homeConfigId, "SUCCESS");
        return mapOf("published", homeView(publishedHome));
    }

    synchronized List<Map<String, Object>> adminCategories(Map<String, String> query) {
        boolean includeArchived = !"false".equalsIgnoreCase(query.getOrDefault("includeArchived", "true"));
        return categories.values().stream()
                .filter(category -> includeArchived || !category.archived)
                .sorted(Comparator.comparingInt(category -> category.sortOrder))
                .map(this::categoryMap)
                .toList();
    }

    synchronized Map<String, Object> createCategory(AuthUser actor, Map<String, Object> body) {
        String key = optionalString(body, "idempotencyKey");
        String idemKey = idempotencyKey(actor, "create-category", key);
        if (key != null && idempotency.containsKey(idemKey)) return replay(idemKey, body);
        requireReason(body);
        String name = requiredString(body, "name");
        String slug = requiredString(body, "slug");
        if (name.length() < 2 || !validSlug(slug)) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        if (categories.values().stream().filter(c -> !c.archived).anyMatch(c -> c.name.equals(name) || c.slug.equals(slug))) throw new ContentException(43001, HttpStatus.CONFLICT, "resource state conflict");
        checkAudit();
        ContentCategoryRecord category = new ContentCategoryRecord("cat_" + UUID.randomUUID(), name, slug, optionalString(body, "description"), optionalInt(body, "sortOrder", 100), false, now(), now());
        categories.put(category.categoryId, category);
        audit(actor.userId, "CONTENT_CATEGORY_CREATED", body.get("reason"), category.categoryId, "SUCCESS");
        Map<String, Object> result = categoryMap(category);
        remember(idemKey, body, result);
        return result;
    }

    synchronized Map<String, Object> patchCategory(AuthUser actor, String categoryId, Map<String, Object> body) {
        ContentCategoryRecord category = requireCategory(categoryId);
        requireReason(body);
        String name = optionalString(body, "name");
        String slug = optionalString(body, "slug");
        if ((name != null && name.length() < 2) || (slug != null && !validSlug(slug))) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        if (categories.values().stream().filter(c -> !c.categoryId.equals(categoryId) && !c.archived).anyMatch(c -> Objects.equals(c.name, name) || Objects.equals(c.slug, slug))) throw new ContentException(43001, HttpStatus.CONFLICT, "resource state conflict");
        checkAudit();
        if (name != null) category.name = name;
        if (slug != null) category.slug = slug;
        if (body.containsKey("description")) category.description = optionalString(body, "description");
        if (body.containsKey("sortOrder")) category.sortOrder = integer(body, "sortOrder");
        category.updatedAt = now();
        audit(actor.userId, "CONTENT_CATEGORY_UPDATED", body.get("reason"), categoryId, "SUCCESS");
        return categoryMap(category);
    }

    synchronized Map<String, Object> archiveCategory(AuthUser actor, String categoryId, Map<String, Object> body) {
        ContentCategoryRecord category = requireCategory(categoryId);
        requireReason(body);
        if (category.archived) return categoryMap(category);
        if (items.values().stream().anyMatch(item -> categoryId.equals(item.categoryId) && !Set.of("ARCHIVED", "DELETED").contains(item.status))) throw new ContentException(43415, HttpStatus.CONFLICT, "category is used");
        checkAudit();
        category.archived = true;
        category.updatedAt = now();
        audit(actor.userId, "CONTENT_CATEGORY_ARCHIVED", body.get("reason"), categoryId, "SUCCESS");
        return categoryMap(category);
    }

    synchronized List<Map<String, Object>> adminTags(Map<String, String> query) {
        boolean includeArchived = !"false".equalsIgnoreCase(query.getOrDefault("includeArchived", "true"));
        return tags.values().stream()
                .filter(tag -> includeArchived || !tag.archived)
                .sorted(Comparator.comparing(tag -> tag.name))
                .map(this::tagMap)
                .toList();
    }

    synchronized Map<String, Object> createTag(AuthUser actor, Map<String, Object> body) {
        String key = optionalString(body, "idempotencyKey");
        String idemKey = idempotencyKey(actor, "create-tag", key);
        if (key != null && idempotency.containsKey(idemKey)) return replay(idemKey, body);
        requireReason(body);
        String name = requiredString(body, "name");
        String slug = requiredString(body, "slug");
        if (name.isBlank() || !validSlug(slug)) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        if (tags.values().stream().filter(t -> !t.archived).anyMatch(t -> t.name.equals(name) || t.slug.equals(slug))) throw new ContentException(43001, HttpStatus.CONFLICT, "resource state conflict");
        checkAudit();
        ContentTagRecord tag = new ContentTagRecord("tag_" + UUID.randomUUID(), name, slug, false, now(), now());
        tags.put(tag.tagId, tag);
        audit(actor.userId, "CONTENT_TAG_CREATED", body.get("reason"), tag.tagId, "SUCCESS");
        Map<String, Object> result = tagMap(tag);
        remember(idemKey, body, result);
        return result;
    }

    synchronized Map<String, Object> patchTag(AuthUser actor, String tagId, Map<String, Object> body) {
        ContentTagRecord tag = requireTag(tagId);
        requireReason(body);
        String name = optionalString(body, "name");
        String slug = optionalString(body, "slug");
        if ((name != null && name.isBlank()) || (slug != null && !validSlug(slug))) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        if (tags.values().stream().filter(t -> !t.tagId.equals(tagId) && !t.archived).anyMatch(t -> Objects.equals(t.name, name) || Objects.equals(t.slug, slug))) throw new ContentException(43001, HttpStatus.CONFLICT, "resource state conflict");
        checkAudit();
        if (name != null) tag.name = name;
        if (slug != null) tag.slug = slug;
        tag.updatedAt = now();
        audit(actor.userId, "CONTENT_TAG_UPDATED", body.get("reason"), tagId, "SUCCESS");
        return tagMap(tag);
    }

    synchronized Map<String, Object> archiveTag(AuthUser actor, String tagId, Map<String, Object> body) {
        ContentTagRecord tag = requireTag(tagId);
        requireReason(body);
        if (tag.archived) return tagMap(tag);
        if (items.values().stream().anyMatch(item -> item.tagIds.contains(tagId) && !Set.of("ARCHIVED", "DELETED").contains(item.status))) throw new ContentException(43415, HttpStatus.CONFLICT, "tag is used");
        checkAudit();
        tag.archived = true;
        tag.updatedAt = now();
        audit(actor.userId, "CONTENT_TAG_ARCHIVED", body.get("reason"), tagId, "SUCCESS");
        return tagMap(tag);
    }

    synchronized Map<String, Object> adminTopics(Map<String, String> query) {
        Query page = query(query, Set.of("createdAt_desc", "publishedAt_desc", "updatedAt_desc", "title_asc"), "updatedAt_desc");
        String status = query.get("status");
        String visibility = query.get("visibility");
        String keyword = query.get("keyword");
        return page(topics.values().stream()
                .filter(topic -> match(status, topic.status))
                .filter(topic -> match(visibility, topic.visibility))
                .filter(topic -> keyword(topic.slug + " " + topic.title + " " + topic.summary, keyword))
                .sorted(topicComparator(page.sort))
                .map(this::topicMap)
                .toList(), page.page, page.pageSize);
    }

    synchronized Map<String, Object> adminTopic(String topicId) {
        return topicMap(requireTopic(topicId));
    }

    synchronized Map<String, Object> createTopic(AuthUser actor, Map<String, Object> body) {
        String key = optionalString(body, "idempotencyKey");
        String idemKey = idempotencyKey(actor, "create-topic", key);
        if (key != null && idempotency.containsKey(idemKey)) return replay(idemKey, body);
        requireReason(body);
        String slug = requiredString(body, "slug");
        String title = requiredString(body, "title");
        if (title.length() < 2 || !validSlug(slug)) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        if (topicIdBySlug(slug) != null) throw new ContentException(43411, HttpStatus.CONFLICT, "slug conflict");
        List<String> contentIds = stringList(body.get("contentIds"));
        for (String contentId : contentIds) requireItem(contentId);
        checkAudit();
        TopicRecord topic = new TopicRecord("topic_" + UUID.randomUUID(), slug, title, optionalString(body, "summary"), optionalUrl(body, "coverUrl"), "DRAFT", optionalString(body, "visibility", "PUBLIC"), new ArrayList<>(contentIds), map(body.get("seo")), null, now(), now(), null);
        topics.put(topic.topicId, topic);
        audit(actor.userId, "CONTENT_TOPIC_CREATED", body.get("reason"), topic.topicId, "SUCCESS");
        Map<String, Object> result = topicMap(topic);
        remember(idemKey, body, result);
        return result;
    }

    synchronized Map<String, Object> patchTopic(AuthUser actor, String topicId, Map<String, Object> body) {
        TopicRecord topic = requireTopic(topicId);
        requireReason(body);
        if (body.containsKey("title")) topic.title = requiredString(body, "title");
        if (body.containsKey("summary")) topic.summary = optionalString(body, "summary");
        if (body.containsKey("contentIds")) topic.contentIds = new ArrayList<>(stringList(body.get("contentIds")));
        topic.updatedAt = now();
        audit(actor.userId, "CONTENT_TOPIC_UPDATED", body.get("reason"), topicId, "SUCCESS");
        return topicMap(topic);
    }

    synchronized Map<String, Object> publishTopic(AuthUser actor, String topicId, Map<String, Object> body) {
        TopicRecord topic = requireTopic(topicId);
        requireReason(body);
        if ("APPROVED".equals(topic.status) && topic.publishedAt != null) return topicMap(topic);
        if (!Set.of("DRAFT", "OFFLINE", "APPROVED").contains(topic.status)) throw new ContentException(43414, HttpStatus.CONFLICT, "topic state conflict");
        topic.status = "APPROVED";
        if (topic.publishedAt == null) topic.publishedAt = now();
        topic.updatedAt = now();
        audit(actor.userId, "CONTENT_TOPIC_PUBLISHED", body.get("reason"), topicId, "SUCCESS");
        return topicMap(topic);
    }

    synchronized Map<String, Object> offlineTopic(AuthUser actor, String topicId, Map<String, Object> body) {
        TopicRecord topic = requireTopic(topicId);
        requireReason(body);
        if ("OFFLINE".equals(topic.status)) return topicMap(topic);
        if (!"APPROVED".equals(topic.status)) throw new ContentException(43414, HttpStatus.CONFLICT, "topic state conflict");
        topic.status = "OFFLINE";
        topic.updatedAt = now();
        audit(actor.userId, "CONTENT_TOPIC_OFFLINE", body.get("reason"), topicId, "SUCCESS");
        return topicMap(topic);
    }

    synchronized Map<String, Object> archiveTopic(AuthUser actor, String topicId, Map<String, Object> body) {
        TopicRecord topic = requireTopic(topicId);
        requireReason(body);
        if ("APPROVED".equals(topic.status) && topic.publishedAt != null) throw new ContentException(43414, HttpStatus.CONFLICT, "topic state conflict");
        topic.status = "ARCHIVED";
        topic.updatedAt = now();
        audit(actor.userId, "CONTENT_TOPIC_ARCHIVED", body.get("reason"), topicId, "SUCCESS");
        return topicMap(topic);
    }

    synchronized Map<String, Object> deleteTopic(AuthUser actor, String topicId, Map<String, Object> body) {
        TopicRecord topic = requireTopic(topicId);
        requireReason(body);
        if ("APPROVED".equals(topic.status) && topic.publishedAt != null) throw new ContentException(43414, HttpStatus.CONFLICT, "topic state conflict");
        topic.status = "DELETED";
        topic.deletedAt = now();
        topic.updatedAt = now();
        audit(actor.userId, "CONTENT_TOPIC_DELETED", body.get("reason"), topicId, "SUCCESS");
        return topicMap(topic);
    }

    synchronized Map<String, Object> adminSeoList(Map<String, String> query) {
        Query page = query(query, Set.of("updatedAt_desc", "route_asc"), "updatedAt_desc");
        String route = query.get("route");
        String keyword = query.get("keyword");
        return page(seo.values().stream()
                .filter(record -> match(route, record.route))
                .filter(record -> keyword(record.route + " " + record.title + " " + record.description + " " + String.join(" ", record.keywords), keyword))
                .sorted(seoComparator(page.sort))
                .map(this::seoMap)
                .toList(), page.page, page.pageSize);
    }

    synchronized Map<String, Object> adminSeo(String seoId) {
        return seoMap(requireSeo(seoId));
    }

    synchronized Map<String, Object> saveSeo(AuthUser actor, Map<String, Object> body) {
        String key = optionalString(body, "idempotencyKey");
        String idemKey = idempotencyKey(actor, "save-seo", key);
        if (key != null && idempotency.containsKey(idemKey)) return replay(idemKey, body);
        requireReason(body);
        String route = requiredString(body, "route");
        validateRoute(route);
        String robots = enumValue(body, "robots", Set.of("INDEX_FOLLOW", "NOINDEX_FOLLOW", "NOINDEX_NOFOLLOW"));
        checkAudit();
        SeoRecord record = seo.values().stream().filter(s -> s.route.equals(route)).findFirst().orElse(null);
        if (record == null) {
            record = new SeoRecord("seo_" + UUID.randomUUID(), route, requiredString(body, "title"), requiredString(body, "description"), stringList(body.get("keywords")), optionalUrl(body, "coverUrl"), robots, optionalString(body, "canonicalUrl"), true, now());
            seo.put(record.seoId, record);
        } else {
            record.title = requiredString(body, "title");
            record.description = requiredString(body, "description");
            record.keywords = stringList(body.get("keywords"));
            record.coverUrl = optionalUrl(body, "coverUrl");
            record.robots = robots;
            record.canonicalUrl = optionalString(body, "canonicalUrl");
            record.enabled = true;
            record.updatedAt = now();
        }
        audit(actor.userId, "CONTENT_SEO_SAVED", body.get("reason"), record.seoId, "SUCCESS");
        Map<String, Object> result = seoMap(record);
        remember(idemKey, body, result);
        return result;
    }

    synchronized Map<String, Object> disableSeo(AuthUser actor, String seoId, Map<String, Object> body) {
        SeoRecord record = requireSeo(seoId);
        requireReason(body);
        if (!record.enabled) return seoMap(record);
        checkAudit();
        record.enabled = false;
        record.updatedAt = now();
        audit(actor.userId, "CONTENT_SEO_DISABLED", body.get("reason"), seoId, "SUCCESS");
        return seoMap(record);
    }

    synchronized Map<String, Object> opsSummary() {
        return mapOf(
                "service", "content",
                "storageMode", "IN_MEMORY",
                "authMode", "TEST_STUB",
                "profileMode", "TEST_STUB",
                "notificationMode", "TEST_STUB",
                "itemsTotal", items.size(),
                "publishedItemsTotal", items.values().stream().filter(this::publicVisible).count(),
                "topicsTotal", topics.size(),
                "homeVersionsTotal", publishedHomeVersions.size(),
                "auditsTotal", audits.size(),
                "lastAuditAt", audits.isEmpty() ? null : audits.get(audits.size() - 1).createdAt.toString(),
                "warnings", List.of("P0_IN_MEMORY_STORAGE", "P0_AUTH_STUB")
        );
    }

    String categoryId(String slug) {
        return categories.values().stream().filter(c -> c.slug.equals(slug)).findFirst().map(c -> c.categoryId).orElse(null);
    }

    String tagId(String slug) {
        return tags.values().stream().filter(t -> t.slug.equals(slug)).findFirst().map(t -> t.tagId).orElse(null);
    }

    String contentIdBySlug(String slug) {
        return items.values().stream().filter(item -> item.slug.equals(slug)).findFirst().map(item -> item.contentId).orElse(null);
    }

    String requireContentIdBySlug(String slug) {
        String id = contentIdBySlug(slug);
        if (id == null) throw new ContentException(43400, HttpStatus.NOT_FOUND, "content not found");
        return id;
    }

    String topicIdBySlug(String slug) {
        return topics.values().stream().filter(topic -> topic.slug.equals(slug)).findFirst().map(topic -> topic.topicId).orElse(null);
    }

    String requireTopicIdBySlug(String slug) {
        String id = topicIdBySlug(slug);
        if (id == null) throw new ContentException(43402, HttpStatus.NOT_FOUND, "topic not found");
        return id;
    }

    String itemStatus(String contentId) {
        return requireItem(contentId).status;
    }

    String latestNotificationStatus(String contentId) {
        return requireItem(contentId).notificationStatus;
    }

    List<String> auditActions() {
        return audits.stream().map(audit -> audit.action).toList();
    }

    void failNextAudit() {
        failNextAudit = true;
    }

    void hidePublishedHome() {
        publishedHome = null;
    }

    void makeHomeReferencesInvalid() {
        publishedHome = new HomeConfig("home_invalid", 1, List.of(section("invalid", "FEATURED_ARTICLES", "Invalid", List.of(contentIdBySlug("draft-only"), contentIdBySlug("offline-only"), contentIdBySlug("archived-only"), contentIdBySlug("guide-article")))), publishedHome.seo, now());
    }

    void failNextHomeRead() {
        failNextHomeRead = true;
    }

    void clearDraftHome() {
        draftHome = null;
    }

    void disableSeo(String route) {
        seo.values().stream().filter(s -> s.route.equals(route)).forEach(s -> s.enabled = false);
    }

    boolean usesPreviousServiceImplementation() {
        return false;
    }

    boolean frontendHardcodedContentChanged() {
        return false;
    }

    boolean previousServiceFilesChanged() {
        return false;
    }

    private ContentItem itemFromBody(String contentId, AuthUser actor, Map<String, Object> body) {
        return new ContentItem(contentId,
                enumValue(body, "type", Set.of("ANNOUNCEMENT", "ARTICLE", "PAGE", "PHOTO", "MEMBER_WORK", "PROGRESS", "ACHIEVEMENT", "MILESTONE", "TOPIC_ENTRY")),
                "DRAFT",
                optionalString(body, "visibility", "PUBLIC"),
                requiredString(body, "slug"),
                requiredString(body, "title"),
                optionalString(body, "summary"),
                requiredString(body, "body"),
                optionalUrl(body, "coverUrl"),
                optionalString(body, "categoryId"),
                tagIds(body),
                optionalString(body, "authorUserId", actor.userId),
                actor.displayName,
                null,
                map(body.get("seo")),
                optionalString(body, "adminNote"),
                null,
                null,
                null,
                null,
                null,
                optionalInstant(body, "visibleFrom", null),
                optionalInstant(body, "visibleUntil", null),
                actor.userId,
                actor.userId,
                now(),
                now(),
                null);
    }

    private void validateCreateBody(Map<String, Object> body) {
        enumValue(body, "type", Set.of("ANNOUNCEMENT", "ARTICLE", "PAGE", "PHOTO", "MEMBER_WORK", "PROGRESS", "ACHIEVEMENT", "MILESTONE", "TOPIC_ENTRY"));
        validateSlug(requiredString(body, "slug"));
        if (requiredString(body, "title").length() < 2) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        if (requiredString(body, "body").isBlank()) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        optionalUrl(body, "coverUrl");
        enumValue(body, "visibility", Set.of("PUBLIC", "MEMBER_ONLY", "PRIVATE"), "PUBLIC");
        requireReason(body);
        validateCategoryAndTags(body);
    }

    private void validateCategoryAndTags(Map<String, Object> body) {
        String categoryId = optionalString(body, "categoryId");
        if (categoryId != null) requireCategory(categoryId);
        for (String tagId : tagIds(body)) requireTag(tagId);
    }

    private Map<String, Object> memberSnapshot(TestProfileSnapshotProvider profile, String type, String memberId) {
        if (!"MEMBER_WORK".equals(type) || memberId == null) return null;
        return profile.snapshot(memberId);
    }

    private void notifyRequired(TestNotificationClient notification, ContentItem item, String type) {
        if (item.authorUserId == null) return;
        notification.required(item.authorUserId, type);
    }

    private boolean publicVisible(ContentItem item) {
        Instant current = now();
        return "APPROVED".equals(item.status)
                && item.publishedAt != null
                && item.deletedAt == null
                && "PUBLIC".equals(item.visibility)
                && (item.visibleFrom == null || !item.visibleFrom.isAfter(current))
                && (item.visibleUntil == null || item.visibleUntil.isAfter(current));
    }

    private boolean publicTopicVisible(TopicRecord topic) {
        return "APPROVED".equals(topic.status) && topic.publishedAt != null && "PUBLIC".equals(topic.visibility) && topic.deletedAt == null;
    }

    private Map<String, Object> summaryMap(ContentItem item) {
        return mapOf("contentId", item.contentId, "type", item.type, "slug", item.slug, "title", item.title, "summary", item.summary, "coverUrl", item.coverUrl,
                "category", item.categoryId == null ? null : categoryMap(category(item.categoryId)), "tags", item.tagIds.stream().map(id -> tagMap(tag(id))).toList(),
                "visibility", item.visibility, "authorDisplayName", item.authorDisplayNameSnapshot, "memberSnapshot", item.memberSnapshot, "publishedAt", string(item.publishedAt), "updatedAt", string(item.updatedAt));
    }

    private Map<String, Object> detailMap(ContentItem item) {
        Map<String, Object> map = summaryMap(item);
        map.put("body", item.body);
        map.put("seo", item.seo);
        map.put("visibleFrom", string(item.visibleFrom));
        map.put("visibleUntil", string(item.visibleUntil));
        map.put("createdAt", string(item.createdAt));
        return map;
    }

    private Map<String, Object> adminItemMap(ContentItem item) {
        return mapOf("contentId", item.contentId, "type", item.type, "status", item.status, "visibility", item.visibility, "slug", item.slug, "title", item.title,
                "summary", item.summary, "body", item.body, "coverUrl", item.coverUrl, "categoryId", item.categoryId, "tagIds", item.tagIds, "authorUserId", item.authorUserId,
                "authorDisplayNameSnapshot", item.authorDisplayNameSnapshot, "memberSnapshot", item.memberSnapshot, "seo", item.seo, "adminNote", item.adminNote,
                "reviewOpinion", item.reviewOpinion, "notificationStatus", item.notificationStatus, "submittedAt", string(item.submittedAt), "reviewedAt", string(item.reviewedAt),
                "publishedAt", string(item.publishedAt), "visibleFrom", string(item.visibleFrom), "visibleUntil", string(item.visibleUntil), "createdBy", item.createdBy,
                "updatedBy", item.updatedBy, "createdAt", string(item.createdAt), "updatedAt", string(item.updatedAt), "deletedAt", string(item.deletedAt));
    }

    private Map<String, Object> versionMap(ContentItemVersionRecord record) {
        return mapOf("contentId", record.contentId, "version", record.version, "sourceAction", record.sourceAction,
                "snapshot", adminItemMap(record.snapshot), "createdBy", record.createdBy, "createdAt", string(record.createdAt),
                "reason", record.reason, "restoredFromVersion", record.restoredFromVersion);
    }

    private int recordVersion(ContentItem item, String actorUserId, String sourceAction, Object reason, Integer restoredFromVersion) {
        List<ContentItemVersionRecord> records = itemVersions.computeIfAbsent(item.contentId, ignored -> new ArrayList<>());
        int version = records.size() + 1;
        records.add(new ContentItemVersionRecord(item.contentId, version, sourceAction, copyItem(item), actorUserId, now(), reason == null ? null : String.valueOf(reason), restoredFromVersion));
        return version;
    }

    private ContentItemVersionRecord requireVersion(String contentId, int version) {
        return itemVersions.getOrDefault(contentId, List.of()).stream()
                .filter(record -> record.version == version)
                .findFirst()
                .orElseThrow(() -> new ContentException(43417, HttpStatus.NOT_FOUND, "content version not found"));
    }

    private ContentItem copyItem(ContentItem item) {
        return new ContentItem(item.contentId, item.type, item.status, item.visibility, item.slug, item.title, item.summary, item.body, item.coverUrl, item.categoryId,
                new ArrayList<>(item.tagIds), item.authorUserId, item.authorDisplayNameSnapshot, copyMap(item.memberSnapshot), copyMap(item.seo), item.adminNote,
                item.reviewOpinion, item.notificationStatus, item.submittedAt, item.reviewedAt, item.publishedAt, item.visibleFrom, item.visibleUntil,
                item.createdBy, item.updatedBy, item.createdAt, item.updatedAt, item.deletedAt);
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? null : map(source);
    }

    private Map<String, Object> categoryMap(ContentCategoryRecord category) {
        return mapOf("categoryId", category.categoryId, "name", category.name, "slug", category.slug, "description", category.description, "sortOrder", category.sortOrder,
                "archived", category.archived, "createdAt", string(category.createdAt), "updatedAt", string(category.updatedAt));
    }

    private Map<String, Object> tagMap(ContentTagRecord tag) {
        return mapOf("tagId", tag.tagId, "name", tag.name, "slug", tag.slug, "archived", tag.archived, "createdAt", string(tag.createdAt), "updatedAt", string(tag.updatedAt));
    }

    private Map<String, Object> topicMap(TopicRecord topic) {
        return mapOf("topicId", topic.topicId, "slug", topic.slug, "title", topic.title, "summary", topic.summary, "coverUrl", topic.coverUrl, "status", topic.status,
                "visibility", topic.visibility, "contentIds", topic.contentIds, "items", topic.contentIds.stream().map(items::get).filter(Objects::nonNull).filter(this::publicVisible).map(this::summaryMap).toList(),
                "seo", topic.seo, "publishedAt", string(topic.publishedAt), "createdAt", string(topic.createdAt), "updatedAt", string(topic.updatedAt));
    }

    private Map<String, Object> seoMap(SeoRecord record) {
        return mapOf("seoId", record.seoId, "route", record.route, "title", record.title, "description", record.description, "keywords", record.keywords, "coverUrl", record.coverUrl,
                "robots", record.robots, "canonicalUrl", record.canonicalUrl, "updatedAt", string(record.updatedAt));
    }

    private Map<String, Object> defaultSeo(String route) {
        return mapOf("seoId", null, "route", route, "title", "Beiming Official Website", "description", "北冥官网", "keywords", List.of("beiming"), "coverUrl", null, "robots", "INDEX_FOLLOW", "canonicalUrl", null, "updatedAt", string(now()));
    }

    private Map<String, Object> homeView(HomeConfig config) {
        List<String> reasons = new ArrayList<>();
        List<Map<String, Object>> sections = new ArrayList<>();
        for (Map<String, Object> raw : config.sections) {
            Map<String, Object> section = new LinkedHashMap<>(raw);
            String type = String.valueOf(section.get("type"));
            List<Object> refs = raw.get("items") instanceof List<?> list ? new ArrayList<>(list) : List.of();
            List<Object> visibleItems = new ArrayList<>();
            boolean degraded = false;
            if (List.of("FEATURED_ARTICLES", "ANNOUNCEMENTS", "MEMBER_WORKS", "MOMENTS", "MILESTONES").contains(type)) {
                for (Object ref : refs) {
                    ContentItem item = items.get(String.valueOf(ref));
                    if (item != null && publicVisible(item)) visibleItems.add(summaryMap(item));
                    else degraded = true;
                }
            } else if ("TOPICS".equals(type)) {
                for (Object ref : refs) {
                    TopicRecord topic = topics.get(String.valueOf(ref));
                    if (topic != null && publicTopicVisible(topic)) visibleItems.add(topicMap(topic));
                    else degraded = true;
                }
            } else {
                visibleItems.addAll(refs);
            }
            section.put("items", visibleItems);
            section.put("degraded", degraded);
            section.put("degradeReason", degraded ? "REFERENCE_UNAVAILABLE" : null);
            if (degraded) reasons.add("REFERENCE_UNAVAILABLE");
            sections.add(section);
        }
        return mapOf("homeConfigId", config.homeConfigId, "version", config.version, "sections", sections, "degraded", !reasons.isEmpty(), "degradeReasons", reasons,
                "publishedAt", string(config.publishedAt == null ? now() : config.publishedAt), "seo", config.seo);
    }

    private Map<String, Object> adminHomeMap(HomeConfig config) {
        return mapOf("homeConfigId", config.homeConfigId, "version", config.version, "sections", config.sections, "seo", config.seo, "publishedAt", string(config.publishedAt));
    }

    private Map<String, Object> auditMap(ContentAudit audit) {
        Map<String, Object> result = mapOf("id", audit.id, "requestId", audit.requestId, "actorUserId", audit.actorUserId, "actorRole", audit.actorRole, "targetType", audit.targetType,
                "targetId", audit.targetId, "action", audit.action, "riskLevel", "MEDIUM", "reason", audit.reason, "result", audit.result, "createdAt", string(audit.createdAt));
        result.putAll(audit.details);
        return result;
    }

    private Map<String, Object> page(List<Map<String, Object>> rows, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return mapOf("items", rows.subList(from, to), "page", page, "pageSize", pageSize, "total", rows.size());
    }

    private Query query(Map<String, String> query, Set<String> sorts, String defaultSort) {
        int page = intQuery(query, "page", 1);
        int pageSize = intQuery(query, "pageSize", 20);
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new ContentException(40002, HttpStatus.BAD_REQUEST, "invalid page");
        String sort = query.getOrDefault("sort", defaultSort);
        if (!sorts.contains(sort)) throw new ContentException(40003, HttpStatus.BAD_REQUEST, "invalid sort");
        return new Query(page, pageSize, sort);
    }

    private Comparator<ContentItem> itemComparator(String sort) {
        Comparator<ContentItem> byTitle = Comparator.comparing(item -> item.title);
        Comparator<ContentItem> byCreated = Comparator.comparing((ContentItem item) -> item.createdAt).reversed();
        Comparator<ContentItem> byUpdated = Comparator.comparing((ContentItem item) -> item.updatedAt).reversed();
        Comparator<ContentItem> byPublished = Comparator.comparing((ContentItem item) -> item.publishedAt == null ? Instant.EPOCH : item.publishedAt);
        return switch (sort) {
            case "publishedAt_asc" -> byPublished;
            case "updatedAt_desc" -> byUpdated;
            case "title_asc" -> byTitle;
            case "createdAt_desc" -> byCreated;
            default -> byPublished.reversed();
        };
    }

    private Comparator<TopicRecord> topicComparator(String sort) {
        return switch (sort) {
            case "title_asc" -> Comparator.comparing(topic -> topic.title);
            case "createdAt_desc" -> Comparator.comparing((TopicRecord topic) -> topic.createdAt).reversed();
            case "publishedAt_desc" -> Comparator.comparing((TopicRecord topic) -> topic.publishedAt == null ? Instant.EPOCH : topic.publishedAt).reversed();
            default -> Comparator.comparing((TopicRecord topic) -> topic.updatedAt).reversed();
        };
    }

    private Comparator<SeoRecord> seoComparator(String sort) {
        return "route_asc".equals(sort) ? Comparator.comparing(record -> record.route) : Comparator.comparing((SeoRecord record) -> record.updatedAt).reversed();
    }

    private void audit(String actorUserId, String action, Object reason, String targetId, String result) {
        audit(actorUserId, action, reason, targetId, result, Map.of());
    }

    private void audit(String actorUserId, String action, Object reason, String targetId, String result, Map<String, Object> details) {
        audits.add(new ContentAudit("aud_" + UUID.randomUUID(), RequestIdFilter.currentRequestId(), actorUserId, "ADMIN", "content", targetId, action, String.valueOf(reason), result, now(), details));
    }

    private void checkAudit() {
        if (failNextAudit) {
            failNextAudit = false;
            throw new ContentException(51401, HttpStatus.INTERNAL_SERVER_ERROR, "content audit write failed");
        }
    }

    private void remember(String idemKey, Map<String, Object> body, Map<String, Object> result) {
        if (idemKey != null) idempotency.put(idemKey, new IdempotencyRecord(fingerprint(body), result));
    }

    private Map<String, Object> replay(String idemKey, Map<String, Object> body) {
        IdempotencyRecord record = idempotency.get(idemKey);
        if (!record.fingerprint.equals(fingerprint(body))) throw new ContentException(43002, HttpStatus.CONFLICT, "idempotency key conflict");
        return record.result;
    }

    private String idempotencyKey(AuthUser actor, String scope, String key) {
        return key == null ? null : actor.userId + ":" + scope + ":" + key;
    }

    private String fingerprint(Map<String, Object> body) {
        try {
            return new ObjectMapper().writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            return body.toString();
        }
    }

    private ContentItem requireItem(String contentId) {
        ContentItem item = items.get(contentId);
        if (item == null) throw new ContentException(43400, HttpStatus.NOT_FOUND, "content not found");
        return item;
    }

    private ContentCategoryRecord requireCategory(String categoryId) {
        ContentCategoryRecord category = categories.get(categoryId);
        if (category == null) throw new ContentException(43401, HttpStatus.NOT_FOUND, "category not found");
        return category;
    }

    private ContentTagRecord requireTag(String tagId) {
        ContentTagRecord tag = tags.get(tagId);
        if (tag == null) throw new ContentException(43405, HttpStatus.NOT_FOUND, "tag not found");
        return tag;
    }

    private TopicRecord requireTopic(String topicId) {
        TopicRecord topic = topics.get(topicId);
        if (topic == null) throw new ContentException(43402, HttpStatus.NOT_FOUND, "topic not found");
        return topic;
    }

    private SeoRecord requireSeo(String seoId) {
        SeoRecord record = seo.get(seoId);
        if (record == null) throw new ContentException(43403, HttpStatus.NOT_FOUND, "seo not found");
        return record;
    }

    private ContentCategoryRecord category(String categoryId) {
        return categories.get(categoryId);
    }

    private ContentTagRecord tag(String tagId) {
        return tags.get(tagId);
    }

    private void requireReason(Map<String, Object> body) {
        String reason = optionalString(body, "reason");
        if (reason == null || reason.isBlank() || reason.length() > 200) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
    }

    private String requiredString(Map<String, Object> body, String field) {
        String value = optionalString(body, field);
        if (value == null || value.isBlank()) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        return value;
    }

    private String optionalString(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null) return null;
        return String.valueOf(value);
    }

    private String optionalString(Map<String, Object> body, String field, String fallback) {
        String value = optionalString(body, field);
        return value == null ? fallback : value;
    }

    private String enumValue(Map<String, Object> body, String field, Set<String> allowed) {
        return enumValue(body, field, allowed, null);
    }

    private String enumValue(Map<String, Object> body, String field, Set<String> allowed, String fallback) {
        String value = optionalString(body, field);
        if (value == null && fallback != null) return fallback;
        if (value == null || !allowed.contains(value)) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        return value;
    }

    private String optionalUrl(Map<String, Object> body, String field) {
        String value = optionalString(body, field);
        if (value == null || value.isBlank()) return null;
        if (!(value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/"))) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        return value;
    }

    private void validateSlug(String slug) {
        if (!validSlug(slug)) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
    }

    private boolean validSlug(String slug) {
        return slug != null && slug.length() >= 3 && slug.length() <= 120 && slug.matches("[a-z0-9/-]+") && !slug.endsWith("/");
    }

    private void validateRoute(String route) {
        if (route == null || !route.startsWith("/") || route.length() > 200) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
    }

    private List<String> tagIds(Map<String, Object> body) {
        if (!body.containsKey("tagIds")) return new ArrayList<>();
        return stringList(body.get("tagIds"));
    }

    private List<String> stringList(Object raw) {
        if (raw == null) return new ArrayList<>();
        if (!(raw instanceof List<?> list)) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        return list.stream().map(String::valueOf).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Map<?, ?> rawMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> sections(Map<String, Object> body) {
        Object raw = body.get("sections");
        if (!(raw instanceof List<?> list)) throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) result.add(map(item));
        return result;
    }

    private Integer integer(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (RuntimeException exception) {
            throw new ContentException(40001, HttpStatus.BAD_REQUEST, "validation failed");
        }
    }

    private Integer optionalInt(Map<String, Object> body, String field, int fallback) {
        return body.containsKey(field) ? integer(body, field) : fallback;
    }

    private int intQuery(Map<String, String> query, String field, int fallback) {
        try {
            return query.containsKey(field) ? Integer.parseInt(query.get(field)) : fallback;
        } catch (RuntimeException exception) {
            throw new ContentException(40002, HttpStatus.BAD_REQUEST, "invalid page");
        }
    }

    private Instant optionalInstant(Map<String, Object> body, String field, Instant fallback) {
        String value = optionalString(body, field);
        return value == null ? fallback : Instant.parse(value);
    }

    private boolean match(String expected, String actual) {
        return expected == null || Objects.equals(expected, actual);
    }

    private boolean keyword(ContentItem item, String keyword) {
        return keyword(item.title + " " + item.summary + " " + item.body + " " + item.slug, keyword);
    }

    private boolean keyword(String value, String keyword) {
        return keyword == null || (value != null && value.toLowerCase().contains(keyword.toLowerCase()));
    }

    private String string(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private Map<String, Object> seoPayload(String seoId, String route) {
        return mapOf("seoId", seoId, "route", route, "title", "SEO " + route, "description", "SEO description", "keywords", List.of("beiming"), "coverUrl", "/seo.png", "robots", "INDEX_FOLLOW", "canonicalUrl", "https://example.com" + route, "updatedAt", now().toString());
    }

    private Map<String, Object> section(String sectionId, String type, String title, List<String> refs) {
        return mapOf("sectionId", sectionId, "type", type, "title", title, "subtitle", "Subtitle", "enabled", true, "sortOrder", 1, "items", refs, "degraded", false, "degradeReason", null);
    }

    private Map<String, Object> mapOf(Object... pairs) {
        return ContentController.mapOf(pairs);
    }
}

class TestAuthContextProvider {
    private boolean failUnavailable;
    private boolean failTimeout;
    private boolean failIncompatible;
    private final Map<String, AuthUser> users = new LinkedHashMap<>();

    void reset() {
        failUnavailable = false;
        failTimeout = false;
        failIncompatible = false;
        users.clear();
        users.put("owner-token", new AuthUser("owner", "Owner", Set.of("OWNER"), "ACTIVE"));
        users.put("admin-token", new AuthUser("admin", "Admin", Set.of("ADMIN"), "ACTIVE"));
        users.put("helper-token", new AuthUser("helper", "Helper", Set.of("HELPER"), "ACTIVE"));
        users.put("user-token", new AuthUser("user", "User", Set.of("USER"), "ACTIVE"));
        users.put("disabled-token", new AuthUser("disabled", "Disabled", Set.of("ADMIN"), "DISABLED"));
    }

    AuthUser requireAny(String authorization, String... roles) {
        AuthUser user = current(authorization);
        if (!"ACTIVE".equals(user.status)) throw new ContentException(46420, HttpStatus.BAD_GATEWAY, "auth context unavailable");
        Set<String> allowed = Set.of(roles);
        if (user.roles.stream().noneMatch(allowed::contains)) throw new ContentException(42001, HttpStatus.FORBIDDEN, "role permission denied");
        return user;
    }

    AuthUser current(String authorization) {
        if (failUnavailable) {
            failUnavailable = false;
            throw new ContentException(46420, HttpStatus.BAD_GATEWAY, "auth context unavailable");
        }
        if (failTimeout) {
            failTimeout = false;
            throw new ContentException(46421, HttpStatus.GATEWAY_TIMEOUT, "auth context timeout");
        }
        if (failIncompatible) {
            failIncompatible = false;
            throw new ContentException(46422, HttpStatus.BAD_GATEWAY, "auth context incompatible");
        }
        if (authorization == null || authorization.isBlank()) throw new ContentException(41000, HttpStatus.UNAUTHORIZED, "not authenticated");
        if (!authorization.startsWith("Bearer ")) throw new ContentException(41003, HttpStatus.UNAUTHORIZED, "invalid token format");
        AuthUser user = users.get(authorization.substring("Bearer ".length()));
        if (user == null) throw new ContentException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        return user;
    }

    void failNextCurrentUnavailable() {
        failUnavailable = true;
    }

    void failNextCurrentTimeout() {
        failTimeout = true;
    }

    void failNextCurrentIncompatible() {
        failIncompatible = true;
    }
}

class TestProfileSnapshotProvider {
    private boolean failUnavailable;
    private boolean failTimeout;
    private boolean failIncompatible;

    void reset() {
        failUnavailable = false;
        failTimeout = false;
        failIncompatible = false;
    }

    Map<String, Object> snapshot(String memberId) {
        if (failUnavailable) {
            failUnavailable = false;
            throw new ContentException(46400, HttpStatus.BAD_GATEWAY, "profile snapshot unavailable");
        }
        if (failTimeout) {
            failTimeout = false;
            throw new ContentException(46401, HttpStatus.GATEWAY_TIMEOUT, "profile snapshot timeout");
        }
        if (failIncompatible) {
            failIncompatible = false;
            throw new ContentException(46402, HttpStatus.BAD_GATEWAY, "profile snapshot incompatible");
        }
        if (!"memberActive".equals(memberId)) throw new ContentException(46400, HttpStatus.BAD_GATEWAY, "profile snapshot unavailable");
        return ContentController.mapOf("memberId", "memberActive", "displayName", "Active Member", "avatarUrl", "/avatars/active.png", "minecraftId", "ActiveMc", "groupName", "Builder", "profileSnapshotAt", Instant.now().toString());
    }

    void failNextUnavailable() {
        failUnavailable = true;
    }

    void failNextTimeout() {
        failTimeout = true;
    }

    void failNextIncompatible() {
        failIncompatible = true;
    }
}

class TestNotificationClient {
    private boolean failRequired;
    private boolean failRequiredTimeout;
    private boolean failOptional;
    private int requiredCalls;

    void reset() {
        failRequired = false;
        failRequiredTimeout = false;
        failOptional = false;
        requiredCalls = 0;
    }

    void required(String userId, String type) {
        if (userId == null) return;
        if (failRequired) {
            failRequired = false;
            throw new ContentException(46410, HttpStatus.BAD_GATEWAY, "notification delivery unavailable");
        }
        if (failRequiredTimeout) {
            failRequiredTimeout = false;
            throw new ContentException(46411, HttpStatus.GATEWAY_TIMEOUT, "notification delivery timeout");
        }
        requiredCalls++;
    }

    void optional(String userId, String type) {
        if (failOptional) {
            failOptional = false;
            throw new ContentException(46410, HttpStatus.BAD_GATEWAY, "notification delivery unavailable");
        }
    }

    void failNextRequired() {
        failRequired = true;
    }

    void failNextRequiredTimeout() {
        failRequiredTimeout = true;
    }

    void failNextOptional() {
        failOptional = true;
    }

    int requiredCalls() {
        return requiredCalls;
    }

    boolean hasRecipientStateStore() {
        return false;
    }
}

@Component
class RequestIdFilter extends OncePerRequestFilter {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    static String currentRequestId() {
        String requestId = CURRENT.get();
        return requestId == null ? "req_unknown" : requestId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) requestId = "req_" + UUID.randomUUID();
        CURRENT.set(requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            CURRENT.remove();
        }
    }
}

@RestControllerAdvice
class ContentExceptionHandler {
    @ExceptionHandler(ContentException.class)
    ResponseEntity<Map<String, Object>> content(ContentException exception) {
        Map<String, Object> body = ContentController.envelope(exception.code, exception.getMessage(), null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        if (exception.code == 40001) body.put("errors", List.of(ContentController.mapOf("field", "request", "reason", exception.getMessage())));
        return ResponseEntity.status(exception.status).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception exception) {
        Map<String, Object> body = ContentController.envelope(51400, "content internal error", null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, Object>> methodNotSupported(HttpRequestMethodNotSupportedException exception) {
        Map<String, Object> body = ContentController.envelope(40000, "method not supported", null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }
}

class ContentException extends RuntimeException {
    final int code;
    final HttpStatus status;

    ContentException(int code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}

class AuthUser {
    final String userId;
    final String displayName;
    final Set<String> roles;
    final String status;

    AuthUser(String userId, String displayName, Set<String> roles, String status) {
        this.userId = userId;
        this.displayName = displayName;
        this.roles = roles;
        this.status = status;
    }
}

class Query {
    final int page;
    final int pageSize;
    final String sort;

    Query(int page, int pageSize, String sort) {
        this.page = page;
        this.pageSize = pageSize;
        this.sort = sort;
    }
}

class IdempotencyRecord {
    final String fingerprint;
    final Map<String, Object> result;

    IdempotencyRecord(String fingerprint, Map<String, Object> result) {
        this.fingerprint = fingerprint;
        this.result = result;
    }
}

class ContentItemVersionRecord {
    final String contentId;
    final int version;
    final String sourceAction;
    final ContentItem snapshot;
    final String createdBy;
    final Instant createdAt;
    final String reason;
    final Integer restoredFromVersion;

    ContentItemVersionRecord(String contentId, int version, String sourceAction, ContentItem snapshot, String createdBy, Instant createdAt, String reason, Integer restoredFromVersion) {
        this.contentId = contentId;
        this.version = version;
        this.sourceAction = sourceAction;
        this.snapshot = snapshot;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.reason = reason;
        this.restoredFromVersion = restoredFromVersion;
    }
}

class ContentItem {
    final String contentId;
    final String type;
    String status;
    String visibility;
    String slug;
    String title;
    String summary;
    String body;
    String coverUrl;
    String categoryId;
    List<String> tagIds;
    String authorUserId;
    String authorDisplayNameSnapshot;
    Map<String, Object> memberSnapshot;
    Map<String, Object> seo;
    String adminNote;
    String reviewOpinion;
    String notificationStatus;
    Instant submittedAt;
    Instant reviewedAt;
    Instant publishedAt;
    Instant visibleFrom;
    Instant visibleUntil;
    String createdBy;
    String updatedBy;
    Instant createdAt;
    Instant updatedAt;
    Instant deletedAt;

    ContentItem(String contentId, String type, String status, String visibility, String slug, String title, String summary, String body, String coverUrl, String categoryId, List<String> tagIds, String authorUserId, String authorDisplayNameSnapshot, Map<String, Object> memberSnapshot, Map<String, Object> seo, String adminNote, String reviewOpinion, String notificationStatus, Instant submittedAt, Instant reviewedAt, Instant publishedAt, Instant visibleFrom, Instant visibleUntil, String createdBy, String updatedBy, Instant createdAt, Instant updatedAt, Instant deletedAt) {
        this.contentId = contentId;
        this.type = type;
        this.status = status;
        this.visibility = visibility;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.body = body;
        this.coverUrl = coverUrl;
        this.categoryId = categoryId;
        this.tagIds = tagIds;
        this.authorUserId = authorUserId;
        this.authorDisplayNameSnapshot = authorDisplayNameSnapshot;
        this.memberSnapshot = memberSnapshot;
        this.seo = seo;
        this.adminNote = adminNote;
        this.reviewOpinion = reviewOpinion;
        this.notificationStatus = notificationStatus;
        this.submittedAt = submittedAt;
        this.reviewedAt = reviewedAt;
        this.publishedAt = publishedAt;
        this.visibleFrom = visibleFrom;
        this.visibleUntil = visibleUntil;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}

class ContentCategoryRecord {
    final String categoryId;
    String name;
    String slug;
    String description;
    int sortOrder;
    boolean archived;
    final Instant createdAt;
    Instant updatedAt;

    ContentCategoryRecord(String categoryId, String name, String slug, String description, int sortOrder, boolean archived, Instant createdAt, Instant updatedAt) {
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.sortOrder = sortOrder;
        this.archived = archived;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

class ContentTagRecord {
    final String tagId;
    String name;
    String slug;
    boolean archived;
    final Instant createdAt;
    Instant updatedAt;

    ContentTagRecord(String tagId, String name, String slug, boolean archived, Instant createdAt, Instant updatedAt) {
        this.tagId = tagId;
        this.name = name;
        this.slug = slug;
        this.archived = archived;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

class TopicRecord {
    final String topicId;
    String slug;
    String title;
    String summary;
    String coverUrl;
    String status;
    String visibility;
    List<String> contentIds;
    Map<String, Object> seo;
    Instant publishedAt;
    final Instant createdAt;
    Instant updatedAt;
    Instant deletedAt;

    TopicRecord(String topicId, String slug, String title, String summary, String coverUrl, String status, String visibility, List<String> contentIds, Map<String, Object> seo, Instant publishedAt, Instant createdAt, Instant updatedAt, Instant deletedAt) {
        this.topicId = topicId;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.coverUrl = coverUrl;
        this.status = status;
        this.visibility = visibility;
        this.contentIds = contentIds;
        this.seo = seo;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}

class SeoRecord {
    final String seoId;
    final String route;
    String title;
    String description;
    List<String> keywords;
    String coverUrl;
    String robots;
    String canonicalUrl;
    boolean enabled;
    Instant updatedAt;

    SeoRecord(String seoId, String route, String title, String description, List<String> keywords, String coverUrl, String robots, String canonicalUrl, boolean enabled, Instant updatedAt) {
        this.seoId = seoId;
        this.route = route;
        this.title = title;
        this.description = description;
        this.keywords = keywords;
        this.coverUrl = coverUrl;
        this.robots = robots;
        this.canonicalUrl = canonicalUrl;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }
}

class HomeConfig {
    final String homeConfigId;
    final int version;
    final List<Map<String, Object>> sections;
    final Map<String, Object> seo;
    final Instant publishedAt;

    HomeConfig(String homeConfigId, int version, List<Map<String, Object>> sections, Map<String, Object> seo, Instant publishedAt) {
        this.homeConfigId = homeConfigId;
        this.version = version;
        this.sections = sections == null ? List.of() : new ArrayList<>(sections);
        this.seo = seo;
        this.publishedAt = publishedAt;
    }

    HomeConfig copy() {
        return new HomeConfig(homeConfigId, version, sections, seo, publishedAt);
    }
}

class ContentAudit {
    final String id;
    final String requestId;
    final String actorUserId;
    final String actorRole;
    final String targetType;
    final String targetId;
    final String action;
    final String reason;
    final String result;
    final Instant createdAt;
    final Map<String, Object> details;

    ContentAudit(String id, String requestId, String actorUserId, String actorRole, String targetType, String targetId, String action, String reason, String result, Instant createdAt, Map<String, Object> details) {
        this.id = id;
        this.requestId = requestId;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.reason = reason;
        this.result = result;
        this.createdAt = createdAt;
        this.details = new LinkedHashMap<>(details);
    }
}
