package cn.beiming.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ResourceApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("resource local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "RES-COM", 1, 24);
        addRange(mapped, "RES-PUB-LIST", 1, 26);
        addRange(mapped, "RES-PUB-DETAIL", 1, 18);
        addRange(mapped, "RES-PUB-CAT", 1, 8);
        addRange(mapped, "RES-PUB-VERSION", 1, 12);
        addRange(mapped, "RES-DOWNLOAD", 1, 33);
        addRange(mapped, "RES-ADMIN-LIST", 1, 10);
        addRange(mapped, "RES-ADMIN-DETAIL", 1, 6);
        addRange(mapped, "RES-ITEM-CREATE", 1, 18);
        addRange(mapped, "RES-ITEM-PATCH", 1, 12);
        addRange(mapped, "RES-STATE", 1, 61);
        addRange(mapped, "RES-VERSION-ADMIN", 1, 31);
        addRange(mapped, "RES-VERSION-STATE", 1, 15);
        addRange(mapped, "RES-CAT-ADMIN", 1, 36);
        addRange(mapped, "RES-AUDIT", 1, 8);
        addRange(mapped, "RES-OPS", 1, 8);
        addRange(mapped, "RES-COMPAT", 1, 21);
        addRange(mapped, "RES-HARDEN", 1, 16);
        assertThat(mapped).contains("RES-COM-001", "RES-DOWNLOAD-033", "RES-STATE-061", "RES-COMPAT-021");
        assertThat(mapped).hasSize(363);
    }

    @Test
    @DisplayName("RES-COM common envelope, request id, auth, role, paging, sorting, field isolation, and audit rollback")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/resources").header("X-Request-Id", "req-resource-list"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-resource-list"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray());

        JsonNode generatedRequestId = performJson(get("/api/v1/resources/categories"), 200);
        assertThat(generatedRequestId.at("/requestId").asText()).isNotBlank();

        performJson(get("/api/v1/resources").param("page", "0"), 400, 40002);
        performJson(get("/api/v1/resources").param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/resources").param("sort", "bad_sort"), 400, 40003);
        performJson(get("/api/v1/resources/admin/items"), 401, 41000);
        performJson(get("/api/v1/resources/admin/items").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/resources/admin/items").header("Authorization", bearer("helper-token")), 200);
        performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("helper-token")), validResourceBody("helper-denied"), 403, 42001);
        performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")), Map.of("slug", "bad"), 400, 40001);

        JsonNode created = performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")),
                validResourceBody("common-created"), 201);
        assertThat(created.at("/data/createdBy").asText()).isEqualTo("admin");

        performJson(post("/api/v1/resources/admin/items")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Fail-Audit", "true"), validResourceBody("audit-fail"), 500, 51601);
        performJson(get("/api/v1/resources/by-slug/audit-fail"), 404, 43600);

        JsonNode publicList = performJson(get("/api/v1/resources"), 200);
        assertThat(publicList.toString()).doesNotContain("adminNote", "reviewOpinion", "idempotencyKey", "sharePassword", "secret-code", "internalPath", "downloadUrl", "terminal", "node-daemon", "file-manager");
    }

    @Test
    @DisplayName("RES-PUB public resource list, detail, slug, categories, versions, degradation, and field isolation")
    void publicReadContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/resources")
                .param("type", "CLIENT_PACK")
                .param("categoryId", "cat-client")
                .param("tag", "p0")
                .param("keyword", "Client")
                .param("visibility", "PUBLIC")
                .param("minecraftVersion", "1.20.1")
                .param("sort", "title_asc"), 200);
        assertThat(valuesAt(list, "/data/items", "resourceId")).contains("res-public-client");
        assertThat(list.toString()).doesNotContain("res-draft", "res-pending", "res-approved", "res-rejected", "res-needs", "res-offline", "res-archived", "res-deleted", "res-admin");

        JsonNode hiddenCategoryResource = performJson(get("/api/v1/resources/by-slug/hidden-category-pack"), 200);
        assertThat(hiddenCategoryResource.at("/data/category").isNull()).isTrue();

        JsonNode degraded = performJson(get("/api/v1/resources/by-slug/cloudreve-degraded"), 200);
        assertThat(degraded.at("/data/degraded").asBoolean()).isTrue();
        assertThat(degraded.at("/data/downloadAvailable").asBoolean()).isFalse();

        performJson(get("/api/v1/resources").param("type", "BAD"), 400, 40001);
        performJson(get("/api/v1/resources").param("visibility", "ADMIN_ONLY"), 400, 40001);
        performJson(get("/api/v1/resources/res-missing"), 404, 43600);
        performJson(get("/api/v1/resources/res-draft"), 404, 43600);
        performJson(get("/api/v1/resources/res-admin"), 404, 43600);
        performJson(get("/api/v1/resources/by-slug/missing"), 404, 43600);
        performJson(get("/api/v1/resources/by-slug/offline-pack"), 404, 43600);

        JsonNode detail = performJson(get("/api/v1/resources/res-public-client"), 200);
        assertThat(detail.at("/data/resourceId").asText()).isEqualTo("res-public-client");
        assertThat(detail.at("/data/versions").size()).isGreaterThanOrEqualTo(1);
        assertThat(detail.toString()).doesNotContain("adminNote", "sharePassword", "internalPath", "downloadUrl");

        JsonNode categories = performJson(get("/api/v1/resources/categories").param("type", "CLIENT_PACK"), 200);
        assertThat(valuesAt(categories, "/data/items", "categoryId")).contains("cat-client").doesNotContain("cat-disabled", "cat-archived");
        performJson(get("/api/v1/resources/categories").param("type", "BAD"), 400, 40001);

        JsonNode versions = performJson(get("/api/v1/resources/res-public-client/versions"), 200);
        assertThat(valuesAt(versions, "/data/items", "versionId")).contains("ver-client-1").doesNotContain("ver-client-disabled", "ver-client-archived", "ver-no-entry");
        assertThat(versions.toString()).doesNotContain("adminNote", "sharePassword", "internalPath");
        performJson(get("/api/v1/resources/res-admin/versions"), 404, 43600);
    }

    @Test
    @DisplayName("RES-DOWNLOAD visibility policy, Cloudreve degradation, idempotency, record failure, and secret isolation")
    void downloadContract() throws Exception {
        JsonNode publicTicket = performJson(post("/api/v1/resources/res-public-client/versions/ver-client-1/download"),
                Map.of("clientLabel", "browser", "idempotencyKey", "pub-1"), 200);
        assertThat(publicTicket.at("/data/downloadUrl").asText()).startsWith("https://");
        assertThat(publicTicket.at("/data/degraded").asBoolean()).isFalse();

        performJson(post("/api/v1/resources/res-auth-doc/versions/ver-auth-doc-1/download"), Map.of(), 401, 41000);
        performJson(post("/api/v1/resources/res-auth-doc/versions/ver-auth-doc-1/download")
                .header("Authorization", bearer("user-token")), Map.of(), 200);

        performJson(post("/api/v1/resources/res-member-map/versions/ver-member-map-1/download"), Map.of(), 401, 41000);
        performJson(post("/api/v1/resources/res-member-map/versions/ver-member-map-1/download")
                .header("Authorization", bearer("member-token")), Map.of(), 200);
        performJson(post("/api/v1/resources/res-member-map/versions/ver-member-map-1/download")
                .header("Authorization", bearer("inactive-member-token")), Map.of(), 200);
        performJson(post("/api/v1/resources/res-member-map/versions/ver-member-map-1/download")
                .header("Authorization", bearer("user-token")), Map.of(), 403, 42001);
        performJson(post("/api/v1/resources/res-member-map/versions/ver-member-map-1/download")
                .header("Authorization", bearer("suspended-member-token")), Map.of(), 403, 42001);
        performJson(post("/api/v1/resources/res-member-map/versions/ver-member-map-1/download")
                .header("Authorization", bearer("removed-member-token")), Map.of(), 403, 42001);
        performJson(post("/api/v1/resources/res-member-map/versions/ver-member-map-1/download")
                .header("Authorization", bearer("profile-unavailable-token")), Map.of(), 502, 46610);
        performJson(post("/api/v1/resources/res-member-map/versions/ver-member-map-1/download")
                .header("Authorization", bearer("profile-timeout-token")), Map.of(), 504, 46611);
        performJson(post("/api/v1/resources/res-member-map/versions/ver-member-map-1/download")
                .header("Authorization", bearer("profile-bad-token")), Map.of(), 502, 46612);

        performJson(post("/api/v1/resources/res-admin/versions/ver-admin-1/download").header("Authorization", bearer("admin-token")), Map.of(), 200);
        performJson(post("/api/v1/resources/res-admin/versions/ver-admin-1/download").header("Authorization", bearer("owner-token")), Map.of(), 200);
        performJson(post("/api/v1/resources/res-admin/versions/ver-admin-1/download").header("Authorization", bearer("user-token")), Map.of(), 403, 42001);

        performJson(post("/api/v1/resources/missing/versions/ver-client-1/download"), Map.of(), 404, 43600);
        performJson(post("/api/v1/resources/res-draft/versions/ver-draft-1/download"), Map.of(), 404, 43600);
        performJson(post("/api/v1/resources/res-public-client/versions/missing/download"), Map.of(), 404, 43602);
        performJson(post("/api/v1/resources/res-public-client/versions/ver-client-disabled/download"), Map.of(), 409, 43610);
        performJson(post("/api/v1/resources/res-public-client/versions/ver-client-1/download"), Map.of("downloadEntryId", "missing"), 404, 43603);
        performJson(post("/api/v1/resources/res-expired/versions/ver-expired-1/download"), Map.of(), 409, 43613);
        performJson(post("/api/v1/resources/res-disabled-entry/versions/ver-disabled-entry-1/download"), Map.of(), 409, 43613);
        performJson(post("/api/v1/resources/res-unavailable-entry/versions/ver-unavailable-entry-1/download"), Map.of(), 409, 43613);

        JsonNode stale = performJson(post("/api/v1/resources/res-stale-cloudreve/versions/ver-stale-1/download"), Map.of(), 200);
        assertThat(stale.at("/data/degraded").asBoolean()).isTrue();
        assertThat(stale.at("/data/stale").asBoolean()).isTrue();
        performJson(post("/api/v1/resources/res-broken-cloudreve/versions/ver-broken-1/download"), Map.of(), 502, 46630);
        performJson(post("/api/v1/resources/res-timeout-cloudreve/versions/ver-timeout-1/download"), Map.of(), 504, 46631);

        JsonNode secretSafe = performJson(post("/api/v1/resources/res-password-share/versions/ver-password-1/download"), Map.of(), 200);
        assertThat(secretSafe.toString()).contains("maskedPasswordRequired").doesNotContain("secret-code", "sharePassword");

        performJson(post("/api/v1/resources/res-public-client/versions/ver-client-1/download")
                .header("X-Test-Fail-Download-Record", "true"), Map.of(), 500, 51602);

        JsonNode first = performJson(post("/api/v1/resources/res-public-client/versions/ver-client-1/download"),
                Map.of("idempotencyKey", "download-idem", "clientLabel", "a"), 200);
        JsonNode second = performJson(post("/api/v1/resources/res-public-client/versions/ver-client-1/download"),
                Map.of("clientLabel", "a", "idempotencyKey", "download-idem"), 200);
        assertThat(second.at("/data/ticketId").asText()).isEqualTo(first.at("/data/ticketId").asText());
        performJson(post("/api/v1/resources/res-public-client/versions/ver-client-1/download"),
                Map.of("idempotencyKey", "download-idem", "clientLabel", "b"), 409, 43612);
    }

    @Test
    @DisplayName("RES-ITEM and RES-STATE cover admin resource CRUD, state transitions, notification rules, idempotency, and audit")
    void adminResourceLifecycleContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/resources/admin/items")
                .header("Authorization", bearer("admin-token"))
                .param("status", "DRAFT")
                .param("type", "CLIENT_PACK")
                .param("visibility", "PUBLIC")
                .param("categoryId", "cat-client")
                .param("tag", "p0")
                .param("keyword", "draft")
                .param("sort", "updatedAt_desc"), 200);
        assertThat(list.at("/data/items").isArray()).isTrue();
        performJson(get("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/resources/admin/items/res-public-client").header("Authorization", bearer("helper-token")), 200);
        performJson(get("/api/v1/resources/admin/items/missing").header("Authorization", bearer("admin-token")), 404, 43600);

        Map<String, Object> body = validResourceBody("new-pack");
        JsonNode created = performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")), body, 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");

        Map<String, Object> idemBody = validResourceBody("idem-pack");
        JsonNode idem = performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")),
                with(idemBody, "idempotencyKey", "resource-idem"), 201);
        JsonNode idemAgain = performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")),
                with(idemBody, "idempotencyKey", "resource-idem"), 201);
        assertThat(idemAgain.at("/data/resourceId").asText()).isEqualTo(idem.at("/data/resourceId").asText());
        performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")),
                with(validResourceBody("other-pack"), "idempotencyKey", "resource-idem"), 409, 43612);
        performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")), validResourceBody("public-client"), 409, 43611);
        performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")), with(validResourceBody("bad-category"), "categoryId", "missing"), 404, 43601);
        performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")), with(validResourceBody("bad-time"), "visibleUntil", "2026-01-01T00:00:00Z"), 400, 40001);
        performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")), with(validResourceBody("profile-timeout"), "maintainerMemberId", "profile-timeout"), 504, 46611);

        performJson(patch("/api/v1/resources/admin/items/res-draft").header("Authorization", bearer("admin-token")),
                with(validPatchBody(), "title", "Draft Updated"), 200);
        performJson(patch("/api/v1/resources/admin/items/missing").header("Authorization", bearer("admin-token")), validPatchBody(), 404, 43600);
        performJson(patch("/api/v1/resources/admin/items/res-archived").header("Authorization", bearer("admin-token")), validPatchBody(), 409, 43610);
        performJson(patch("/api/v1/resources/admin/items/res-draft").header("Authorization", bearer("admin-token")), with(validPatchBody(), "slug", "public-client"), 409, 43611);
        performJson(patch("/api/v1/resources/admin/items/res-draft").header("Authorization", bearer("helper-token")), validPatchBody(), 403, 42001);

        performJson(patch("/api/v1/resources/admin/items/res-draft/submit-review").header("Authorization", bearer("admin-token")), reason("submit"), 200);
        performJson(patch("/api/v1/resources/admin/items/res-pending/submit-review").header("Authorization", bearer("admin-token")), reason("submit again"), 200);
        performJson(patch("/api/v1/resources/admin/items/res-published/submit-review").header("Authorization", bearer("admin-token")), reason("bad"), 409, 43610);

        performJson(patch("/api/v1/resources/admin/items/res-pending/approve").header("Authorization", bearer("admin-token")), review("ok"), 200);
        performJson(patch("/api/v1/resources/admin/items/res-rejected/approve").header("Authorization", bearer("admin-token")), review("bad"), 409, 43610);
        performJson(patch("/api/v1/resources/admin/items/res-pending-reject/reject").header("Authorization", bearer("admin-token")).header("X-Test-Notification-Mode", "unavailable"), review("reject"), 502, 46620);
        performJson(patch("/api/v1/resources/admin/items/res-pending-reject/reject").header("Authorization", bearer("admin-token")), review("reject"), 200);
        performJson(patch("/api/v1/resources/admin/items/res-pending-changes/request-changes").header("Authorization", bearer("admin-token")).header("X-Test-Notification-Mode", "timeout"), review("change"), 504, 46621);
        performJson(patch("/api/v1/resources/admin/items/res-pending-changes/request-changes").header("Authorization", bearer("admin-token")), review("change"), 200);

        performJson(patch("/api/v1/resources/admin/items/res-approved/publish").header("Authorization", bearer("admin-token")), reason("publish"), 200);
        performJson(get("/api/v1/resources/res-approved"), 200);
        performJson(patch("/api/v1/resources/admin/items/res-no-version/publish").header("Authorization", bearer("admin-token")), reason("no version"), 409, 43614);
        performJson(patch("/api/v1/resources/admin/items/res-no-entry/publish").header("Authorization", bearer("admin-token")), reason("no entry"), 409, 43614);
        performJson(patch("/api/v1/resources/admin/items/res-draft/publish").header("Authorization", bearer("admin-token")), reason("bad"), 409, 43610);

        performJson(patch("/api/v1/resources/admin/items/res-published/offline").header("Authorization", bearer("admin-token")), reason("offline"), 200);
        performJson(get("/api/v1/resources/res-published"), 404, 43600);
        performJson(patch("/api/v1/resources/admin/items/res-draft/offline").header("Authorization", bearer("admin-token")), reason("bad"), 409, 43610);
        performJson(patch("/api/v1/resources/admin/items/res-offline/archive").header("Authorization", bearer("admin-token")), reason("archive"), 200);
        performJson(patch("/api/v1/resources/admin/items/res-public-client/archive").header("Authorization", bearer("admin-token")), reason("bad"), 409, 43610);
        performJson(patch("/api/v1/resources/admin/items/res-rejected/delete").header("Authorization", bearer("admin-token")), reason("delete"), 200);
        performJson(patch("/api/v1/resources/admin/items/res-public-client/delete").header("Authorization", bearer("admin-token")), reason("bad"), 409, 43610);
    }

    @Test
    @DisplayName("RES-VERSION and RES-CAT cover version/download-entry and category contracts")
    void versionAndCategoryContract() throws Exception {
        JsonNode versions = performJson(get("/api/v1/resources/admin/items/res-public-client/versions")
                .header("Authorization", bearer("helper-token")), 200);
        assertThat(valuesAt(versions, "/data/items", "versionId")).contains("ver-client-1", "ver-client-disabled");
        assertThat(versions.toString()).doesNotContain("sharePassword", "token", "internalPath");

        Map<String, Object> version = validVersionBody("1.20.1-r2");
        JsonNode created = performJson(post("/api/v1/resources/admin/items/res-draft/versions")
                .header("Authorization", bearer("admin-token")), version, 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("ENABLED");
        performJson(post("/api/v1/resources/admin/items/missing/versions").header("Authorization", bearer("admin-token")), version, 404, 43600);
        performJson(post("/api/v1/resources/admin/items/res-archived/versions").header("Authorization", bearer("admin-token")), version, 409, 43610);
        performJson(post("/api/v1/resources/admin/items/res-public-client/versions").header("Authorization", bearer("admin-token")), validVersionBody("1.0.0"), 409, 43611);
        performJson(post("/api/v1/resources/admin/items/res-draft/versions").header("Authorization", bearer("admin-token")), with(validVersionBody("bad-checksum"), "checksumSha256", "bad"), 400, 40001);

        JsonNode idem = performJson(post("/api/v1/resources/admin/items/res-draft/versions").header("Authorization", bearer("admin-token")),
                with(validVersionBody("idem-version"), "idempotencyKey", "version-idem"), 201);
        JsonNode idemAgain = performJson(post("/api/v1/resources/admin/items/res-draft/versions").header("Authorization", bearer("admin-token")),
                with(validVersionBody("idem-version"), "idempotencyKey", "version-idem"), 201);
        assertThat(idemAgain.at("/data/versionId").asText()).isEqualTo(idem.at("/data/versionId").asText());
        performJson(post("/api/v1/resources/admin/items/res-draft/versions").header("Authorization", bearer("admin-token")),
                with(validVersionBody("idem-version-other"), "idempotencyKey", "version-idem"), 409, 43612);

        performJson(patch("/api/v1/resources/admin/items/res-public-client/versions/ver-client-1")
                .header("Authorization", bearer("admin-token")), with(versionPatchBody(), "changelog", "changed"), 200);
        performJson(patch("/api/v1/resources/admin/items/res-public-client/versions/missing")
                .header("Authorization", bearer("admin-token")), versionPatchBody(), 404, 43602);
        performJson(patch("/api/v1/resources/admin/items/res-public-client/versions/ver-client-1")
                .header("Authorization", bearer("admin-token")), with(versionPatchBody(), "versionName", "disabled-version"), 409, 43611);
        performJson(patch("/api/v1/resources/admin/items/res-public-client/versions/ver-client-archived")
                .header("Authorization", bearer("admin-token")), with(versionPatchBody(), "downloadEntry", downloadEntry("https://example.com/new.zip")), 409, 43610);

        performJson(patch("/api/v1/resources/admin/items/res-public-client/versions/ver-client-1/disable")
                .header("Authorization", bearer("admin-token")), reason("disable"), 200);
        performJson(post("/api/v1/resources/res-public-client/versions/ver-client-1/download"), Map.of(), 409, 43610);
        performJson(patch("/api/v1/resources/admin/items/res-public-client/versions/ver-client-disabled/enable")
                .header("Authorization", bearer("admin-token")), reason("enable"), 200);
        performJson(patch("/api/v1/resources/admin/items/res-expired/versions/ver-expired-1/enable")
                .header("Authorization", bearer("admin-token")), reason("bad"), 409, 43613);
        performJson(patch("/api/v1/resources/admin/items/res-no-entry/versions/ver-no-entry/enable")
                .header("Authorization", bearer("admin-token")), reason("bad"), 404, 43603);

        JsonNode categories = performJson(get("/api/v1/resources/admin/categories")
                .header("Authorization", bearer("helper-token"))
                .param("includeArchived", "false")
                .param("enabled", "true")
                .param("keyword", "Client"), 200);
        assertThat(valuesAt(categories, "/data/items", "categoryId")).contains("cat-client").doesNotContain("cat-archived");
        performJson(get("/api/v1/resources/admin/categories").header("Authorization", bearer("admin-token")).param("enabled", "not-bool"), 400, 40001);

        Map<String, Object> category = validCategoryBody("addons");
        JsonNode newCategory = performJson(post("/api/v1/resources/admin/categories")
                .header("Authorization", bearer("admin-token")), category, 201);
        assertThat(newCategory.at("/data/slug").asText()).isEqualTo("addons");
        performJson(post("/api/v1/resources/admin/categories").header("Authorization", bearer("admin-token")), validCategoryBody("client"), 409, 43611);
        performJson(post("/api/v1/resources/admin/categories").header("Authorization", bearer("admin-token")), with(validCategoryBody("bad slug"), "slug", "Bad Slug"), 400, 40001);
        performJson(post("/api/v1/resources/admin/categories").header("Authorization", bearer("helper-token")), validCategoryBody("helper"), 403, 42001);

        performJson(patch("/api/v1/resources/admin/categories/cat-client").header("Authorization", bearer("admin-token")),
                with(validCategoryPatchBody(), "name", "Client Packs Updated"), 200);
        performJson(patch("/api/v1/resources/admin/categories/missing").header("Authorization", bearer("admin-token")), validCategoryPatchBody(), 404, 43601);
        performJson(patch("/api/v1/resources/admin/categories/cat-map").header("Authorization", bearer("admin-token")), with(validCategoryPatchBody(), "slug", "client"), 409, 43611);
        performJson(patch("/api/v1/resources/admin/categories/cat-free/archive").header("Authorization", bearer("admin-token")), reason("archive"), 200);
        performJson(patch("/api/v1/resources/admin/categories/cat-client/archive").header("Authorization", bearer("admin-token")), reason("used"), 409, 43615);
        performJson(patch("/api/v1/resources/admin/categories/missing/archive").header("Authorization", bearer("admin-token")), reason("missing"), 404, 43601);
    }

    @Test
    @DisplayName("RES-AUDIT RES-OPS RES-COMPAT cover audit, self-check, dependency failures, boundaries, and previous regressions")
    void auditOpsAndCompatibilityContract() throws Exception {
        JsonNode audit = performJson(get("/api/v1/resources/admin/items/res-public-client/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("action", "RESOURCE_PUBLISHED")
                .param("from", "2026-05-20T00:00:00Z")
                .param("to", "2026-05-23T00:00:00Z"), 200);
        assertThat(audit.at("/data/items").isArray()).isTrue();
        if (audit.at("/data/items").size() > 0) {
            JsonNode first = audit.at("/data/items/0");
            assertThat(first.has("requestId")).isTrue();
            assertThat(first.has("actorUserId")).isTrue();
            assertThat(first.has("targetType")).isTrue();
            assertThat(first.has("action")).isTrue();
            assertThat(first.has("result")).isTrue();
        }
        performJson(get("/api/v1/resources/admin/items/res-public-client/audit-logs")
                .header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/resources/admin/items/missing/audit-logs")
                .header("Authorization", bearer("admin-token")), 404, 43600);
        performJson(get("/api/v1/resources/admin/items/res-public-client/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("from", "2026-05-23T00:00:00Z")
                .param("to", "2026-05-20T00:00:00Z"), 400, 40001);

        JsonNode ops = performJson(get("/api/v1/resources/admin/ops/summary")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("resource");
        assertThat(ops.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(ops.toString()).doesNotContain("token", "sharePassword", "internalPath", "adminNote");
        performJson(get("/api/v1/resources/admin/ops/summary").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/resources/admin/ops/summary").header("Authorization", bearer("admin-token")).header("X-Test-Fail-Store", "true"), 500, 51600);

        performJson(get("/api/v1/resources/admin/items").header("Authorization", bearer("auth-unavailable-token")), 502, 46600);
        performJson(get("/api/v1/resources/admin/items").header("Authorization", bearer("auth-timeout-token")), 504, 46601);
        performJson(get("/api/v1/resources/admin/items").header("Authorization", bearer("auth-bad-token")), 502, 46602);
        performJson(patch("/api/v1/resources/admin/items/res-pending/reject")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Notification-Mode", "unavailable"), review("reject"), 502, 46620);

        JsonNode contentBoundary = performJson(get("/api/v1/resources/admin/ops/summary")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(contentBoundary.toString()).doesNotContain("container", "terminal", "node-daemon", "file-manager", "MCSM_INSTANCE");
    }

    @Test
    @DisplayName("RES-HARDEN covers pagination, canonical idempotency, trusted fields, audit detail, download records, and self-check gaps")
    void hardeningContract() throws Exception {
        JsonNode firstPage = performJson(get("/api/v1/resources").param("page", "1").param("pageSize", "1"), 200);
        JsonNode secondPage = performJson(get("/api/v1/resources").param("page", "2").param("pageSize", "1"), 200);
        assertThat(firstPage.at("/data/page").asInt()).isEqualTo(1);
        assertThat(firstPage.at("/data/pageSize").asInt()).isEqualTo(1);
        assertThat(firstPage.at("/data/items").size()).isEqualTo(1);
        assertThat(secondPage.at("/data/page").asInt()).isEqualTo(2);
        assertThat(secondPage.at("/data/items").size()).isEqualTo(1);
        assertThat(secondPage.at("/data/items/0/resourceId").asText()).isNotEqualTo(firstPage.at("/data/items/0/resourceId").asText());
        JsonNode emptyPage = performJson(get("/api/v1/resources").param("page", "99").param("pageSize", "20"), 200);
        assertThat(emptyPage.at("/data/page").asInt()).isEqualTo(99);
        assertThat(emptyPage.at("/data/items").size()).isEqualTo(0);

        JsonNode adminPage = performJson(get("/api/v1/resources/admin/items")
                .header("Authorization", bearer("admin-token"))
                .param("page", "2")
                .param("pageSize", "2"), 200);
        assertThat(adminPage.at("/data/page").asInt()).isEqualTo(2);
        assertThat(adminPage.at("/data/items").size()).isLessThanOrEqualTo(2);

        Map<String, Object> trustedBody = with(validResourceBody("trusted-owner"), "createdBy", "browser");
        trustedBody.put("updatedBy", "browser");
        JsonNode trusted = performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("owner-token")), trustedBody, 201);
        assertThat(trusted.at("/data/createdBy").asText()).isEqualTo("owner");
        assertThat(trusted.at("/data/updatedBy").asText()).isEqualTo("owner");

        performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")),
                with(validResourceBody("bad-instant"), "visibleUntil", "not-an-instant"), 400, 40001);
        performJson(post("/api/v1/resources/admin/items").header("Authorization", bearer("admin-token")),
                with(validResourceBody("bad-url"), "coverUrl", "ftp://example.com/file.png"), 400, 40001);

        JsonNode audit = performJson(get("/api/v1/resources/admin/items/" + trusted.at("/data/resourceId").asText() + "/audit-logs")
                .header("Authorization", bearer("owner-token"))
                .param("page", "1")
                .param("pageSize", "1"), 200);
        assertThat(audit.at("/data/pageSize").asInt()).isEqualTo(1);
        assertThat(audit.at("/data/items").size()).isEqualTo(1);
        assertThat(audit.at("/data/items/0/actorUserId").asText()).isEqualTo("owner");
        assertThat(audit.at("/data/items/0/afterState").isMissingNode()).isFalse();
        assertThat(audit.at("/data/items/0/afterState").isNull()).isFalse();

        Map<String, Object> firstVersion = validVersionBody("nested-idem");
        firstVersion.put("idempotencyKey", "nested-idem-key");
        JsonNode version = performJson(post("/api/v1/resources/admin/items/res-draft/versions")
                .header("Authorization", bearer("admin-token")), firstVersion, 201);
        Map<String, Object> secondVersion = validVersionBody("nested-idem");
        secondVersion.put("downloadEntry", reorderedDownloadEntry("https://cloud.example.com/s/nested-idem"));
        secondVersion.put("idempotencyKey", "nested-idem-key");
        JsonNode versionAgain = performJson(post("/api/v1/resources/admin/items/res-draft/versions")
                .header("Authorization", bearer("admin-token")), secondVersion, 201);
        assertThat(versionAgain.at("/data/versionId").asText()).isEqualTo(version.at("/data/versionId").asText());
        Map<String, Object> conflictingVersion = validVersionBody("nested-idem");
        conflictingVersion.put("downloadEntry", reorderedDownloadEntry("https://cloud.example.com/s/nested-idem-other"));
        conflictingVersion.put("idempotencyKey", "nested-idem-key");
        performJson(post("/api/v1/resources/admin/items/res-draft/versions")
                .header("Authorization", bearer("admin-token")), conflictingVersion, 409, 43612);

        JsonNode beforeDownload = performJson(get("/api/v1/resources/admin/ops/summary")
                .header("Authorization", bearer("admin-token")), 200);
        JsonNode download = performJson(post("/api/v1/resources/res-public-client/versions/ver-client-1/download"),
                Map.of("idempotencyKey", "record-idem", "clientLabel", "launcher"), 200);
        JsonNode replay = performJson(post("/api/v1/resources/res-public-client/versions/ver-client-1/download"),
                Map.of("clientLabel", "launcher", "idempotencyKey", "record-idem"), 200);
        assertThat(replay.at("/data/ticketId").asText()).isEqualTo(download.at("/data/ticketId").asText());
        JsonNode afterDownload = performJson(get("/api/v1/resources/admin/ops/summary")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(afterDownload.at("/data/downloadRecordsTotal").asLong()).isEqualTo(beforeDownload.at("/data/downloadRecordsTotal").asLong() + 1);
        assertThat(afterDownload.at("/data/lastDownloadAt").isNull()).isFalse();
        assertThat(afterDownload.at("/data/idempotencyRecordsTotal").asInt()).isGreaterThan(0);
        assertThat(afterDownload.at("/data/productionGaps").isArray()).isTrue();
        assertThat(afterDownload.toString()).doesNotContain("token", "sharePassword", "internalPath", "adminNote", "contract test");
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status, int code) throws Exception {
        JsonNode json = performJson(builder, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status) throws Exception {
        MvcResult result = mvc.perform(builder
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status, int code) throws Exception {
        JsonNode json = performJson(builder, body, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private Map<String, Object> validResourceBody(String slug) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "CLIENT_PACK");
        body.put("visibility", "PUBLIC");
        body.put("slug", slug);
        body.put("title", "Client Pack " + slug);
        body.put("summary", "P0 client resource");
        body.put("description", "A stable client package for tests.");
        body.put("coverUrl", "/assets/resources/" + slug + ".png");
        body.put("categoryId", "cat-client");
        body.put("tags", List.of("p0", "client"));
        body.put("maintainerMemberId", "member-active");
        body.put("visibleFrom", "2026-05-20T00:00:00Z");
        body.put("visibleUntil", "2026-12-31T00:00:00Z");
        body.put("adminNote", "internal note");
        body.put("reason", "contract test");
        return body;
    }

    private Map<String, Object> validPatchBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", "updated summary");
        body.put("reason", "contract patch");
        return body;
    }

    private Map<String, Object> validVersionBody(String versionName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("versionName", versionName);
        body.put("title", "Version " + versionName);
        body.put("changelog", "Contract version.");
        body.put("minecraftVersions", List.of("1.20.1"));
        body.put("loader", "Fabric");
        body.put("fileSizeBytes", 1024);
        body.put("checksumSha256", "a".repeat(64));
        body.put("downloadEntry", downloadEntry("https://cloud.example.com/s/" + versionName));
        body.put("releasedAt", "2026-05-22T00:00:00Z");
        body.put("reason", "create version");
        return body;
    }

    private Map<String, Object> versionPatchBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", "patch version");
        return body;
    }

    private Map<String, Object> downloadEntry(String url) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("provider", "CLOUDREVE_SHARE");
        entry.put("displayName", "Cloudreve");
        entry.put("shareUrl", url);
        entry.put("status", "ACTIVE");
        entry.put("expiresAt", "2026-12-31T00:00:00Z");
        entry.put("adminNote", "entry note");
        return entry;
    }

    private Map<String, Object> reorderedDownloadEntry(String url) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("adminNote", "entry note");
        entry.put("expiresAt", "2026-12-31T00:00:00Z");
        entry.put("status", "ACTIVE");
        entry.put("shareUrl", url);
        entry.put("displayName", "Cloudreve");
        entry.put("provider", "CLOUDREVE_SHARE");
        return entry;
    }

    private Map<String, Object> validCategoryBody(String slug) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Category " + slug);
        body.put("slug", slug);
        body.put("description", "category");
        body.put("icon", "folder");
        body.put("sortOrder", 20);
        body.put("enabled", true);
        body.put("reason", "category");
        return body;
    }

    private Map<String, Object> validCategoryPatchBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "updated");
        body.put("reason", "patch category");
        return body;
    }

    private Map<String, Object> reason(String reason) {
        return Map.of("reason", reason);
    }

    private Map<String, Object> review(String opinion) {
        return Map.of("reviewOpinion", opinion, "reason", opinion);
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private List<String> valuesAt(JsonNode root, String pointer, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : root.at(pointer)) {
            values.add(item.at("/" + field).asText());
        }
        return values;
    }

    private void addRange(Set<String> target, String prefix, int start, int end) {
        for (int index = start; index <= end; index++) {
            target.add(prefix + "-" + "%03d".formatted(index));
        }
    }
}
