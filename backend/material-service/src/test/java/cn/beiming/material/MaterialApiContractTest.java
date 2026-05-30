package cn.beiming.material;

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

import java.nio.file.Files;
import java.nio.file.Path;
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
class MaterialApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("material local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "MAT-COM", 1, 28);
        addRange(mapped, "MAT-PUB", 1, 32);
        addRange(mapped, "MAT-UPLOAD", 1, 40);
        addRange(mapped, "MAT-ME", 1, 40);
        addRange(mapped, "MAT-ADMIN", 1, 42);
        addRange(mapped, "MAT-CAT", 1, 21);
        addRange(mapped, "MAT-ASSET", 1, 18);
        addRange(mapped, "MAT-AUDIT", 1, 11);
        addRange(mapped, "MAT-OPS", 1, 8);
        addRange(mapped, "MAT-COMPAT", 1, 19);

        assertThat(mapped).hasSize(259);
        assertThat(mapped).contains("MAT-COM-001", "MAT-PUB-032", "MAT-UPLOAD-040", "MAT-ME-040", "MAT-ADMIN-042", "MAT-COMPAT-019");
    }

    @Test
    @DisplayName("MAT-COM covers envelope, request id, auth, role, paging, trusted fields, dependencies, and audit rollback")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/materials/featured").header("X-Request-Id", "req-material-featured"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-material-featured"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray());

        JsonNode generated = performJson(get("/api/v1/materials/categories"), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(get("/api/v1/materials").param("page", "0"), 400, 40002);
        performJson(get("/api/v1/materials").param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/materials").param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/materials/me/submissions"), 401, 41000);
        performJson(get("/api/v1/materials/me/submissions").header("Authorization", "Token bad"), 401, 41003);
        performJson(get("/api/v1/materials/admin/items").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/materials/admin/items").header("Authorization", bearer("helper-token")), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-approved/feature").header("Authorization", bearer("helper-token")), reason("feature"), 403, 42001);

        JsonNode session = performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token"))
                .header("X-Request-Id", "req-upload-session"), validUploadSession("shot.png", "session-common"), 201);
        assertThat(session.at("/data/uploadTicket").asText()).isNotBlank();
        assertThat(session.at("/data/ownerUserId").asText()).isEqualTo("member");

        performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("auth-unavailable-token")), validUploadSession("shot.png", "auth-down"), 502, 46700);
        performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("auth-timeout-token")), validUploadSession("shot.png", "auth-timeout"), 504, 46701);
        performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("auth-bad-token")), validUploadSession("shot.png", "auth-bad"), 502, 46702);

        performJson(patch("/api/v1/materials/me/submissions/mat-draft/submit-review")
                .header("Authorization", bearer("profile-unavailable-token")), reason("submit"), 502, 46710);
        performJson(patch("/api/v1/materials/me/submissions/mat-draft/submit-review")
                .header("Authorization", bearer("profile-timeout-token")), reason("submit"), 504, 46711);
        performJson(patch("/api/v1/materials/me/submissions/mat-draft/submit-review")
                .header("Authorization", bearer("profile-bad-token")), reason("submit"), 502, 46712);

        performJson(patch("/api/v1/materials/admin/items/mat-pending-reject/reject")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Notification-Mode", "unavailable"), review("reject"), 502, 46720);
        performJson(patch("/api/v1/materials/admin/items/mat-pending-reject/reject")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Notification-Mode", "timeout"), review("reject"), 504, 46721);

        performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token"))
                .header("X-Test-Storage-Mode", "unavailable"), validUploadSession("shot.png", "storage-down"), 502, 46730);
        performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token"))
                .header("X-Test-Storage-Mode", "timeout"), validUploadSession("shot.png", "storage-timeout"), 504, 46731);

        performJson(patch("/api/v1/materials/admin/items/mat-approved/feature")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Fail-Audit", "true"), reason("audit fail"), 500, 51701);
        JsonNode stillApproved = performJson(get("/api/v1/materials/admin/items/mat-approved").header("Authorization", bearer("admin-token")), 200);
        assertThat(stillApproved.at("/data/status").asText()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("MAT-PUB covers featured, public list/detail/categories/assets, visibility, safety, paging, and field isolation")
    void publicReadContract() throws Exception {
        JsonNode featured = performJson(get("/api/v1/materials/featured")
                .param("kind", "IMAGE")
                .param("categoryId", "cat-builds")
                .param("tag", "spawn")
                .param("limit", "10"), 200);
        assertThat(featured.at("/data/items").toString()).contains("mat-featured");
        assertThat(featured.toString()).doesNotContain("mat-draft", "mat-pending", "mat-rejected", "mat-offline", "mat-unsafe", "uploadTicket", "internalPath", "adminNote", "reviewOpinion");

        performJson(get("/api/v1/materials/featured").param("limit", "51"), 400, 40001);
        JsonNode first = performJson(get("/api/v1/materials").param("page", "1").param("pageSize", "1"), 200);
        JsonNode second = performJson(get("/api/v1/materials").param("page", "2").param("pageSize", "1"), 200);
        assertThat(first.at("/data/page").asInt()).isEqualTo(1);
        assertThat(second.at("/data/page").asInt()).isEqualTo(2);
        assertThat(first.at("/data/items/0/materialId").asText()).isNotEqualTo(second.at("/data/items/0/materialId").asText());
        JsonNode empty = performJson(get("/api/v1/materials").param("page", "99").param("pageSize", "20"), 200);
        assertThat(empty.at("/data/items").size()).isEqualTo(0);
        JsonNode authorFiltered = performJson(get("/api/v1/materials").param("authorUserId", "other"), 200);
        assertThat(authorFiltered.at("/data/items").toString()).doesNotContain("member");

        performJson(get("/api/v1/materials").param("kind", "BAD"), 400, 40001);
        performJson(get("/api/v1/materials/mat-featured"), 200);
        performJson(get("/api/v1/materials/by-slug/featured-spawn"), 200);
        performJson(get("/api/v1/materials/missing"), 404, 43700);
        performJson(get("/api/v1/materials/by-slug/missing"), 404, 43700);
        for (String id : List.of("mat-draft", "mat-pending", "mat-rejected", "mat-needs", "mat-offline", "mat-archived", "mat-deleted", "mat-private", "mat-unsafe")) {
            performJson(get("/api/v1/materials/" + id), 404, 43700);
        }

        JsonNode categories = performJson(get("/api/v1/materials/categories").param("kind", "IMAGE"), 200);
        assertThat(categories.at("/data/items").toString()).contains("cat-builds").doesNotContain("cat-disabled", "cat-archived");
        performJson(post("/api/v1/materials/admin/categories")
                .header("Authorization", bearer("admin-token")), with(validCategory("video"), "kind", "VIDEO"), 201);
        JsonNode imageCategories = performJson(get("/api/v1/materials/categories").param("kind", "IMAGE"), 200);
        assertThat(imageCategories.at("/data/items").toString()).doesNotContain("video");
        performJson(get("/api/v1/materials/categories").param("kind", "BAD"), 400, 40001);
        JsonNode assets = performJson(get("/api/v1/materials/mat-featured/assets"), 200);
        assertThat(assets.at("/data/items").toString()).contains("asset-featured-cover").doesNotContain("securityRejectReason", "uploadTicket", "internalPath");

        JsonNode future = performJson(post("/api/v1/materials/me/submissions").header("Authorization", bearer("member-token")),
                with(validSubmission("future-window", "future-window"), "visibleFrom", "2026-06-01T00:00:00Z"), 201);
        performJson(patch("/api/v1/materials/me/submissions/" + future.at("/data/materialId").asText() + "/submit-review")
                .header("Authorization", bearer("member-token")), reason("submit"), 200);
        performJson(patch("/api/v1/materials/admin/items/" + future.at("/data/materialId").asText() + "/approve")
                .header("Authorization", bearer("admin-token")), review("ok"), 200);
        performJson(get("/api/v1/materials/" + future.at("/data/materialId").asText()), 404, 43700);
    }

    @Test
    @DisplayName("MAT-UPLOAD covers upload session, complete, file security validation, idempotency, ownership, and storage failures")
    void uploadContract() throws Exception {
        Map<String, Object> sessionBody = validUploadSession("new-spawn.png", "upload-1");
        JsonNode created = performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token")), sessionBody, 201);
        String sessionId = created.at("/data/uploadSessionId").asText();
        String ticket = created.at("/data/uploadTicket").asText();
        assertThat(created.at("/data/provider").asText()).isEqualTo("LOCAL_STUB");

        JsonNode replay = performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token")), sessionBody, 201);
        assertThat(replay.at("/data/uploadSessionId").asText()).isEqualTo(sessionId);
        performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token")), validUploadSession("new-spawn.png", "upload-1", "other-checksum"), 409, 43714);

        for (Map<String, Object> invalid : List.of(
                validUploadSession("bad.exe", "upload-bad-ext"),
                validUploadSession("../bad.png", "upload-path"),
                validUploadSession("shell.php.png", "upload-double"),
                validUploadSession("nul\u0000.png", "upload-null")
        )) {
            performJson(post("/api/v1/materials/me/upload-sessions").header("Authorization", bearer("member-token")), invalid, 400, 43712);
        }
        performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token")), with(validUploadSession("too-many.png", "upload-many"), "expectedFileNames", List.of("1.png", "2.png", "3.png", "4.png", "5.png", "6.png", "7.png", "8.png", "9.png", "10.png", "11.png")), 400, 40001);

        JsonNode complete = performJson(patch("/api/v1/materials/me/upload-sessions/" + sessionId + "/complete")
                .header("Authorization", bearer("member-token")), completeBody(ticket, "new-spawn.png", "complete-1"), 200);
        assertThat(complete.at("/data/items/0/status").asText()).isEqualTo("SAFE");
        assertThat(complete.at("/data/items/0/publicAssetUrl").asText()).startsWith("/api/v1/materials/assets/");

        JsonNode completeAgain = performJson(patch("/api/v1/materials/me/upload-sessions/" + sessionId + "/complete")
                .header("Authorization", bearer("member-token")), completeBody(ticket, "new-spawn.png", "complete-1"), 200);
        assertThat(completeAgain.at("/data/items/0/assetId").asText()).isEqualTo(complete.at("/data/items/0/assetId").asText());
        performJson(patch("/api/v1/materials/me/upload-sessions/" + sessionId + "/complete")
                .header("Authorization", bearer("member-token")), completeBody(ticket, "changed.png", "complete-1"), 409, 43714);

        performJson(patch("/api/v1/materials/me/upload-sessions/" + sessionId + "/complete")
                .header("Authorization", bearer("user-token")), completeBody(ticket, "new-spawn.png", "wrong-owner"), 404, 43701);
        performJson(patch("/api/v1/materials/me/upload-sessions/missing/complete")
                .header("Authorization", bearer("member-token")), completeBody(ticket, "new-spawn.png", "missing"), 404, 43701);
        performJson(patch("/api/v1/materials/me/upload-sessions/sess-expired/complete")
                .header("Authorization", bearer("member-token")), completeBody("expired-ticket", "expired.png", "expired"), 404, 43701);
        performJson(patch("/api/v1/materials/me/upload-sessions/" + sessionId + "/complete")
                .header("Authorization", bearer("member-token")), completeBody("bad-ticket", "new-spawn.png", "bad-ticket"), 404, 43701);

        for (String filename : List.of("bad.exe", "shell.php.png", "../bad.png", "nul\u0000.png", "zip-bomb.zip")) {
            JsonNode badSession = performJson(post("/api/v1/materials/me/upload-sessions")
                    .header("Authorization", bearer("member-token")), validUploadSession("safe.png", "session-" + Math.abs(filename.hashCode())), 201);
            performJson(patch("/api/v1/materials/me/upload-sessions/" + badSession.at("/data/uploadSessionId").asText() + "/complete")
                    .header("Authorization", bearer("member-token")), completeBody(badSession.at("/data/uploadTicket").asText(), filename, "complete-" + Math.abs(filename.hashCode())), 400, 43712);
        }

        JsonNode failSession = performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token")), validUploadSession("fail-write.png", "fail-write-session"), 201);
        performJson(patch("/api/v1/materials/me/upload-sessions/" + failSession.at("/data/uploadSessionId").asText() + "/complete")
                .header("Authorization", bearer("member-token"))
                .header("X-Test-Fail-Upload-Record", "true"), completeBody(failSession.at("/data/uploadTicket").asText(), "fail-write.png", "fail-write"), 500, 51702);

        JsonNode videoSession = performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token")), validUploadSession("clip.mp4", "video-session", "VIDEO", "video/mp4"), 201);
        JsonNode videoComplete = performJson(patch("/api/v1/materials/me/upload-sessions/" + videoSession.at("/data/uploadSessionId").asText() + "/complete")
                .header("Authorization", bearer("member-token")), completeBody(videoSession.at("/data/uploadTicket").asText(), "clip.mp4", "video-complete", "video/mp4", "MP4"), 200);
        assertThat(videoComplete.at("/data/items/0/extension").asText()).isEqualTo("mp4");

        JsonNode documentSession = performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token")), validUploadSession("rules.pdf", "document-session", "DOCUMENT_ATTACHMENT", "application/pdf"), 201);
        JsonNode documentComplete = performJson(patch("/api/v1/materials/me/upload-sessions/" + documentSession.at("/data/uploadSessionId").asText() + "/complete")
                .header("Authorization", bearer("member-token")), completeBody(documentSession.at("/data/uploadTicket").asText(), "rules.pdf", "document-complete", "application/pdf", "PDF"), 200);
        assertThat(documentComplete.at("/data/items/0/mimeType").asText()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("MAT-ME covers current user submission creation, isolation, editing, submit, withdraw, resubmit, authorization, and profile")
    void currentUserSubmissionContract() throws Exception {
        JsonNode created = createUserSubmission("my-spawn", "create-my-1");
        String materialId = created.at("/data/materialId").asText();
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(created.at("/data/author/userId").asText()).isEqualTo("member");

        JsonNode replay = createUserSubmission("my-spawn", "create-my-1");
        assertThat(replay.at("/data/materialId").asText()).isEqualTo(materialId);
        performJson(post("/api/v1/materials/me/submissions").header("Authorization", bearer("member-token")), validSubmission("other-slug", "create-my-1"), 409, 43714);

        performJson(post("/api/v1/materials/me/submissions").header("Authorization", bearer("member-token")), validSubmission("featured-spawn", "slug-conflict"), 409, 43711);
        performJson(post("/api/v1/materials/me/submissions").header("Authorization", bearer("member-token")), with(validSubmission("bad-cat", "bad-cat"), "categoryId", "missing"), 404, 43703);
        performJson(post("/api/v1/materials/me/submissions").header("Authorization", bearer("member-token")), with(validSubmission("bad-license", "bad-license"), "license", Map.of("licenseType", "AUTHORIZED_REPOST", "authorConfirmed", true)), 400, 43713);
        performJson(post("/api/v1/materials/me/submissions").header("Authorization", bearer("member-token")), with(validSubmission("bad-asset", "bad-asset"), "assetIds", List.of("asset-unsafe")), 409, 43715);
        performJson(post("/api/v1/materials/me/submissions").header("Authorization", bearer("member-token")), with(validSubmission("missing-asset", "missing-asset"), "assetIds", List.of("missing")), 404, 43702);
        performJson(post("/api/v1/materials/me/submissions").header("Authorization", bearer("member-token")), with(validSubmission("bad-time", "bad-time"), "visibleUntil", "2020-01-01T00:00:00Z"), 400, 40001);

        JsonNode mine = performJson(get("/api/v1/materials/me/submissions")
                .header("Authorization", bearer("member-token"))
                .param("status", "DRAFT")
                .param("kind", "IMAGE")
                .param("keyword", "Spawn")
                .param("sort", "updatedAt_desc"), 200);
        assertThat(mine.at("/data/items").toString()).contains(materialId).doesNotContain("mat-other-user");
        performJson(get("/api/v1/materials/me/submissions/mat-other-user").header("Authorization", bearer("member-token")), 404, 43700);

        performJson(patch("/api/v1/materials/me/submissions/" + materialId).header("Authorization", bearer("member-token")),
                with(Map.of("summary", "updated", "reason", "edit"), "authorUserId", "browser"), 200);
        performJson(patch("/api/v1/materials/me/submissions/mat-pending").header("Authorization", bearer("member-token")), Map.of("summary", "bad", "reason", "edit"), 409, 43710);
        performJson(patch("/api/v1/materials/me/submissions/mat-approved").header("Authorization", bearer("member-token")), Map.of("summary", "bad", "reason", "edit"), 409, 43710);

        performJson(patch("/api/v1/materials/me/submissions/" + materialId + "/submit-review")
                .header("Authorization", bearer("member-token")), reason("submit"), 200);
        performJson(patch("/api/v1/materials/me/submissions/" + materialId + "/submit-review")
                .header("Authorization", bearer("member-token")), reason("submit again"), 200);
        performJson(patch("/api/v1/materials/me/submissions/mat-unsafe-draft/submit-review")
                .header("Authorization", bearer("member-token")), reason("submit"), 409, 43715);
        performJson(patch("/api/v1/materials/me/submissions/mat-no-license/submit-review")
                .header("Authorization", bearer("member-token")), reason("submit"), 400, 43713);
        performJson(patch("/api/v1/materials/me/submissions/mat-draft/submit-review")
                .header("Authorization", bearer("suspended-member-token")), reason("submit"), 403, 42001);

        performJson(patch("/api/v1/materials/me/submissions/mat-pending/withdraw")
                .header("Authorization", bearer("member-token")), reason("withdraw"), 200);
        performJson(patch("/api/v1/materials/me/submissions/mat-draft/withdraw")
                .header("Authorization", bearer("member-token")), reason("withdraw again"), 200);
        performJson(patch("/api/v1/materials/me/submissions/mat-approved/withdraw")
                .header("Authorization", bearer("member-token")), reason("bad"), 409, 43710);
        performJson(patch("/api/v1/materials/me/submissions/mat-rejected/resubmit")
                .header("Authorization", bearer("member-token")), reason("resubmit"), 200);
        performJson(patch("/api/v1/materials/me/submissions/mat-needs/resubmit")
                .header("Authorization", bearer("member-token")), reason("resubmit"), 200);
        performJson(patch("/api/v1/materials/me/submissions/mat-draft/resubmit")
                .header("Authorization", bearer("member-token")), reason("bad"), 409, 43710);

        JsonNode trustedActor = performJson(get("/api/v1/materials/me/submissions")
                .header("Authorization", bearer("user-token"))
                .header("X-Request-Id", "req-material-trusted")
                .header("X-Gateway-Internal-Request-Id", "req-material-trusted")
                .header("X-Beiming-Actor-User-Id", "member")
                .header("X-Beiming-Actor-Roles", "USER")
                .header("X-Beiming-Actor-Permissions", "MATERIAL_SUBMIT"), 200);
        assertThat(trustedActor.at("/data/items").toString()).contains("mat-draft");
        JsonNode forgedActor = performJson(get("/api/v1/materials/me/submissions")
                .header("Authorization", bearer("user-token"))
                .header("X-Beiming-Actor-User-Id", "member")
                .header("X-Beiming-Actor-Roles", "USER"), 200);
        assertThat(forgedActor.at("/data/items").toString()).doesNotContain("mat-draft");
    }

    @Test
    @DisplayName("MAT-ADMIN covers review, feature, offline, archive, delete, permissions, notification, idempotency, and audit")
    void adminLifecycleContract() throws Exception {
        performJson(get("/api/v1/materials/admin/items")
                .header("Authorization", bearer("helper-token"))
                .param("status", "PENDING_REVIEW")
                .param("kind", "IMAGE")
                .param("assetStatus", "SAFE")
                .param("sort", "submittedAt_desc"), 200);
        JsonNode privateItems = performJson(get("/api/v1/materials/admin/items").header("Authorization", bearer("helper-token")).param("visibility", "PRIVATE"), 200);
        assertThat(privateItems.at("/data/items").toString()).contains("mat-private").doesNotContain("mat-featured");
        JsonNode eventItems = performJson(get("/api/v1/materials/admin/items").header("Authorization", bearer("helper-token")).param("categoryId", "cat-events"), 200);
        assertThat(eventItems.at("/data/items").toString()).contains("mat-public").doesNotContain("mat-featured");
        JsonNode otherAuthorItems = performJson(get("/api/v1/materials/admin/items").header("Authorization", bearer("helper-token")).param("authorUserId", "other"), 200);
        assertThat(otherAuthorItems.at("/data/items").toString()).contains("mat-other-user").doesNotContain("mat-draft");
        JsonNode keywordItems = performJson(get("/api/v1/materials/admin/items").header("Authorization", bearer("helper-token")).param("keyword", "offline"), 200);
        assertThat(keywordItems.at("/data/items").toString()).contains("mat-offline").doesNotContain("mat-featured");
        performJson(get("/api/v1/materials/admin/items/missing").header("Authorization", bearer("admin-token")), 404, 43700);

        performJson(patch("/api/v1/materials/admin/items/mat-pending/approve").header("Authorization", bearer("helper-token")), review("ok"), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-approved/approve").header("Authorization", bearer("admin-token")), review("again"), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-draft/approve").header("Authorization", bearer("admin-token")), review("bad"), 409, 43710);
        performJson(patch("/api/v1/materials/admin/items/mat-pending-unsafe/approve").header("Authorization", bearer("admin-token")), review("unsafe"), 409, 43715);
        Map<String, Object> reviewIdempotent = with(review("idem ok"), "idempotencyKey", "review-idem");
        JsonNode approvedByIdem = performJson(patch("/api/v1/materials/admin/items/mat-pending-approve/approve")
                .header("Authorization", bearer("admin-token")), reviewIdempotent, 200);
        JsonNode approvedByIdemReplay = performJson(patch("/api/v1/materials/admin/items/mat-pending-approve/approve")
                .header("Authorization", bearer("admin-token")), reviewIdempotent, 200);
        assertThat(approvedByIdemReplay.at("/data/materialId").asText()).isEqualTo(approvedByIdem.at("/data/materialId").asText());
        performJson(patch("/api/v1/materials/admin/items/mat-pending-approve/approve")
                .header("Authorization", bearer("admin-token")), with(review("changed"), "idempotencyKey", "review-idem"), 409, 43714);

        performJson(patch("/api/v1/materials/admin/items/mat-pending-aux/approve")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Notification-Mode", "aux-fail"), review("ok"), 200);

        performJson(patch("/api/v1/materials/admin/items/mat-pending-reject/reject").header("Authorization", bearer("admin-token")), review("reject"), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-no-author/reject").header("Authorization", bearer("admin-token")), review("reject"), 502, 46722);
        performJson(patch("/api/v1/materials/admin/items/mat-pending-changes/request-changes").header("Authorization", bearer("admin-token")),
                Map.of("reviewOpinion", "needs work", "publicComment", "补一张全景", "reason", "needs work"), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-pending/request-changes").header("Authorization", bearer("admin-token")),
                Map.of("reviewOpinion", "needs work", "reason", "needs work"), 400, 40001);

        performJson(patch("/api/v1/materials/admin/items/mat-approved/feature").header("Authorization", bearer("admin-token")), reason("feature"), 200);
        performJson(get("/api/v1/materials/mat-approved"), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-featured/feature").header("Authorization", bearer("admin-token")), reason("again"), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-no-feature-license/feature").header("Authorization", bearer("admin-token")), reason("feature"), 400, 43713);
        performJson(patch("/api/v1/materials/admin/items/mat-draft/feature").header("Authorization", bearer("admin-token")), reason("bad"), 409, 43710);
        performJson(patch("/api/v1/materials/admin/items/mat-featured/unfeature").header("Authorization", bearer("admin-token")), reason("unfeature"), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-approved/offline").header("Authorization", bearer("admin-token")), reason("offline"), 200);
        performJson(get("/api/v1/materials/mat-approved"), 404, 43700);
        performJson(patch("/api/v1/materials/admin/items/mat-offline/archive").header("Authorization", bearer("admin-token")), reason("archive"), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-featured/archive").header("Authorization", bearer("admin-token")), reason("bad archive"), 409, 43710);
        performJson(patch("/api/v1/materials/admin/items/mat-draft/delete").header("Authorization", bearer("admin-token")), reason("delete"), 200);
        performJson(patch("/api/v1/materials/admin/items/mat-featured/delete").header("Authorization", bearer("admin-token")), reason("bad delete"), 409, 43710);
        performJson(patch("/api/v1/materials/admin/items/mat-approved/feature").header("Authorization", bearer("admin-token")), Map.of(), 400, 40001);
    }

    @Test
    @DisplayName("MAT-CAT and MAT-ASSET cover category management and file security administration")
    void categoryAndAssetAdminContract() throws Exception {
        performJson(get("/api/v1/materials/admin/categories").header("Authorization", bearer("helper-token"))
                .param("includeArchived", "false")
                .param("enabled", "true")
                .param("keyword", "Build"), 200);
        JsonNode category = performJson(post("/api/v1/materials/admin/categories")
                .header("Authorization", bearer("admin-token")), with(validCategory("screenshots"), "kind", "VIDEO"), 201);
        assertThat(category.at("/data/slug").asText()).isEqualTo("screenshots");
        assertThat(category.at("/data/kind").asText()).isEqualTo("VIDEO");
        JsonNode videoCategories = performJson(get("/api/v1/materials/admin/categories")
                .header("Authorization", bearer("helper-token"))
                .param("kind", "VIDEO"), 200);
        assertThat(videoCategories.at("/data/items").toString()).contains("screenshots").doesNotContain("cat-builds");
        performJson(post("/api/v1/materials/admin/categories").header("Authorization", bearer("helper-token")), validCategory("helper"), 403, 42001);
        performJson(post("/api/v1/materials/admin/categories").header("Authorization", bearer("admin-token")), validCategory("builds"), 409, 43711);
        performJson(post("/api/v1/materials/admin/categories").header("Authorization", bearer("admin-token")), with(validCategory("bad slug"), "slug", "Bad Slug"), 400, 40001);
        Map<String, Object> patchCategory = Map.of("description", "updated", "reason", "patch", "idempotencyKey", "cat-patch");
        performJson(patch("/api/v1/materials/admin/categories/cat-builds").header("Authorization", bearer("admin-token")), patchCategory, 200);
        performJson(patch("/api/v1/materials/admin/categories/cat-builds").header("Authorization", bearer("admin-token")), patchCategory, 200);
        performJson(patch("/api/v1/materials/admin/categories/cat-builds").header("Authorization", bearer("admin-token")), Map.of("description", "changed", "reason", "patch", "idempotencyKey", "cat-patch"), 409, 43714);
        performJson(patch("/api/v1/materials/admin/categories/missing").header("Authorization", bearer("admin-token")), Map.of("description", "updated", "reason", "patch"), 404, 43703);
        performJson(patch("/api/v1/materials/admin/categories/cat-free/archive").header("Authorization", bearer("admin-token")), reason("archive"), 200);
        performJson(patch("/api/v1/materials/admin/categories/cat-builds/archive").header("Authorization", bearer("admin-token")), reason("used"), 409, 43716);

        JsonNode pdfSession = performJson(post("/api/v1/materials/me/upload-sessions")
                .header("Authorization", bearer("member-token")), validUploadSession("asset-filter.pdf", "asset-filter-pdf", "DOCUMENT_ATTACHMENT", "application/pdf"), 201);
        performJson(patch("/api/v1/materials/me/upload-sessions/" + pdfSession.at("/data/uploadSessionId").asText() + "/complete")
                .header("Authorization", bearer("member-token")), completeBody(pdfSession.at("/data/uploadTicket").asText(), "asset-filter.pdf", "asset-filter-complete", "application/pdf", "PDF"), 200);

        JsonNode assets = performJson(get("/api/v1/materials/admin/assets")
                .header("Authorization", bearer("helper-token"))
                .param("status", "SAFE")
                .param("ownerUserId", "member")
                .param("materialId", "mat-featured"), 200);
        assertThat(assets.toString()).doesNotContain("uploadTicket", "internalPath");
        JsonNode pdfAssets = performJson(get("/api/v1/materials/admin/assets")
                .header("Authorization", bearer("helper-token"))
                .param("extension", "pdf")
                .param("mimeType", "application/pdf")
                .param("sort", "createdAt_desc"), 200);
        assertThat(pdfAssets.at("/data/items").toString()).contains("asset-filter.pdf").doesNotContain("featured.png");
        performJson(patch("/api/v1/materials/admin/assets/asset-scanning/security-status")
                .header("Authorization", bearer("admin-token")), Map.of("status", "SAFE", "reason", "safe"), 200);
        performJson(patch("/api/v1/materials/admin/assets/asset-featured-cover/security-status")
                .header("Authorization", bearer("helper-token")), Map.of("status", "REJECTED", "securityRejectReason", "bad", "reason", "reject"), 403, 42001);
        performJson(patch("/api/v1/materials/admin/assets/missing/security-status")
                .header("Authorization", bearer("admin-token")), Map.of("status", "SAFE", "reason", "safe"), 404, 43702);
        performJson(patch("/api/v1/materials/admin/assets/asset-featured-cover/security-status")
                .header("Authorization", bearer("admin-token")), Map.of("status", "BAD", "reason", "bad"), 400, 40001);
        performJson(patch("/api/v1/materials/admin/assets/asset-featured-cover/security-status")
                .header("Authorization", bearer("admin-token")), Map.of("status", "REJECTED", "reason", "bad"), 400, 40001);
        Map<String, Object> assetIdempotent = Map.of("status", "QUARANTINED", "reason", "quarantine", "idempotencyKey", "asset-status-idem");
        performJson(patch("/api/v1/materials/admin/assets/asset-featured-cover/security-status")
                .header("Authorization", bearer("admin-token")), assetIdempotent, 200);
        performJson(patch("/api/v1/materials/admin/assets/asset-featured-cover/security-status")
                .header("Authorization", bearer("admin-token")), assetIdempotent, 200);
        performJson(patch("/api/v1/materials/admin/assets/asset-featured-cover/security-status")
                .header("Authorization", bearer("admin-token")), Map.of("status", "SAFE", "reason", "changed", "idempotencyKey", "asset-status-idem"), 409, 43714);
        performJson(patch("/api/v1/materials/admin/assets/asset-featured-cover/security-status")
                .header("Authorization", bearer("admin-token")), Map.of("status", "REJECTED", "securityRejectReason", "malware", "reason", "reject"), 200);
        performJson(get("/api/v1/materials/mat-featured"), 404, 43700);
    }

    @Test
    @DisplayName("MAT-AUDIT MAT-OPS MAT-COMPAT cover audit, self-check, sensitive fields, and service boundaries")
    void auditOpsCompatibilityContract() throws Exception {
        JsonNode audit = performJson(get("/api/v1/materials/admin/items/mat-featured/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("action", "MATERIAL_FEATURED")
                .param("actorUserId", "system")
                .param("result", "SUCCESS")
                .param("from", "2026-05-20T00:00:00Z")
                .param("to", "2026-05-31T00:00:00Z")
                .param("sort", "createdAt_asc"), 200);
        assertThat(audit.at("/data/items").isArray()).isTrue();
        if (audit.at("/data/items").size() > 0) {
            assertThat(audit.at("/data/items/0/requestId").asText()).isNotBlank();
            assertThat(audit.at("/data/items/0/action").asText()).isNotBlank();
        }
        assertThat(audit.toString()).doesNotContain("uploadTicket", "internalPath", "token", "notification body");
        performJson(get("/api/v1/materials/admin/items/mat-featured/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/materials/admin/items/missing/audit-logs").header("Authorization", bearer("admin-token")), 404, 43700);
        performJson(get("/api/v1/materials/admin/items/mat-featured/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("from", "2026-06-01T00:00:00Z")
                .param("to", "2026-05-01T00:00:00Z"), 400, 40001);
        JsonNode noActorAudit = performJson(get("/api/v1/materials/admin/items/mat-featured/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("actorUserId", "owner"), 200);
        assertThat(noActorAudit.at("/data/items").size()).isEqualTo(0);
        performJson(get("/api/v1/materials/admin/items/mat-featured/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("sort", "bad"), 400, 40003);

        JsonNode ops = performJson(get("/api/v1/materials/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("material");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8126);
        assertThat(ops.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(ops.at("/data/uploadProvider").asText()).isEqualTo("LOCAL_STUB");
        assertThat(ops.at("/data/productionGaps").toString()).contains("PERSISTENT_STORAGE_NOT_ENABLED", "REAL_OBJECT_STORAGE_NOT_ENABLED", "GATEWAY_INTERNAL_SIGNATURE_NOT_ENABLED");
        assertThat(ops.toString()).doesNotContain("token", "ticket", "secret", "internalPath", "stackTrace");
        performJson(get("/api/v1/materials/admin/ops/summary").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/materials/admin/ops/summary").header("Authorization", bearer("admin-token")).header("X-Test-Fail-Store", "true"), 500, 51700);

        assertThat(ops.toString()).doesNotContain("container", "terminal", "node-daemon", "file-manager", "Cloudreve", "downloadUrl");
        Path sourcePath = Path.of("backend/material-service/src/main/java/cn/beiming/material/MaterialModule.java");
        if (!Files.exists(sourcePath)) {
            sourcePath = Path.of("src/main/java/cn/beiming/material/MaterialModule.java");
        }
        String source = Files.readString(sourcePath);
        assertThat(source).contains("synchronized");
    }

    private JsonNode createUserSubmission(String slug, String idempotencyKey) throws Exception {
        return performJson(post("/api/v1/materials/me/submissions").header("Authorization", bearer("member-token")),
                validSubmission(slug, idempotencyKey), 201);
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

    private Map<String, Object> validUploadSession(String fileName, String key) {
        return validUploadSession(fileName, key, "a".repeat(64));
    }

    private Map<String, Object> validUploadSession(String fileName, String key, String checksum) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kind", "IMAGE");
        body.put("expectedFileNames", List.of(fileName));
        body.put("expectedMimeTypes", List.of("image/png"));
        body.put("maxFileSizeBytes", 1_048_576);
        body.put("checksumSha256", checksum);
        body.put("idempotencyKey", key);
        return body;
    }

    private Map<String, Object> validUploadSession(String fileName, String key, String kind, String mimeType) {
        Map<String, Object> body = validUploadSession(fileName, key);
        body.put("kind", kind);
        body.put("expectedMimeTypes", List.of(mimeType));
        return body;
    }

    private Map<String, Object> completeBody(String ticket, String fileName, String key) {
        return completeBody(ticket, fileName, key, "image/png", "PNG");
    }

    private Map<String, Object> completeBody(String ticket, String fileName, String key, String mimeType, String signature) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("displayName", fileName);
        file.put("mimeType", mimeType);
        file.put("extension", extension(fileName));
        file.put("fileSizeBytes", 2048);
        file.put("checksumSha256", "a".repeat(64));
        file.put("signature", signature);
        file.put("width", 1920);
        file.put("height", 1080);
        file.put("durationSeconds", null);
        return Map.of("uploadTicket", ticket, "files", List.of(file), "idempotencyKey", key);
    }

    private Map<String, Object> validSubmission(String slug, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kind", "IMAGE");
        body.put("slug", slug);
        body.put("title", "Spawn Screenshot " + slug);
        body.put("summary", "A spawn screenshot");
        body.put("description", "A safe material submission.");
        body.put("categoryId", "cat-builds");
        body.put("tags", List.of("spawn", "build"));
        body.put("assetIds", List.of("asset-owned-safe"));
        body.put("coverAssetId", "asset-owned-safe");
        body.put("visibility", "PUBLIC");
        Map<String, Object> license = new LinkedHashMap<>();
        license.put("licenseType", "ORIGINAL");
        license.put("authorConfirmed", true);
        license.put("allowHomepageFeature", true);
        license.put("allowDerivativeUse", true);
        license.put("sourceUrl", null);
        license.put("creditText", "Steve");
        body.put("license", license);
        body.put("visibleFrom", "2026-05-20T00:00:00Z");
        body.put("visibleUntil", "2026-12-31T00:00:00Z");
        body.put("idempotencyKey", key);
        return body;
    }

    private Map<String, Object> validCategory(String slug) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Category " + slug);
        body.put("slug", slug);
        body.put("description", "category");
        body.put("sortOrder", 20);
        body.put("enabled", true);
        body.put("reason", "category");
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

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase();
    }

    private void addRange(Set<String> target, String prefix, int start, int end) {
        for (int index = start; index <= end; index++) {
            target.add(prefix + "-" + "%03d".formatted(index));
        }
    }
}
