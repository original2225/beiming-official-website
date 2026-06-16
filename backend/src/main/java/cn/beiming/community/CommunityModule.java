package cn.beiming.community;

import cn.beiming.engagement.persistence.CommunityPersistence;
import cn.beiming.engagement.TrustedGatewayAuth;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
class CommunityModule {
    @Bean
    CommunityStore communityStore(CommunityTestControls testControls, CommunityPersistence persistence) {
        return new CommunityStore(testControls, persistence);
    }

    @Bean
    TestCommunityAuthProvider communityAuthProvider() {
        return new TestCommunityAuthProvider();
    }

    @Bean
    CommunityTestControls communityTestControls(@Value("${community.test-controls.enabled:false}") boolean enabled) {
        return new CommunityTestControls(enabled);
    }

    @Bean
    @ConditionalOnMissingBean
    CommunityFlowEvidenceRecorder communityFlowEvidenceRecorder() {
        return new NoopCommunityFlowEvidenceRecorder();
    }
}

@RestController
@RequestMapping("/api/v1/community")
class CommunityController {
    private final CommunityStore store;
    private final TestCommunityAuthProvider auth;
    private final CommunityFlowEvidenceRecorder evidenceRecorder;

    CommunityController(CommunityStore store, TestCommunityAuthProvider auth, CommunityFlowEvidenceRecorder evidenceRecorder) {
        this.store = store;
        this.auth = auth;
        this.evidenceRecorder = evidenceRecorder;
    }

    @GetMapping("/boards")
    Map<String, Object> publicBoards(@RequestParam Map<String, String> query) {
        return ok(store.publicBoards(query));
    }

    @GetMapping("/boards/{boardId}")
    Map<String, Object> publicBoard(@PathVariable String boardId) {
        return ok(store.publicBoard(boardId));
    }

    @GetMapping("/posts")
    Map<String, Object> publicPosts(@RequestParam Map<String, String> query) {
        return ok(store.publicPosts(query));
    }

    @GetMapping("/posts/{postId}")
    Map<String, Object> publicPost(@PathVariable String postId, HttpServletRequest request) {
        return ok(store.publicPost(postId, request));
    }

    @GetMapping("/posts/{postId}/comments")
    Map<String, Object> publicComments(@PathVariable String postId, @RequestParam Map<String, String> query) {
        return ok(store.publicComments(postId, query));
    }

    @GetMapping("/polls/{pollId}")
    Map<String, Object> publicPoll(@PathVariable String pollId) {
        return ok(store.publicPoll(pollId));
    }

    @GetMapping("/search")
    Map<String, Object> search(@RequestParam Map<String, String> query) {
        return ok(store.search(query));
    }

    @PostMapping("/me/posts")
    ResponseEntity<Map<String, Object>> createPost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        MutationResult result = store.createPost(actor, bodyOrEmpty(body), request);
        int responseCode = result.created() ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
        if (result.created()) {
            evidenceRecorder.recordPostWrite(request, "COMMUNITY_POST_CREATED", result.value(), responseCode);
        }
        return ResponseEntity.status(responseCode).body(okBody(result.value()));
    }

    @PatchMapping("/me/posts/{postId}")
    Map<String, Object> updatePost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String postId,
                                   @RequestBody(required = false) Map<String, Object> body,
                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.updatePost(actor, postId, bodyOrEmpty(body), request));
    }

    @PostMapping("/me/posts/{postId}/submit")
    Map<String, Object> submitPost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String postId,
                                   @RequestBody(required = false) Map<String, Object> body,
                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.submitPost(actor, postId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/me/posts/{postId}/withdraw")
    Map<String, Object> withdrawPost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String postId,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.withdrawPost(actor, postId, bodyOrEmpty(body), request));
    }

    @PostMapping("/me/posts/{postId}/comments")
    ResponseEntity<Map<String, Object>> createComment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @PathVariable String postId,
                                                      @RequestBody(required = false) Map<String, Object> body,
                                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        Map<String, Object> payload = store.createComment(actor, postId, bodyOrEmpty(body), request);
        evidenceRecorder.recordCommentWrite(request, "COMMUNITY_COMMENT_CREATED", payload, HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(payload));
    }

    @PatchMapping("/me/comments/{commentId}")
    Map<String, Object> updateComment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String commentId,
                                      @RequestBody(required = false) Map<String, Object> body,
                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.updateComment(actor, commentId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/me/comments/{commentId}/archive")
    Map<String, Object> archiveComment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String commentId,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.archiveComment(actor, commentId, bodyOrEmpty(body), request));
    }

    @PostMapping("/me/posts/{postId}/like")
    Map<String, Object> likePost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String postId,
                                 @RequestBody(required = false) Map<String, Object> body,
                                 HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.setPostLike(actor, postId, true, bodyOrEmpty(body), request));
    }

    @DeleteMapping("/me/posts/{postId}/like")
    Map<String, Object> unlikePost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String postId,
                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.setPostLike(actor, postId, false, Map.of(), request));
    }

    @PostMapping("/me/comments/{commentId}/like")
    Map<String, Object> likeComment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String commentId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.setCommentLike(actor, commentId, true, bodyOrEmpty(body), request));
    }

    @DeleteMapping("/me/comments/{commentId}/like")
    Map<String, Object> unlikeComment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String commentId,
                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.setCommentLike(actor, commentId, false, Map.of(), request));
    }

    @PostMapping("/me/posts/{postId}/favorite")
    Map<String, Object> favorite(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String postId,
                                 @RequestBody(required = false) Map<String, Object> body,
                                 HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.setFavorite(actor, postId, true, bodyOrEmpty(body), request));
    }

    @DeleteMapping("/me/posts/{postId}/favorite")
    Map<String, Object> unfavorite(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String postId,
                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.setFavorite(actor, postId, false, Map.of(), request));
    }

    @PostMapping("/me/polls/{pollId}/votes")
    Map<String, Object> vote(@RequestHeader(value = "Authorization", required = false) String authorization,
                             @PathVariable String pollId,
                             @RequestBody(required = false) Map<String, Object> body,
                             HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.vote(actor, pollId, bodyOrEmpty(body), request));
    }

    @PostMapping("/me/posts/{postId}/reports")
    ResponseEntity<Map<String, Object>> reportPost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @PathVariable String postId,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        Map<String, Object> payload = store.createReport(actor, "POST", postId, bodyOrEmpty(body), request);
        evidenceRecorder.recordReportWrite(request, "COMMUNITY_REPORT_CREATED", payload, HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(payload));
    }

    @PostMapping("/me/comments/{commentId}/reports")
    ResponseEntity<Map<String, Object>> reportComment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @PathVariable String commentId,
                                                      @RequestBody(required = false) Map<String, Object> body,
                                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        Map<String, Object> payload = store.createReport(actor, "COMMENT", commentId, bodyOrEmpty(body), request);
        evidenceRecorder.recordReportWrite(request, "COMMUNITY_REPORT_CREATED", payload, HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(payload));
    }

    @GetMapping("/me/reports")
    Map<String, Object> myReports(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.myReports(actor, query));
    }

    @PostMapping("/me/tickets")
    ResponseEntity<Map<String, Object>> createTicket(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                     @RequestBody(required = false) Map<String, Object> body,
                                                     HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(store.createTicket(actor, bodyOrEmpty(body), request)));
    }

    @GetMapping("/me/tickets")
    Map<String, Object> myTickets(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.myTickets(actor, query));
    }

    @GetMapping("/me/tickets/{ticketId}")
    Map<String, Object> myTicket(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String ticketId) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.myTicket(actor, ticketId));
    }

    @PatchMapping("/me/tickets/{ticketId}")
    Map<String, Object> appendTicket(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String ticketId,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.appendTicket(actor, ticketId, bodyOrEmpty(body), request));
    }

    @PostMapping("/me/tickets/{ticketId}/close")
    Map<String, Object> closeTicket(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String ticketId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        CommunityUser actor = auth.requireUser(authorization);
        return ok(store.closeTicket(actor, ticketId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/boards")
    Map<String, Object> adminBoards(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminBoards(query));
    }

    @PostMapping("/admin/boards")
    ResponseEntity<Map<String, Object>> createBoard(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @RequestBody(required = false) Map<String, Object> body,
                                                    HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        MutationResult result = store.createBoard(actor, bodyOrEmpty(body), request);
        int responseCode = result.created() ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
        if (result.created()) {
            evidenceRecorder.recordBoardWrite(request, "COMMUNITY_BOARD_CREATED", result.value(), responseCode);
        }
        return ResponseEntity.status(responseCode).body(okBody(result.value()));
    }

    @PatchMapping("/admin/boards/{boardId}")
    Map<String, Object> updateBoard(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String boardId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.updateBoard(actor, boardId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/boards/{boardId}/archive")
    Map<String, Object> archiveBoard(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String boardId,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.archiveBoard(actor, boardId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/posts")
    Map<String, Object> adminPosts(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminPosts(query));
    }

    @GetMapping("/admin/posts/{postId}")
    Map<String, Object> adminPost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String postId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminPost(postId));
    }

    @PatchMapping("/admin/posts/{postId}/approve")
    Map<String, Object> approvePost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String postId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.reviewPost(actor, postId, "APPROVED", bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/posts/{postId}/reject")
    Map<String, Object> rejectPost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String postId,
                                   @RequestBody(required = false) Map<String, Object> body,
                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.reviewPost(actor, postId, "REJECTED", bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/posts/{postId}/request-changes")
    Map<String, Object> requestPostChanges(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable String postId,
                                           @RequestBody(required = false) Map<String, Object> body,
                                           HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.reviewPost(actor, postId, "NEEDS_CHANGES", bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/posts/{postId}/offline")
    Map<String, Object> offlinePost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String postId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.offlinePost(actor, postId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/posts/{postId}/archive")
    Map<String, Object> archivePost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String postId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.archivePost(actor, postId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/posts/{postId}/delete")
    Map<String, Object> deletePost(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String postId,
                                   @RequestBody(required = false) Map<String, Object> body,
                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.deletePost(actor, postId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/comments")
    Map<String, Object> adminComments(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminComments(query));
    }

    @PatchMapping("/admin/comments/{commentId}/approve")
    Map<String, Object> approveComment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String commentId,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.reviewComment(actor, commentId, "APPROVED", bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/comments/{commentId}/reject")
    Map<String, Object> rejectComment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String commentId,
                                      @RequestBody(required = false) Map<String, Object> body,
                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.reviewComment(actor, commentId, "REJECTED", bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/comments/{commentId}/offline")
    Map<String, Object> offlineComment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String commentId,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.offlineComment(actor, commentId, bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/reports")
    Map<String, Object> adminReports(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminReports(query));
    }

    @GetMapping("/admin/reports/{reportId}")
    Map<String, Object> adminReport(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String reportId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminReport(reportId));
    }

    @PatchMapping("/admin/reports/{reportId}/assign")
    Map<String, Object> assignReport(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String reportId,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.assignReport(actor, reportId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/reports/{reportId}/resolve")
    Map<String, Object> resolveReport(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String reportId,
                                      @RequestBody(required = false) Map<String, Object> body,
                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.finishReport(actor, reportId, "RESOLVED", bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/reports/{reportId}/dismiss")
    Map<String, Object> dismissReport(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String reportId,
                                      @RequestBody(required = false) Map<String, Object> body,
                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.finishReport(actor, reportId, "DISMISSED", bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/tickets")
    Map<String, Object> adminTickets(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminTickets(query));
    }

    @GetMapping("/admin/tickets/{ticketId}")
    Map<String, Object> adminTicket(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String ticketId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminTicket(ticketId));
    }

    @PatchMapping("/admin/tickets/{ticketId}/assign")
    Map<String, Object> assignTicket(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String ticketId,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.assignTicket(actor, ticketId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/tickets/{ticketId}/messages")
    ResponseEntity<Map<String, Object>> replyTicket(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @PathVariable String ticketId,
                                                    @RequestBody(required = false) Map<String, Object> body,
                                                    HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(store.replyTicket(actor, ticketId, bodyOrEmpty(body), request)));
    }

    @PatchMapping("/admin/tickets/{ticketId}/status")
    Map<String, Object> ticketStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String ticketId,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.ticketStatus(actor, ticketId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/penalties")
    ResponseEntity<Map<String, Object>> createPenalty(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @RequestBody(required = false) Map<String, Object> body,
                                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(store.createPenalty(actor, bodyOrEmpty(body), request)));
    }

    @PatchMapping("/admin/penalties/{penaltyId}")
    Map<String, Object> updatePenalty(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String penaltyId,
                                      @RequestBody(required = false) Map<String, Object> body,
                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.updatePenalty(actor, penaltyId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/penalties/{penaltyId}/revoke")
    Map<String, Object> revokePenalty(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String penaltyId,
                                      @RequestBody(required = false) Map<String, Object> body,
                                      HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.revokePenalty(actor, penaltyId, bodyOrEmpty(body), request));
    }

    @PostMapping("/admin/polls")
    ResponseEntity<Map<String, Object>> createPoll(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(store.createPoll(actor, bodyOrEmpty(body), request)));
    }

    @PatchMapping("/admin/polls/{pollId}")
    Map<String, Object> updatePoll(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String pollId,
                                   @RequestBody(required = false) Map<String, Object> body,
                                   HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.updatePoll(actor, pollId, bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/polls/{pollId}/open")
    Map<String, Object> openPoll(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String pollId,
                                 @RequestBody(required = false) Map<String, Object> body,
                                 HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.pollStatus(actor, pollId, "OPEN", bodyOrEmpty(body), request));
    }

    @PatchMapping("/admin/polls/{pollId}/close")
    Map<String, Object> closePoll(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String pollId,
                                  @RequestBody(required = false) Map<String, Object> body,
                                  HttpServletRequest request) {
        CommunityUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.pollStatus(actor, pollId, "CLOSED", bodyOrEmpty(body), request));
    }

    @GetMapping("/admin/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(query));
    }

    @GetMapping("/admin/ops/summary")
    Map<String, Object> opsSummary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        CommunityUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.opsSummary(actor));
    }

    static Map<String, Object> ok(Object data) {
        return okBody(data);
    }

    static Map<String, Object> okBody(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        response.put("requestId", requestId());
        return response;
    }

    static String requestId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Object value = attrs == null ? null : attrs.getRequest().getAttribute("requestId");
        return value == null ? "req-" + UUID.randomUUID() : value.toString();
    }

    private static Map<String, Object> bodyOrEmpty(Map<String, Object> body) {
        return body == null ? Map.of() : body;
    }
}

class CommunityStore {
    private static final String NOW = "2026-05-24T12:00:00Z";
    private static final Set<String> BOARD_STATUSES = Set.of("DRAFT", "ACTIVE", "LOCKED", "ARCHIVED");
    private static final Set<String> VISIBILITIES = Set.of("PUBLIC", "MEMBER_ONLY", "STAFF_ONLY");
    private static final Set<String> POST_TYPES = Set.of("DISCUSSION", "QUESTION", "GUIDE", "SHOWCASE", "SUGGESTION", "ANNOUNCEMENT_DISCUSSION", "RESOURCE_DISCUSSION");
    private static final Set<String> POST_STATUSES = Set.of("DRAFT", "PENDING_REVIEW", "APPROVED", "NEEDS_CHANGES", "REJECTED", "LOCKED", "OFFLINE", "ARCHIVED", "DELETED");
    private static final Set<String> COMMENT_STATUSES = Set.of("PENDING_REVIEW", "APPROVED", "NEEDS_CHANGES", "REJECTED", "OFFLINE", "ARCHIVED", "DELETED");
    private static final Set<String> REPORT_STATUSES = Set.of("OPEN", "UNDER_REVIEW", "RESOLVED", "DISMISSED", "ESCALATED", "ARCHIVED");
    private static final Set<String> TICKET_STATUSES = Set.of("OPEN", "WAITING_STAFF", "WAITING_USER", "RESOLVED", "CLOSED", "ARCHIVED");
    private static final Set<String> TICKET_TYPES = Set.of("BAN_APPEAL", "WHITELIST_ISSUE", "ACCOUNT_ISSUE", "RESOURCE_ISSUE", "BUG_REPORT", "CONTENT_DISPUTE", "OTHER");
    private static final Set<String> PENALTY_TYPES = Set.of("WARNING", "MUTE", "BAN", "WHITELIST_REVIEW_REQUIRED", "POST_RESTRICTED", "SUBMISSION_RESTRICTED");
    private static final Set<String> PENALTY_STATUSES = Set.of("ACTIVE", "EXPIRED", "REVOKED", "ARCHIVED");
    private static final Set<String> POLL_STATUSES = Set.of("DRAFT", "OPEN", "CLOSED", "ARCHIVED");
    private final Map<String, BoardRecord> boards = new ConcurrentHashMap<>();
    private final Map<String, PostRecord> posts = new ConcurrentHashMap<>();
    private final Map<String, CommentRecord> comments = new ConcurrentHashMap<>();
    private final Map<String, PollRecord> polls = new ConcurrentHashMap<>();
    private final Map<String, ReportRecord> reports = new ConcurrentHashMap<>();
    private final Map<String, TicketRecord> tickets = new ConcurrentHashMap<>();
    private final Map<String, PenaltyRecord> penalties = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final Set<String> postLikes = ConcurrentHashMap.newKeySet();
    private final Set<String> commentLikes = ConcurrentHashMap.newKeySet();
    private final Set<String> favorites = ConcurrentHashMap.newKeySet();
    private final Set<String> pollVotes = ConcurrentHashMap.newKeySet();
    private final Set<String> postViewFingerprints = ConcurrentHashMap.newKeySet();
    private final List<Map<String, Object>> audits = java.util.Collections.synchronizedList(new ArrayList<>());
    private final CommunityTestControls testControls;
    private final CommunityPersistence persistence;
    private int idSeq = 1000;

    CommunityStore(CommunityTestControls testControls, CommunityPersistence persistence) {
        this.testControls = testControls;
        this.persistence = persistence;
        seedBoard();
    }

    private void seedBoard() {
        BoardRecord board = new BoardRecord();
        board.boardId = "board-general";
        board.slug = "general";
        board.name = "综合讨论";
        board.description = "默认公开社区板块";
        board.visibility = "PUBLIC";
        board.status = "ACTIVE";
        board.allowedPostTypes = new ArrayList<>(List.of("DISCUSSION", "QUESTION", "SUGGESTION", "RESOURCE_DISCUSSION"));
        board.tags = new ArrayList<>(List.of("general"));
        board.sortOrder = 1;
        board.createdAt = NOW;
        board.updatedAt = NOW;
        boards.put(board.boardId, board);
    }

    Map<String, Object> publicBoards(Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        sort(query, Set.of("sortOrder_asc", "lastPostAt_desc", "postCount_desc", "createdAt_desc"), "sortOrder_asc");
        String visibility = enumQuery(query, "visibility", VISIBILITIES);
        String keyword = lower(query.get("keyword"));
        List<Map<String, Object>> rows = boards.values().stream()
                .filter(board -> "ACTIVE".equals(board.status))
                .filter(board -> visibility == null || visibility.equals(board.visibility))
                .filter(board -> "PUBLIC".equals(board.visibility))
                .filter(board -> keyword == null || board.name.toLowerCase().contains(keyword) || board.slug.toLowerCase().contains(keyword))
                .sorted(Comparator.comparingInt(board -> board.sortOrder))
                .map(this::boardView)
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> publicBoard(String boardId) {
        BoardRecord board = requirePublicBoard(boardId);
        Map<String, Object> view = boardView(board);
        view.put("recentPosts", publicPostRows(Map.of("boardId", boardId), 1, 5));
        return view;
    }

    Map<String, Object> publicPosts(Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        sort(query, Set.of("lastCommentAt_desc", "createdAt_desc", "likeCount_desc", "viewCount_desc"), "createdAt_desc");
        return pageRows(publicPostRows(query, page, pageSize), page, pageSize);
    }

    Map<String, Object> publicPost(String postId, HttpServletRequest request) {
        PostRecord post = requirePost(postId);
        if (!"APPROVED".equals(post.status)) throw new CommunityException(404, 49001, "post not found");
        if (postViewFingerprints.add(postId + ":" + accessFingerprint(request))) post.viewCount++;
        return postView(post, false);
    }

    Map<String, Object> publicComments(String postId, Map<String, String> query) {
        PostRecord post = requirePost(postId);
        if (!"APPROVED".equals(post.status)) throw new CommunityException(404, 49001, "post not found");
        int page = page(query);
        int pageSize = pageSize(query);
        sort(query, Set.of("createdAt_asc", "createdAt_desc", "likeCount_desc"), "createdAt_asc");
        String parent = query.get("parentCommentId");
        List<Map<String, Object>> rows = comments.values().stream()
                .filter(comment -> postId.equals(comment.postId))
                .filter(comment -> "APPROVED".equals(comment.status))
                .filter(comment -> parent == null || Objects.equals(parent, comment.parentCommentId))
                .sorted(Comparator.comparing(comment -> comment.createdAt))
                .map(comment -> commentView(comment, false))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> publicPoll(String pollId) {
        PollRecord poll = requirePoll(pollId);
        if (!Set.of("OPEN", "CLOSED").contains(poll.status)) throw new CommunityException(404, 49003, "poll not found");
        return pollView(poll);
    }

    Map<String, Object> search(Map<String, String> query) {
        String keyword = query.get("keyword");
        if (keyword == null || keyword.isBlank() || keyword.length() > 80) throw new CommunityException(400, 40001, "invalid keyword");
        enumQuery(query, "scope", Set.of("ALL", "POST", "COMMENT", "BOARD"));
        return publicPosts(Map.of("keyword", keyword));
    }

    Map<String, Object> adminBoards(Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        sort(query, Set.of("sortOrder_asc", "lastPostAt_desc", "postCount_desc", "createdAt_desc"), "sortOrder_asc");
        String status = enumQuery(query, "status", BOARD_STATUSES);
        List<Map<String, Object>> rows = boards.values().stream()
                .filter(board -> status == null || status.equals(board.status))
                .sorted(Comparator.comparingInt(board -> board.sortOrder))
                .map(this::boardView)
                .toList();
        return pageRows(rows, page, pageSize);
    }

    synchronized MutationResult createBoard(CommunityUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        IdempotencyRecord existing = replay(actor.userId(), "createBoard", body);
        if (existing != null) return new MutationResult(false, existing.value());
        failBeforeWrite(request);
        BoardRecord board = new BoardRecord();
        board.boardId = "board-" + (++idSeq);
        board.slug = validateSlug(body);
        if (boards.values().stream().anyMatch(item -> item.slug.equals(board.slug))) throw new CommunityException(409, 49010, "board slug exists");
        board.name = validateRequiredString(body, "name", 2, 40);
        board.description = validateRequiredString(body, "description", 1, 300);
        board.visibility = enumBody(body, "visibility", VISIBILITIES, "PUBLIC");
        board.status = enumBody(body, "status", BOARD_STATUSES, "ACTIVE");
        board.allowedPostTypes = stringList(body, "allowedPostTypes", POST_TYPES, 1, 10);
        board.tags = looseStringList(body, "tags", 0, 12, 24);
        board.sortOrder = intBody(body, "sortOrder", 0);
        board.createdAt = NOW;
        board.updatedAt = NOW;
        boards.put(board.boardId, board);
        audit(actor, "COMMUNITY_BOARD", board.boardId, "COMMUNITY_BOARD_CREATED", "MEDIUM", null, board.status, "create board");
        Map<String, Object> value = boardView(board);
        remember(actor.userId(), "createBoard", body, value);
        persist(request, actor, "community.board.create", "COMMUNITY_BOARD_CREATED", "COMMUNITY_BOARD", board.boardId, "MEDIUM", null, board.status, "create board", withSnapshotType("BOARD", value), body, value, 201);
        return new MutationResult(true, value);
    }

    synchronized Map<String, Object> updateBoard(CommunityUser actor, String boardId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        failBeforeWrite(request);
        BoardRecord board = requireBoard(boardId);
        if ("ARCHIVED".equals(board.status)) throw new CommunityException(409, 49010, "board archived");
        if (body.containsKey("name")) board.name = validateRequiredString(body, "name", 2, 40);
        if (body.containsKey("description")) board.description = validateRequiredString(body, "description", 1, 300);
        if (body.containsKey("visibility")) board.visibility = enumBody(body, "visibility", VISIBILITIES, board.visibility);
        if (body.containsKey("status")) board.status = enumBody(body, "status", BOARD_STATUSES, board.status);
        if (body.containsKey("allowedPostTypes")) board.allowedPostTypes = stringList(body, "allowedPostTypes", POST_TYPES, 1, 10);
        if (body.containsKey("tags")) board.tags = looseStringList(body, "tags", 0, 12, 24);
        if (body.containsKey("sortOrder")) board.sortOrder = intBody(body, "sortOrder", board.sortOrder);
        board.updatedAt = NOW;
        audit(actor, "COMMUNITY_BOARD", board.boardId, "COMMUNITY_BOARD_UPDATED", "MEDIUM", null, board.status, "update board");
        Map<String, Object> value = boardView(board);
        persist(request, actor, "community.board.update", "COMMUNITY_BOARD_UPDATED", "COMMUNITY_BOARD", board.boardId, "MEDIUM", null, board.status, "update board", withSnapshotType("BOARD", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> archiveBoard(CommunityUser actor, String boardId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        failBeforeWrite(request);
        BoardRecord board = requireBoard(boardId);
        board.status = "ARCHIVED";
        board.archivedAt = NOW;
        board.updatedAt = NOW;
        audit(actor, "COMMUNITY_BOARD", board.boardId, "COMMUNITY_BOARD_ARCHIVED", "MEDIUM", null, board.status, "archive board");
        Map<String, Object> value = boardView(board);
        persist(request, actor, "community.board.archive", "COMMUNITY_BOARD_ARCHIVED", "COMMUNITY_BOARD", board.boardId, "MEDIUM", null, board.status, "archive board", withSnapshotType("BOARD", value), body, value, 200);
        return value;
    }

    synchronized MutationResult createPost(CommunityUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        IdempotencyRecord existing = replay(actor.userId(), "createPost", body);
        if (existing != null) return new MutationResult(false, existing.value());
        ensureCanWritePost(actor);
        failProfile(request);
        failLinked(body, request);
        BoardRecord board = requireBoard(stringRequired(body, "boardId"));
        if (!"ACTIVE".equals(board.status)) throw new CommunityException(409, 49010, "board not writable");
        String type = enumBody(body, "type", POST_TYPES, "DISCUSSION");
        if (!board.allowedPostTypes.contains(type)) throw new CommunityException(409, 49010, "post type denied");
        PostRecord post = new PostRecord();
        post.postId = "post-" + (++idSeq);
        post.boardId = board.boardId;
        post.type = type;
        post.title = validateRequiredString(body, "title", 2, 80);
        post.summary = optionalString(body, "summary", 200);
        post.body = validateRequiredString(body, "body", 1, 20000);
        post.tags = looseStringList(body, "tags", 0, 8, 24);
        post.status = "DRAFT";
        post.author = author(actor, false);
        post.linkedContentSnapshot = linkedContent(body);
        post.linkedResourceSnapshot = linkedResource(body);
        post.createdAt = NOW;
        post.updatedAt = NOW;
        posts.put(post.postId, post);
        audit(actor, "COMMUNITY_POST", post.postId, "COMMUNITY_POST_CREATED", "LOW", null, post.status, "create post");
        Map<String, Object> value = postView(post, true);
        remember(actor.userId(), "createPost", body, value);
        persist(request, actor, "community.post.create", "COMMUNITY_POST_CREATED", "COMMUNITY_POST", post.postId, "LOW", null, post.status, "create post", withSnapshotType("POST", value), body, value, 201);
        return new MutationResult(true, value);
    }

    synchronized Map<String, Object> updatePost(CommunityUser actor, String postId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        PostRecord post = requirePost(postId);
        requireOwner(actor, post.authorUserId(), 49001);
        if (!Set.of("DRAFT", "NEEDS_CHANGES").contains(post.status)) throw new CommunityException(409, 49011, "post state denied");
        if (body.containsKey("title")) post.title = validateRequiredString(body, "title", 2, 80);
        if (body.containsKey("summary")) post.summary = optionalString(body, "summary", 200);
        if (body.containsKey("body")) post.body = validateRequiredString(body, "body", 1, 20000);
        if (body.containsKey("tags")) post.tags = looseStringList(body, "tags", 0, 8, 24);
        post.updatedAt = NOW;
        audit(actor, "COMMUNITY_POST", post.postId, "COMMUNITY_POST_UPDATED", "LOW", null, post.status, "update post");
        Map<String, Object> value = postView(post, true);
        persist(request, actor, "community.post.update", "COMMUNITY_POST_UPDATED", "COMMUNITY_POST", post.postId, "LOW", null, post.status, "update post", withSnapshotType("POST", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> submitPost(CommunityUser actor, String postId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        PostRecord post = requirePost(postId);
        requireOwner(actor, post.authorUserId(), 49001);
        ensureCanWritePost(actor);
        if (!Set.of("DRAFT", "NEEDS_CHANGES", "PENDING_REVIEW").contains(post.status)) throw new CommunityException(409, 49011, "post state denied");
        post.status = "PENDING_REVIEW";
        post.submittedAt = NOW;
        post.updatedAt = NOW;
        audit(actor, "COMMUNITY_POST", post.postId, "COMMUNITY_POST_SUBMITTED", "LOW", null, post.status, "submit post");
        Map<String, Object> value = postView(post, true);
        persist(request, actor, "community.post.submit", "COMMUNITY_POST_SUBMITTED", "COMMUNITY_POST", post.postId, "LOW", null, post.status, "submit post", withSnapshotType("POST", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> withdrawPost(CommunityUser actor, String postId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 200);
        PostRecord post = requirePost(postId);
        requireOwner(actor, post.authorUserId(), 49001);
        if (!Set.of("DRAFT", "PENDING_REVIEW", "NEEDS_CHANGES").contains(post.status)) throw new CommunityException(409, 49011, "post state denied");
        post.status = "DRAFT";
        post.updatedAt = NOW;
        audit(actor, "COMMUNITY_POST", post.postId, "COMMUNITY_POST_WITHDRAWN", "LOW", null, post.status, "withdraw post");
        Map<String, Object> value = postView(post, true);
        persist(request, actor, "community.post.withdraw", "COMMUNITY_POST_WITHDRAWN", "COMMUNITY_POST", post.postId, "LOW", null, post.status, "withdraw post", withSnapshotType("POST", value), body, value, 200);
        return value;
    }

    Map<String, Object> adminPosts(Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        sort(query, Set.of("lastCommentAt_desc", "createdAt_desc", "likeCount_desc", "viewCount_desc"), "createdAt_desc");
        String status = enumQuery(query, "status", POST_STATUSES);
        List<Map<String, Object>> rows = posts.values().stream()
                .filter(post -> status == null || status.equals(post.status))
                .sorted(Comparator.comparing((PostRecord post) -> post.createdAt).reversed())
                .map(post -> postView(post, true))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> adminPost(String postId) {
        return postView(requirePost(postId), true);
    }

    synchronized Map<String, Object> reviewPost(CommunityUser actor, String postId, String targetStatus, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        validateRequiredString(body, "reviewComment", 1, 1000);
        failBeforeWrite(request);
        PostRecord post = requirePost(postId);
        if (!Set.of("PENDING_REVIEW", "NEEDS_CHANGES").contains(post.status)) throw new CommunityException(409, 49011, "post state denied");
        post.status = targetStatus;
        post.reviewComment = string(body.get("reviewComment"));
        post.reviewerUserId = actor.userId();
        post.reviewedAt = NOW;
        post.updatedAt = NOW;
        post.notificationStatus = notificationStatus(request);
        post.notificationFailure = notificationFailure(request);
        audit(actor, "COMMUNITY_POST", post.postId, "COMMUNITY_POST_" + targetStatus, "MEDIUM", null, post.status, "review post");
        auditNotificationFailure(actor, "COMMUNITY_POST", post.postId, post.notificationFailure);
        Map<String, Object> value = postView(post, true);
        persist(request, actor, "community.post.review", "COMMUNITY_POST_" + targetStatus, "COMMUNITY_POST", post.postId, "MEDIUM", null, post.status, "review post", withSnapshotType("POST", value), body, value, 200);
        if (post.notificationFailure != null) {
            persist(request, actor, null, "COMMUNITY_NOTIFICATION_FAILED", "COMMUNITY_POST", post.postId, "LOW", null, "FAILED", "notification failed", withSnapshotType("POST", value), Map.of(), value, 200);
        }
        return value;
    }

    synchronized Map<String, Object> offlinePost(CommunityUser actor, String postId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        validateRequiredString(body, "publicReason", 1, 200);
        failBeforeWrite(request);
        PostRecord post = requirePost(postId);
        if (!Set.of("APPROVED", "LOCKED").contains(post.status)) throw new CommunityException(409, 49011, "post state denied");
        post.status = "OFFLINE";
        post.offlineAt = NOW;
        post.updatedAt = NOW;
        post.notificationStatus = notificationStatus(request);
        post.notificationFailure = notificationFailure(request);
        audit(actor, "COMMUNITY_POST", post.postId, "COMMUNITY_POST_OFFLINE", "MEDIUM", null, post.status, "offline post");
        auditNotificationFailure(actor, "COMMUNITY_POST", post.postId, post.notificationFailure);
        Map<String, Object> value = postView(post, true);
        persist(request, actor, "community.post.offline", "COMMUNITY_POST_OFFLINE", "COMMUNITY_POST", post.postId, "MEDIUM", null, post.status, "offline post", withSnapshotType("POST", value), body, value, 200);
        if (post.notificationFailure != null) {
            persist(request, actor, null, "COMMUNITY_NOTIFICATION_FAILED", "COMMUNITY_POST", post.postId, "LOW", null, "FAILED", "notification failed", withSnapshotType("POST", value), Map.of(), value, 200);
        }
        return value;
    }

    synchronized Map<String, Object> archivePost(CommunityUser actor, String postId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        PostRecord post = requirePost(postId);
        post.status = "ARCHIVED";
        post.archivedAt = NOW;
        post.updatedAt = NOW;
        audit(actor, "COMMUNITY_POST", post.postId, "COMMUNITY_POST_ARCHIVED", "MEDIUM", null, post.status, "archive post");
        Map<String, Object> value = postView(post, true);
        persist(request, actor, "community.post.archive", "COMMUNITY_POST_ARCHIVED", "COMMUNITY_POST", post.postId, "MEDIUM", null, post.status, "archive post", withSnapshotType("POST", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> deletePost(CommunityUser actor, String postId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        if (!"DELETE_COMMUNITY_POST".equals(string(body.get("confirmText")))) throw new CommunityException(403, 42003, "missing confirmation");
        PostRecord post = requirePost(postId);
        post.status = "DELETED";
        post.deletedAt = NOW;
        post.updatedAt = NOW;
        audit(actor, "COMMUNITY_POST", post.postId, "COMMUNITY_POST_DELETED", "HIGH", null, post.status, "delete post");
        Map<String, Object> value = postView(post, true);
        persist(request, actor, "community.post.delete", "COMMUNITY_POST_DELETED", "COMMUNITY_POST", post.postId, "HIGH", null, post.status, "delete post", withSnapshotType("POST", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> createComment(CommunityUser actor, String postId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        ensureCanComment(actor);
        failProfile(request);
        PostRecord post = requirePost(postId);
        if (!"APPROVED".equals(post.status)) throw new CommunityException(409, 49011, "post not commentable");
        String parent = string(body.get("parentCommentId"));
        if (parent != null) {
            CommentRecord parentComment = requireComment(parent);
            if (parentComment.parentCommentId != null) throw new CommunityException(409, 49023, "comment depth exceeded");
        }
        CommentRecord comment = new CommentRecord();
        comment.commentId = "comment-" + (++idSeq);
        comment.postId = postId;
        comment.parentCommentId = parent;
        comment.body = validateRequiredString(body, "body", 1, 10000);
        comment.status = "PENDING_REVIEW";
        comment.author = author(actor, false);
        comment.createdAt = NOW;
        comment.updatedAt = NOW;
        comment.submittedAt = NOW;
        comments.put(comment.commentId, comment);
        audit(actor, "COMMUNITY_COMMENT", comment.commentId, "COMMUNITY_COMMENT_CREATED", "LOW", null, comment.status, "create comment");
        Map<String, Object> value = commentView(comment, true);
        persist(request, actor, "community.comment.create", "COMMUNITY_COMMENT_CREATED", "COMMUNITY_COMMENT", comment.commentId, "LOW", null, comment.status, "create comment", withSnapshotType("COMMENT", value), body, value, 201);
        return value;
    }

    synchronized Map<String, Object> updateComment(CommunityUser actor, String commentId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        CommentRecord comment = requireComment(commentId);
        requireOwner(actor, comment.authorUserId(), 49002);
        if (!Set.of("PENDING_REVIEW", "NEEDS_CHANGES", "APPROVED").contains(comment.status)) throw new CommunityException(409, 49012, "comment state denied");
        comment.body = validateRequiredString(body, "body", 1, 10000);
        comment.status = "PENDING_REVIEW";
        comment.updatedAt = NOW;
        audit(actor, "COMMUNITY_COMMENT", comment.commentId, "COMMUNITY_COMMENT_UPDATED", "LOW", null, comment.status, "update comment");
        Map<String, Object> value = commentView(comment, true);
        persist(request, actor, "community.comment.update", "COMMUNITY_COMMENT_UPDATED", "COMMUNITY_COMMENT", comment.commentId, "LOW", null, comment.status, "update comment", withSnapshotType("COMMENT", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> archiveComment(CommunityUser actor, String commentId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 200);
        CommentRecord comment = requireComment(commentId);
        requireOwner(actor, comment.authorUserId(), 49002);
        comment.status = "ARCHIVED";
        comment.deletedAt = NOW;
        comment.updatedAt = NOW;
        audit(actor, "COMMUNITY_COMMENT", comment.commentId, "COMMUNITY_COMMENT_ARCHIVED", "LOW", null, comment.status, "archive comment");
        Map<String, Object> value = commentView(comment, true);
        persist(request, actor, "community.comment.archive", "COMMUNITY_COMMENT_ARCHIVED", "COMMUNITY_COMMENT", comment.commentId, "LOW", null, comment.status, "archive comment", withSnapshotType("COMMENT", value), body, value, 200);
        return value;
    }

    Map<String, Object> adminComments(Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", COMMENT_STATUSES);
        List<Map<String, Object>> rows = comments.values().stream()
                .filter(comment -> status == null || status.equals(comment.status))
                .sorted(Comparator.comparing((CommentRecord comment) -> comment.createdAt).reversed())
                .map(comment -> commentView(comment, true))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    synchronized Map<String, Object> reviewComment(CommunityUser actor, String commentId, String status, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        failBeforeWrite(request);
        CommentRecord comment = requireComment(commentId);
        if (!Set.of("PENDING_REVIEW", "NEEDS_CHANGES").contains(comment.status)) throw new CommunityException(409, 49012, "comment state denied");
        comment.status = status;
        comment.reviewComment = string(body.getOrDefault("reviewComment", "审核完成"));
        comment.reviewedAt = NOW;
        comment.updatedAt = NOW;
        audit(actor, "COMMUNITY_COMMENT", comment.commentId, "COMMUNITY_COMMENT_" + status, "MEDIUM", null, status, "review comment");
        Map<String, Object> value = commentView(comment, true);
        persist(request, actor, "community.comment.review", "COMMUNITY_COMMENT_" + status, "COMMUNITY_COMMENT", comment.commentId, "MEDIUM", null, status, "review comment", withSnapshotType("COMMENT", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> offlineComment(CommunityUser actor, String commentId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        validateRequiredString(body, "publicReason", 1, 200);
        failBeforeWrite(request);
        CommentRecord comment = requireComment(commentId);
        comment.status = "OFFLINE";
        comment.updatedAt = NOW;
        audit(actor, "COMMUNITY_COMMENT", comment.commentId, "COMMUNITY_COMMENT_OFFLINE", "MEDIUM", null, comment.status, "offline comment");
        Map<String, Object> value = commentView(comment, true);
        persist(request, actor, "community.comment.offline", "COMMUNITY_COMMENT_OFFLINE", "COMMUNITY_COMMENT", comment.commentId, "MEDIUM", null, comment.status, "offline comment", withSnapshotType("COMMENT", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> setPostLike(CommunityUser actor, String postId, boolean enabled, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        failReaction(request);
        PostRecord post = requireVisiblePost(postId);
        String key = actor.userId() + ":POST:" + postId;
        if (enabled) postLikes.add(key); else postLikes.remove(key);
        post.likeCount = countPrefix(postLikes, ":POST:" + postId);
        audit(actor, "COMMUNITY_POST", post.postId, enabled ? "COMMUNITY_POST_LIKED" : "COMMUNITY_POST_UNLIKED", "LOW", null, post.status, "post reaction");
        Map<String, Object> value = linkedMap("postId", postId, "likeCount", post.likeCount);
        persist(request, actor, "community.reaction.post", enabled ? "COMMUNITY_POST_LIKED" : "COMMUNITY_POST_UNLIKED", "COMMUNITY_POST", post.postId, "LOW", null, post.status, "post reaction", reactionSnapshot("POST", postId, actor, enabled), body, value, 200);
        persist(request, actor, null, "COMMUNITY_POST_REACTION_COUNTED", "COMMUNITY_POST", post.postId, "LOW", null, post.status, "post reaction count", withSnapshotType("POST", postView(post, true)), Map.of(), value, 200);
        return value;
    }

    synchronized Map<String, Object> setCommentLike(CommunityUser actor, String commentId, boolean enabled, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        failReaction(request);
        CommentRecord comment = requireComment(commentId);
        if (!"APPROVED".equals(comment.status)) throw new CommunityException(404, 49002, "comment not visible");
        String key = actor.userId() + ":COMMENT:" + commentId;
        if (enabled) commentLikes.add(key); else commentLikes.remove(key);
        comment.likeCount = countPrefix(commentLikes, ":COMMENT:" + commentId);
        audit(actor, "COMMUNITY_COMMENT", comment.commentId, enabled ? "COMMUNITY_COMMENT_LIKED" : "COMMUNITY_COMMENT_UNLIKED", "LOW", null, comment.status, "comment reaction");
        Map<String, Object> value = linkedMap("commentId", commentId, "likeCount", comment.likeCount);
        persist(request, actor, "community.reaction.comment", enabled ? "COMMUNITY_COMMENT_LIKED" : "COMMUNITY_COMMENT_UNLIKED", "COMMUNITY_COMMENT", comment.commentId, "LOW", null, comment.status, "comment reaction", reactionSnapshot("COMMENT", commentId, actor, enabled), body, value, 200);
        persist(request, actor, null, "COMMUNITY_COMMENT_REACTION_COUNTED", "COMMUNITY_COMMENT", comment.commentId, "LOW", null, comment.status, "comment reaction count", withSnapshotType("COMMENT", commentView(comment, true)), Map.of(), value, 200);
        return value;
    }

    synchronized Map<String, Object> setFavorite(CommunityUser actor, String postId, boolean enabled, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        PostRecord post = requireVisiblePost(postId);
        String key = actor.userId() + ":POST:" + postId;
        if (enabled) favorites.add(key); else favorites.remove(key);
        post.favoriteCount = countPrefix(favorites, ":POST:" + postId);
        audit(actor, "COMMUNITY_POST", post.postId, enabled ? "COMMUNITY_POST_FAVORITED" : "COMMUNITY_POST_UNFAVORITED", "LOW", null, post.status, "favorite post");
        Map<String, Object> value = linkedMap("postId", postId, "favoriteCount", post.favoriteCount);
        persist(request, actor, "community.favorite.post", enabled ? "COMMUNITY_POST_FAVORITED" : "COMMUNITY_POST_UNFAVORITED", "COMMUNITY_POST", post.postId, "LOW", null, post.status, "favorite post", favoriteSnapshot(postId, actor, enabled), body, value, 200);
        persist(request, actor, null, "COMMUNITY_POST_FAVORITE_COUNTED", "COMMUNITY_POST", post.postId, "LOW", null, post.status, "favorite count", withSnapshotType("POST", postView(post, true)), Map.of(), value, 200);
        return value;
    }

    synchronized Map<String, Object> createPoll(CommunityUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        failBeforeWrite(request);
        PollRecord poll = new PollRecord();
        poll.pollId = "poll-" + (++idSeq);
        poll.postId = string(body.get("postId"));
        poll.title = validateRequiredString(body, "title", 2, 80);
        poll.description = optionalString(body, "description", 500);
        poll.status = "DRAFT";
        poll.multipleChoice = boolBody(body, "multipleChoice", false);
        poll.minChoices = intBody(body, "minChoices", 1);
        poll.maxChoices = intBody(body, "maxChoices", 1);
        poll.eligibleVisibility = enumBody(body, "eligibleVisibility", VISIBILITIES, "PUBLIC");
        poll.anonymousResult = boolBody(body, "anonymousResult", true);
        poll.opensAt = optionalInstant(body, "opensAt");
        poll.closesAt = optionalInstant(body, "closesAt");
        validatePollWindow(poll.opensAt, poll.closesAt);
        poll.options = pollOptions(body);
        poll.createdAt = NOW;
        poll.updatedAt = NOW;
        polls.put(poll.pollId, poll);
        audit(actor, "COMMUNITY_POLL", poll.pollId, "COMMUNITY_POLL_CREATED", "MEDIUM", null, poll.status, "create poll");
        Map<String, Object> value = pollView(poll);
        persist(request, actor, "community.poll.create", "COMMUNITY_POLL_CREATED", "COMMUNITY_POLL", poll.pollId, "MEDIUM", null, poll.status, "create poll", withSnapshotType("POLL", value), body, value, 201);
        return value;
    }

    synchronized Map<String, Object> updatePoll(CommunityUser actor, String pollId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        PollRecord poll = requirePoll(pollId);
        if (!"DRAFT".equals(poll.status)) throw new CommunityException(409, 49013, "poll state denied");
        if (body.containsKey("title")) poll.title = validateRequiredString(body, "title", 2, 80);
        if (body.containsKey("description")) poll.description = optionalString(body, "description", 500);
        poll.updatedAt = NOW;
        audit(actor, "COMMUNITY_POLL", poll.pollId, "COMMUNITY_POLL_UPDATED", "MEDIUM", null, poll.status, "update poll");
        Map<String, Object> value = pollView(poll);
        persist(request, actor, "community.poll.update", "COMMUNITY_POLL_UPDATED", "COMMUNITY_POLL", poll.pollId, "MEDIUM", null, poll.status, "update poll", withSnapshotType("POLL", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> pollStatus(CommunityUser actor, String pollId, String status, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        PollRecord poll = requirePoll(pollId);
        if ("OPEN".equals(status) && !"DRAFT".equals(poll.status)) throw new CommunityException(409, 49013, "poll state denied");
        if ("CLOSED".equals(status) && !"OPEN".equals(poll.status)) throw new CommunityException(409, 49013, "poll state denied");
        poll.status = status;
        poll.updatedAt = NOW;
        audit(actor, "COMMUNITY_POLL", poll.pollId, "COMMUNITY_POLL_" + status, "MEDIUM", null, poll.status, "poll status");
        Map<String, Object> value = pollView(poll);
        persist(request, actor, "community.poll.status", "COMMUNITY_POLL_" + status, "COMMUNITY_POLL", poll.pollId, "MEDIUM", null, poll.status, "poll status", withSnapshotType("POLL", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> vote(CommunityUser actor, String pollId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        PollRecord poll = requirePoll(pollId);
        if (!"OPEN".equals(poll.status)) throw new CommunityException(409, 49020, "poll not open");
        ensurePollEligible(actor, poll);
        ensurePollWithinWindow(poll);
        List<String> optionIds = looseStringList(body, "optionIds", poll.minChoices, poll.maxChoices, 100);
        Set<String> available = new LinkedHashSet<>();
        for (PollOption option : poll.options) available.add(option.optionId);
        if (!available.containsAll(optionIds)) throw new CommunityException(409, 49020, "invalid option");
        String voteKey = actor.userId() + ":" + pollId;
        if (!pollVotes.add(voteKey)) throw new CommunityException(409, 49020, "already voted");
        for (PollOption option : poll.options) {
            if (optionIds.contains(option.optionId)) option.voteCount++;
        }
        poll.voteCount++;
        audit(actor, "COMMUNITY_POLL", poll.pollId, "COMMUNITY_POLL_VOTED", "LOW", null, poll.status, "vote");
        Map<String, Object> value = pollView(poll);
        persist(request, actor, "community.poll.vote", "COMMUNITY_POLL_VOTED", "COMMUNITY_POLL", poll.pollId, "LOW", null, poll.status, "vote", pollVoteSnapshot(pollId, actor, optionIds), body, value, 200);
        persist(request, actor, null, "COMMUNITY_POLL_COUNTED", "COMMUNITY_POLL", poll.pollId, "LOW", null, poll.status, "poll count", withSnapshotType("POLL", value), Map.of(), value, 200);
        return value;
    }

    synchronized Map<String, Object> createReport(CommunityUser actor, String targetType, String targetId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        if ("POST".equals(targetType)) requireVisiblePost(targetId); else {
            CommentRecord comment = requireComment(targetId);
            if (!"APPROVED".equals(comment.status)) throw new CommunityException(404, 49002, "comment not visible");
        }
        String reasonType = enumBody(body, "reasonType", Set.of("SPAM", "HARASSMENT", "INAPPROPRIATE", "COPYRIGHT", "IMPERSONATION", "GAME_VIOLATION", "OTHER"), "OTHER");
        if (reports.values().stream().anyMatch(report -> report.reporterUserId().equals(actor.userId()) && report.targetType.equals(targetType) && report.targetId.equals(targetId) && report.reasonType.equals(reasonType) && Set.of("OPEN", "UNDER_REVIEW").contains(report.status))) {
            throw new CommunityException(409, 49021, "duplicate report");
        }
        ReportRecord report = new ReportRecord();
        report.reportId = "report-" + (++idSeq);
        report.targetType = targetType;
        report.targetId = targetId;
        report.reasonType = reasonType;
        report.description = validateRequiredString(body, "description", 5, 2000);
        report.evidenceLinks = evidenceLinks(body);
        report.status = "OPEN";
        report.reporter = author(actor, false);
        report.createdAt = NOW;
        report.updatedAt = NOW;
        reports.put(report.reportId, report);
        audit(actor, "COMMUNITY_REPORT", report.reportId, "COMMUNITY_REPORT_CREATED", "LOW", null, report.status, "create report");
        Map<String, Object> value = reportView(report, false);
        persist(request, actor, "community.report.create", "COMMUNITY_REPORT_CREATED", "COMMUNITY_REPORT", report.reportId, "LOW", null, report.status, "create report", withSnapshotType("REPORT", reportView(report, true)), body, value, 201);
        return value;
    }

    Map<String, Object> myReports(CommunityUser actor, Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", REPORT_STATUSES);
        List<Map<String, Object>> rows = reports.values().stream()
                .filter(report -> report.reporterUserId().equals(actor.userId()))
                .filter(report -> status == null || status.equals(report.status))
                .sorted(Comparator.comparing((ReportRecord report) -> report.createdAt).reversed())
                .map(report -> reportView(report, false))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> adminReports(Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", REPORT_STATUSES);
        List<Map<String, Object>> rows = reports.values().stream()
                .filter(report -> status == null || status.equals(report.status))
                .sorted(Comparator.comparing((ReportRecord report) -> report.createdAt).reversed())
                .map(report -> reportView(report, true))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> adminReport(String reportId) {
        return reportView(requireReport(reportId), true);
    }

    synchronized Map<String, Object> assignReport(CommunityUser actor, String reportId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 200);
        ReportRecord report = requireReport(reportId);
        if (!Set.of("OPEN", "UNDER_REVIEW").contains(report.status)) throw new CommunityException(409, 49014, "report state denied");
        String assignee = string(body.get("assigneeUserId"));
        if (assignee == null) assignee = actor.userId();
        if (actor.roles().contains("HELPER") && !assignee.equals(actor.userId())) throw new CommunityException(403, 42001, "helper assignment denied");
        report.assigneeUserId = assignee;
        report.status = "UNDER_REVIEW";
        report.updatedAt = NOW;
        audit(actor, "COMMUNITY_REPORT", report.reportId, "COMMUNITY_REPORT_ASSIGNED", "MEDIUM", null, report.status, "assign report");
        Map<String, Object> value = reportView(report, true);
        persist(request, actor, "community.report.assign", "COMMUNITY_REPORT_ASSIGNED", "COMMUNITY_REPORT", report.reportId, "MEDIUM", null, report.status, "assign report", withSnapshotType("REPORT", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> finishReport(CommunityUser actor, String reportId, String status, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        failBeforeWrite(request);
        ReportRecord report = requireReport(reportId);
        if (!Set.of("OPEN", "UNDER_REVIEW").contains(report.status)) throw new CommunityException(409, 49014, "report state denied");
        report.status = status;
        report.resolution = validateRequiredString(body, "resolution", 1, 1000);
        String linkedPenaltyId = string(body.get("linkedPenaltyId"));
        if (linkedPenaltyId != null) {
            requirePenalty(linkedPenaltyId);
            report.linkedPenaltyId = linkedPenaltyId;
        }
        report.updatedAt = NOW;
        report.resolvedAt = NOW;
        audit(actor, "COMMUNITY_REPORT", report.reportId, "COMMUNITY_REPORT_" + status, "MEDIUM", null, report.status, "finish report");
        Map<String, Object> value = reportView(report, true);
        persist(request, actor, "community.report.finish", "COMMUNITY_REPORT_" + status, "COMMUNITY_REPORT", report.reportId, "MEDIUM", null, report.status, "finish report", withSnapshotType("REPORT", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> createTicket(CommunityUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        String type = enumBody(body, "type", TICKET_TYPES, "OTHER");
        if ("PENDING_PROFILE".equals(actor.status()) && !Set.of("ACCOUNT_ISSUE", "WHITELIST_ISSUE", "RESOURCE_ISSUE").contains(type)) {
            throw new CommunityException(409, 49022, "pending profile ticket type denied");
        }
        TicketRecord ticket = new TicketRecord();
        ticket.ticketId = "ticket-" + (++idSeq);
        ticket.type = type;
        ticket.title = validateRequiredString(body, "title", 2, 80);
        ticket.status = "WAITING_STAFF";
        ticket.priority = "NORMAL";
        ticket.creator = author(actor, false);
        ticket.relatedObject = safeRelatedObject(body.get("relatedObject"));
        ticket.createdAt = NOW;
        ticket.updatedAt = NOW;
        ticket.messages.add(ticketMessage(ticket.ticketId, "USER_REPLY", validateRequiredString(body, "body", 1, 10000), actor, attachments(body)));
        tickets.put(ticket.ticketId, ticket);
        audit(actor, "COMMUNITY_TICKET", ticket.ticketId, "COMMUNITY_TICKET_CREATED", "LOW", null, ticket.status, "create ticket");
        Map<String, Object> value = ticketView(ticket, false);
        persist(request, actor, "community.ticket.create", "COMMUNITY_TICKET_CREATED", "COMMUNITY_TICKET", ticket.ticketId, "LOW", null, ticket.status, "create ticket", withSnapshotType("TICKET", value), body, value, 201);
        return value;
    }

    Map<String, Object> myTickets(CommunityUser actor, Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", TICKET_STATUSES);
        List<Map<String, Object>> rows = tickets.values().stream()
                .filter(ticket -> ticket.creatorUserId().equals(actor.userId()))
                .filter(ticket -> status == null || status.equals(ticket.status))
                .sorted(Comparator.comparing((TicketRecord ticket) -> ticket.createdAt).reversed())
                .map(ticket -> ticketView(ticket, false))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> myTicket(CommunityUser actor, String ticketId) {
        TicketRecord ticket = requireTicket(ticketId);
        requireOwner(actor, ticket.creatorUserId(), 49005);
        return ticketView(ticket, false);
    }

    synchronized Map<String, Object> appendTicket(CommunityUser actor, String ticketId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        TicketRecord ticket = requireTicket(ticketId);
        requireOwner(actor, ticket.creatorUserId(), 49005);
        if (!Set.of("OPEN", "WAITING_USER", "WAITING_STAFF").contains(ticket.status)) throw new CommunityException(409, 49015, "ticket state denied");
        ticket.messages.add(ticketMessage(ticket.ticketId, "USER_REPLY", validateRequiredString(body, "body", 1, 10000), actor, attachments(body)));
        ticket.status = "WAITING_STAFF";
        ticket.updatedAt = NOW;
        ticket.lastReplyAt = NOW;
        audit(actor, "COMMUNITY_TICKET", ticket.ticketId, "COMMUNITY_TICKET_USER_REPLIED", "LOW", null, ticket.status, "append ticket");
        Map<String, Object> value = ticketView(ticket, false);
        persist(request, actor, "community.ticket.user-reply", "COMMUNITY_TICKET_USER_REPLIED", "COMMUNITY_TICKET", ticket.ticketId, "LOW", null, ticket.status, "append ticket", withSnapshotType("TICKET", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> closeTicket(CommunityUser actor, String ticketId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 200);
        TicketRecord ticket = requireTicket(ticketId);
        requireOwner(actor, ticket.creatorUserId(), 49005);
        ticket.status = "CLOSED";
        ticket.closedAt = NOW;
        ticket.updatedAt = NOW;
        audit(actor, "COMMUNITY_TICKET", ticket.ticketId, "COMMUNITY_TICKET_CLOSED", "LOW", null, ticket.status, "close ticket");
        Map<String, Object> value = ticketView(ticket, false);
        persist(request, actor, "community.ticket.close", "COMMUNITY_TICKET_CLOSED", "COMMUNITY_TICKET", ticket.ticketId, "LOW", null, ticket.status, "close ticket", withSnapshotType("TICKET", value), body, value, 200);
        return value;
    }

    Map<String, Object> adminTickets(Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        String status = enumQuery(query, "status", TICKET_STATUSES);
        List<Map<String, Object>> rows = tickets.values().stream()
                .filter(ticket -> status == null || status.equals(ticket.status))
                .sorted(Comparator.comparing((TicketRecord ticket) -> ticket.createdAt).reversed())
                .map(ticket -> ticketView(ticket, true))
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> adminTicket(String ticketId) {
        return ticketView(requireTicket(ticketId), true);
    }

    synchronized Map<String, Object> assignTicket(CommunityUser actor, String ticketId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 200);
        TicketRecord ticket = requireTicket(ticketId);
        String assignee = string(body.get("assigneeUserId"));
        if (assignee == null) assignee = actor.userId();
        if (actor.roles().contains("HELPER") && !assignee.equals(actor.userId())) throw new CommunityException(403, 42001, "helper assignment denied");
        ticket.assigneeUserId = assignee;
        ticket.status = "WAITING_STAFF";
        ticket.updatedAt = NOW;
        audit(actor, "COMMUNITY_TICKET", ticket.ticketId, "COMMUNITY_TICKET_ASSIGNED", "MEDIUM", null, ticket.status, "assign ticket");
        Map<String, Object> value = ticketView(ticket, true);
        persist(request, actor, "community.ticket.assign", "COMMUNITY_TICKET_ASSIGNED", "COMMUNITY_TICKET", ticket.ticketId, "MEDIUM", null, ticket.status, "assign ticket", withSnapshotType("TICKET", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> replyTicket(CommunityUser actor, String ticketId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        failBeforeWrite(request);
        TicketRecord ticket = requireTicket(ticketId);
        String messageType = enumBody(body, "messageType", Set.of("USER_REPLY", "STAFF_REPLY", "INTERNAL_NOTE", "SYSTEM_EVENT"), "STAFF_REPLY");
        if ("INTERNAL_NOTE".equals(messageType) && actor.roles().contains("HELPER")) throw new CommunityException(403, 42001, "internal note denied");
        ticket.messages.add(ticketMessage(ticket.ticketId, messageType, validateRequiredString(body, "body", 1, 10000), actor, attachments(body)));
        ticket.status = "STAFF_REPLY".equals(messageType) ? "WAITING_USER" : ticket.status;
        ticket.updatedAt = NOW;
        ticket.lastReplyAt = NOW;
        audit(actor, "COMMUNITY_TICKET", ticket.ticketId, "COMMUNITY_TICKET_STAFF_REPLIED", "MEDIUM", null, ticket.status, "reply ticket");
        Map<String, Object> value = ticketView(ticket, true);
        persist(request, actor, "community.ticket.staff-reply", "COMMUNITY_TICKET_STAFF_REPLIED", "COMMUNITY_TICKET", ticket.ticketId, "MEDIUM", null, ticket.status, "reply ticket", withSnapshotType("TICKET", value), body, value, 201);
        return value;
    }

    synchronized Map<String, Object> ticketStatus(CommunityUser actor, String ticketId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        TicketRecord ticket = requireTicket(ticketId);
        String status = enumBody(body, "status", TICKET_STATUSES, null);
        if (!ticketTransitionAllowed(actor, ticket.status, status)) throw new CommunityException(409, 49015, "ticket state denied");
        ticket.status = status;
        if ("RESOLVED".equals(status)) ticket.resolvedAt = NOW;
        if ("CLOSED".equals(status)) ticket.closedAt = NOW;
        ticket.updatedAt = NOW;
        audit(actor, "COMMUNITY_TICKET", ticket.ticketId, "COMMUNITY_TICKET_STATUS_CHANGED", "MEDIUM", null, ticket.status, "ticket status");
        Map<String, Object> value = ticketView(ticket, true);
        persist(request, actor, "community.ticket.status", "COMMUNITY_TICKET_STATUS_CHANGED", "COMMUNITY_TICKET", ticket.ticketId, "MEDIUM", null, ticket.status, "ticket status", withSnapshotType("TICKET", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> createPenalty(CommunityUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        validateRequiredString(body, "publicReason", 1, 200);
        if (!"CREATE_COMMUNITY_PENALTY".equals(string(body.get("confirmText")))) throw new CommunityException(403, 42003, "missing confirmation");
        failBeforeWrite(request);
        PenaltyRecord penalty = new PenaltyRecord();
        penalty.penaltyId = "penalty-" + (++idSeq);
        penalty.targetUserId = validateRequiredString(body, "targetUserId", 1, 80);
        penalty.targetMemberId = "member-" + penalty.targetUserId;
        penalty.type = enumBody(body, "type", PENALTY_TYPES, "WARNING");
        penalty.status = "ACTIVE";
        penalty.publicReason = string(body.get("publicReason"));
        penalty.reason = string(body.get("reason"));
        penalty.startsAt = NOW;
        penalty.expiresAt = string(body.get("expiresAt"));
        penalty.createdBy = actor.userId();
        penalty.createdAt = NOW;
        penalty.updatedAt = NOW;
        penalties.put(penalty.penaltyId, penalty);
        audit(actor, "COMMUNITY_PENALTY", penalty.penaltyId, "COMMUNITY_PENALTY_CREATED", "HIGH", null, penalty.status, "create penalty");
        Map<String, Object> value = penaltyView(penalty);
        persist(request, actor, "community.penalty.create", "COMMUNITY_PENALTY_CREATED", "COMMUNITY_PENALTY", penalty.penaltyId, "HIGH", null, penalty.status, "create penalty", withSnapshotType("PENALTY", value), body, value, 201);
        return value;
    }

    synchronized Map<String, Object> updatePenalty(CommunityUser actor, String penaltyId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        PenaltyRecord penalty = requirePenalty(penaltyId);
        if (!"ACTIVE".equals(penalty.status)) throw new CommunityException(409, 49016, "penalty state denied");
        if (body.containsKey("publicReason")) penalty.publicReason = validateRequiredString(body, "publicReason", 1, 200);
        if (body.containsKey("expiresAt")) penalty.expiresAt = string(body.get("expiresAt"));
        penalty.updatedAt = NOW;
        audit(actor, "COMMUNITY_PENALTY", penalty.penaltyId, "COMMUNITY_PENALTY_UPDATED", "HIGH", null, penalty.status, "update penalty");
        Map<String, Object> value = penaltyView(penalty);
        persist(request, actor, "community.penalty.update", "COMMUNITY_PENALTY_UPDATED", "COMMUNITY_PENALTY", penalty.penaltyId, "HIGH", null, penalty.status, "update penalty", withSnapshotType("PENALTY", value), body, value, 200);
        return value;
    }

    synchronized Map<String, Object> revokePenalty(CommunityUser actor, String penaltyId, Map<String, Object> body, HttpServletRequest request) {
        validateIdempotencyKey(body);
        validateReason(body, 500);
        validateRequiredString(body, "publicReason", 1, 200);
        if (!"REVOKE_COMMUNITY_PENALTY".equals(string(body.get("confirmText")))) throw new CommunityException(403, 42003, "missing confirmation");
        failBeforeWrite(request);
        PenaltyRecord penalty = requirePenalty(penaltyId);
        if (!Set.of("ACTIVE", "EXPIRED").contains(penalty.status)) throw new CommunityException(409, 49016, "penalty state denied");
        penalty.status = "REVOKED";
        penalty.revokedBy = actor.userId();
        penalty.revokedAt = NOW;
        penalty.revokeReason = string(body.get("publicReason"));
        penalty.updatedAt = NOW;
        audit(actor, "COMMUNITY_PENALTY", penalty.penaltyId, "COMMUNITY_PENALTY_REVOKED", "HIGH", null, penalty.status, "revoke penalty");
        Map<String, Object> value = penaltyView(penalty);
        persist(request, actor, "community.penalty.revoke", "COMMUNITY_PENALTY_REVOKED", "COMMUNITY_PENALTY", penalty.penaltyId, "HIGH", null, penalty.status, "revoke penalty", withSnapshotType("PENALTY", value), body, value, 200);
        return value;
    }

    Map<String, Object> auditLogs(Map<String, String> query) {
        int page = page(query);
        int pageSize = pageSize(query);
        sort(query, Set.of("createdAt_desc", "createdAt_asc"), "createdAt_desc");
        validateTimeRange(query);
        String result = enumQuery(query, "result", Set.of("SUCCESS", "FAILED"));
        String action = query.get("action");
        List<Map<String, Object>> rows = audits.stream()
                .filter(audit -> result == null || result.equals(audit.get("result")))
                .filter(audit -> action == null || action.equals(audit.get("action")))
                .sorted(Comparator.comparing((Map<String, Object> audit) -> Objects.toString(audit.get("createdAt"))).reversed())
                .toList();
        return pageRows(rows, page, pageSize);
    }

    Map<String, Object> opsSummary(CommunityUser actor) {
        Map<String, Object> summary = linkedMap(
                "service", "community",
                "port", 8132,
                "legacyPort", 8112,
                "storageMode", "IN_MEMORY",
                "authMode", actor.authMode(),
                "actorUserId", actor.userId(),
                "profileMode", "TEST_STUB",
                "notificationMode", "TEST_STUB",
                "contentMode", "TEST_STUB",
                "resourceMode", "TEST_STUB",
                "attendanceMode", "SKIPPED",
                "testControlsEnabled", testControls.enabled(),
                "boardsTotal", boards.size(),
                "postsTotal", posts.size(),
                "pendingReviewPostsTotal", posts.values().stream().filter(post -> "PENDING_REVIEW".equals(post.status)).count(),
                "commentsTotal", comments.size(),
                "openReportsTotal", reports.values().stream().filter(report -> "OPEN".equals(report.status)).count(),
                "openTicketsTotal", tickets.values().stream().filter(ticket -> Set.of("OPEN", "WAITING_STAFF", "WAITING_USER").contains(ticket.status)).count(),
                "activePenaltiesTotal", penalties.values().stream().filter(penalty -> "ACTIVE".equals(penalty.status)).count(),
                "pollsOpenTotal", polls.values().stream().filter(poll -> "OPEN".equals(poll.status)).count(),
                "auditsTotal", audits.size(),
                "idempotencyRecordsTotal", idempotency.size(),
                "lastAuditAt", audits.isEmpty() ? null : audits.get(audits.size() - 1).get("createdAt"),
                "productionGaps", List.of("P1_IN_MEMORY_STORAGE", "P1_AUTH_STUB", "P1_PROFILE_STUB", "P1_NOTIFICATION_STUB", "P1_CONTENT_STUB", "P1_RESOURCE_STUB", "ATTENDANCE_CONTRIBUTION_NOT_CONNECTED", "TEST_CONTROLS_DISABLED_OUTSIDE_TEST")
        );
        summary.putAll(persistence.counts());
        return summary;
    }

    private List<Map<String, Object>> publicPostRows(Map<String, String> query, int page, int pageSize) {
        String boardId = query.get("boardId");
        String type = enumQuery(query, "type", POST_TYPES);
        String keyword = lower(query.get("keyword"));
        return posts.values().stream()
                .filter(post -> "APPROVED".equals(post.status))
                .filter(post -> boardId == null || boardId.equals(post.boardId))
                .filter(post -> type == null || type.equals(post.type))
                .filter(post -> keyword == null || post.title.toLowerCase().contains(keyword) || post.body.toLowerCase().contains(keyword))
                .sorted(Comparator.comparing((PostRecord post) -> post.createdAt).reversed())
                .map(post -> postView(post, false))
                .toList();
    }

    private BoardRecord requireBoard(String boardId) {
        BoardRecord board = boards.get(boardId);
        if (board == null) throw new CommunityException(404, 49000, "board not found");
        return board;
    }

    private BoardRecord requirePublicBoard(String boardId) {
        BoardRecord board = requireBoard(boardId);
        if (!"ACTIVE".equals(board.status) || !"PUBLIC".equals(board.visibility)) throw new CommunityException(404, 49000, "board not found");
        return board;
    }

    private PostRecord requirePost(String postId) {
        PostRecord post = posts.get(postId);
        if (post == null) throw new CommunityException(404, 49001, "post not found");
        return post;
    }

    private PostRecord requireVisiblePost(String postId) {
        PostRecord post = requirePost(postId);
        if (!"APPROVED".equals(post.status)) throw new CommunityException(404, 49001, "post not visible");
        return post;
    }

    private CommentRecord requireComment(String commentId) {
        CommentRecord comment = comments.get(commentId);
        if (comment == null) throw new CommunityException(404, 49002, "comment not found");
        return comment;
    }

    private PollRecord requirePoll(String pollId) {
        PollRecord poll = polls.get(pollId);
        if (poll == null) throw new CommunityException(404, 49003, "poll not found");
        return poll;
    }

    private ReportRecord requireReport(String reportId) {
        ReportRecord report = reports.get(reportId);
        if (report == null) throw new CommunityException(404, 49004, "report not found");
        return report;
    }

    private TicketRecord requireTicket(String ticketId) {
        TicketRecord ticket = tickets.get(ticketId);
        if (ticket == null) throw new CommunityException(404, 49005, "ticket not found");
        return ticket;
    }

    private PenaltyRecord requirePenalty(String penaltyId) {
        PenaltyRecord penalty = penalties.get(penaltyId);
        if (penalty == null) throw new CommunityException(404, 49006, "penalty not found");
        return penalty;
    }

    private void ensureCanWritePost(CommunityUser actor) {
        if ("PENDING_PROFILE".equals(actor.status())) throw new CommunityException(409, 49022, "pending profile cannot post");
        if (hasActivePenalty(actor.userId(), Set.of("BAN", "POST_RESTRICTED"))) throw new CommunityException(409, 49022, "post restricted");
    }

    private void ensureCanComment(CommunityUser actor) {
        if (hasActivePenalty(actor.userId(), Set.of("BAN", "MUTE"))) throw new CommunityException(409, 49022, "comment restricted");
    }

    private boolean hasActivePenalty(String userId, Set<String> types) {
        return penalties.values().stream().anyMatch(penalty -> userId.equals(penalty.targetUserId) && "ACTIVE".equals(penalty.status) && types.contains(penalty.type));
    }

    private String accessFingerprint(HttpServletRequest request) {
        if (request == null) return "unknown";
        String forwarded = string(request.getHeader("X-Forwarded-For"));
        String ip = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        String userAgent = string(request.getHeader("User-Agent"));
        return Objects.toString(ip, "unknown") + ":" + Objects.toString(userAgent, "unknown");
    }

    private void ensurePollEligible(CommunityUser actor, PollRecord poll) {
        if ("STAFF_ONLY".equals(poll.eligibleVisibility) && !isStaff(actor)) throw new CommunityException(403, 42001, "poll staff only");
        if ("PENDING_PROFILE".equals(actor.status())) throw new CommunityException(409, 49020, "poll eligibility denied");
    }

    private boolean isStaff(CommunityUser actor) {
        return actor.roles().stream().anyMatch(Set.of("HELPER", "ADMIN", "OWNER")::contains);
    }

    private void ensurePollWithinWindow(PollRecord poll) {
        Instant now = Instant.parse(NOW);
        if (poll.opensAt != null && now.isBefore(Instant.parse(poll.opensAt))) throw new CommunityException(409, 49020, "poll not opened");
        if (poll.closesAt != null && !now.isBefore(Instant.parse(poll.closesAt))) throw new CommunityException(409, 49020, "poll closed");
    }

    private void validatePollWindow(String opensAt, String closesAt) {
        if (opensAt != null && closesAt != null && !Instant.parse(closesAt).isAfter(Instant.parse(opensAt))) {
            throw new CommunityException(400, 40001, "invalid poll window");
        }
    }

    private String optionalInstant(Map<String, Object> body, String field) {
        String value = string(body.get(field));
        if (value == null || value.isBlank()) return null;
        try {
            Instant.parse(value);
            return value;
        } catch (DateTimeParseException exception) {
            throw new CommunityException(400, 40001, "invalid " + field);
        }
    }

    private List<String> evidenceLinks(Map<String, Object> body) {
        List<String> links = looseStringList(body, "evidenceLinks", 0, 10, 500);
        for (String link : links) {
            if (!safeLink(link)) throw new CommunityException(400, 40001, "invalid evidence link");
        }
        return links;
    }

    private List<Map<String, Object>> attachments(Map<String, Object> body) {
        Object raw = body.get("attachments");
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> list) || list.size() > 5) throw new CommunityException(400, 40001, "invalid attachments");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) throw new CommunityException(400, 40001, "invalid attachment");
            String attachmentId = requiredObjectString(map, "attachmentId", 1, 80);
            String name = requiredObjectString(map, "name", 1, 120);
            String url = requiredObjectString(map, "url", 1, 500);
            if (!safeInternalLink(url)) throw new CommunityException(400, 40001, "invalid attachment url");
            result.add(linkedMap("attachmentId", attachmentId, "name", name, "url", url));
        }
        return result;
    }

    private Map<String, Object> safeRelatedObject(Object raw) {
        if (raw == null) return null;
        if (!(raw instanceof Map<?, ?> map)) throw new CommunityException(400, 40001, "invalid related object");
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of("type", "id", "targetId", "title", "summary", "sourceModule")) {
            Object value = map.get(key);
            if (value != null) result.put(key, Objects.toString(value));
        }
        return result.isEmpty() ? null : result;
    }

    private String requiredObjectString(Map<?, ?> map, String field, int min, int max) {
        Object raw = map.get(field);
        String value = raw == null ? null : raw.toString();
        if (value == null || value.isBlank() || value.length() < min || value.length() > max) {
            throw new CommunityException(400, 40001, "invalid " + field);
        }
        return value;
    }

    private boolean safeLink(String link) {
        return link != null && !link.isBlank() && (link.startsWith("http://") || link.startsWith("https://") || safeInternalLink(link));
    }

    private boolean safeInternalLink(String link) {
        return link != null && link.startsWith("/") && !link.startsWith("//") && !link.contains("\\");
    }

    private boolean ticketTransitionAllowed(CommunityUser actor, String current, String target) {
        if ("ARCHIVED".equals(target)) return "CLOSED".equals(current) && actor.roles().stream().anyMatch(Set.of("ADMIN", "OWNER")::contains);
        if ("ARCHIVED".equals(current) || "CLOSED".equals(current)) return false;
        if ("RESOLVED".equals(current)) return "CLOSED".equals(target);
        return Set.of("WAITING_STAFF", "WAITING_USER", "RESOLVED", "CLOSED").contains(target);
    }

    private Map<String, Object> boardView(BoardRecord board) {
        long approvedPosts = posts.values().stream().filter(post -> board.boardId.equals(post.boardId) && "APPROVED".equals(post.status)).count();
        return linkedMap(
                "boardId", board.boardId,
                "slug", board.slug,
                "name", board.name,
                "description", board.description,
                "visibility", board.visibility,
                "status", board.status,
                "allowedPostTypes", board.allowedPostTypes,
                "tags", board.tags,
                "sortOrder", board.sortOrder,
                "postCount", approvedPosts,
                "lastPostAt", board.lastPostAt,
                "createdAt", board.createdAt,
                "updatedAt", board.updatedAt,
                "archivedAt", board.archivedAt
        );
    }

    private Map<String, Object> postView(PostRecord post, boolean admin) {
        Map<String, Object> view = linkedMap(
                "postId", post.postId,
                "boardId", post.boardId,
                "type", post.type,
                "title", post.title,
                "summary", post.summary,
                "body", post.body,
                "tags", post.tags,
                "status", post.status,
                "author", post.author,
                "linkedContentSnapshot", post.linkedContentSnapshot,
                "linkedResourceSnapshot", post.linkedResourceSnapshot,
                "pollId", post.pollId,
                "commentCount", comments.values().stream().filter(comment -> post.postId.equals(comment.postId) && "APPROVED".equals(comment.status)).count(),
                "likeCount", post.likeCount,
                "favoriteCount", post.favoriteCount,
                "viewCount", post.viewCount,
                "acceptedCommentId", post.acceptedCommentId,
                "lastCommentAt", post.lastCommentAt,
                "submittedAt", post.submittedAt,
                "reviewedAt", post.reviewedAt,
                "reviewComment", post.reviewComment,
                "notificationStatus", post.notificationStatus,
                "createdAt", post.createdAt,
                "updatedAt", post.updatedAt,
                "offlineAt", post.offlineAt,
                "archivedAt", post.archivedAt,
                "deletedAt", post.deletedAt
        );
        if (admin) {
            view.put("reviewerUserId", post.reviewerUserId);
            view.put("notificationFailure", post.notificationFailure);
        }
        return view;
    }

    private Map<String, Object> commentView(CommentRecord comment, boolean admin) {
        Map<String, Object> view = linkedMap(
                "commentId", comment.commentId,
                "postId", comment.postId,
                "parentCommentId", comment.parentCommentId,
                "body", comment.body,
                "status", comment.status,
                "author", comment.author,
                "likeCount", comment.likeCount,
                "isAcceptedAnswer", comment.isAcceptedAnswer,
                "submittedAt", comment.submittedAt,
                "reviewedAt", comment.reviewedAt,
                "reviewComment", comment.reviewComment,
                "createdAt", comment.createdAt,
                "updatedAt", comment.updatedAt,
                "deletedAt", comment.deletedAt
        );
        return view;
    }

    private Map<String, Object> pollView(PollRecord poll) {
        List<Map<String, Object>> options = poll.options.stream()
                .map(option -> linkedMap("optionId", option.optionId, "label", option.label, "description", option.description, "voteCount", option.voteCount))
                .toList();
        return linkedMap(
                "pollId", poll.pollId,
                "postId", poll.postId,
                "title", poll.title,
                "description", poll.description,
                "status", poll.status,
                "options", options,
                "multipleChoice", poll.multipleChoice,
                "minChoices", poll.minChoices,
                "maxChoices", poll.maxChoices,
                "eligibleVisibility", poll.eligibleVisibility,
                "anonymousResult", poll.anonymousResult,
                "voteCount", poll.voteCount,
                "opensAt", poll.opensAt,
                "closesAt", poll.closesAt,
                "createdAt", poll.createdAt,
                "updatedAt", poll.updatedAt
        );
    }

    private Map<String, Object> reportView(ReportRecord report, boolean admin) {
        Map<String, Object> view = linkedMap(
                "reportId", report.reportId,
                "targetType", report.targetType,
                "targetId", report.targetId,
                "reasonType", report.reasonType,
                "description", report.description,
                "evidenceLinks", report.evidenceLinks,
                "status", report.status,
                "reporter", admin ? report.reporter : null,
                "resolution", report.resolution,
                "notificationStatus", report.notificationStatus,
                "createdAt", report.createdAt,
                "updatedAt", report.updatedAt,
                "resolvedAt", report.resolvedAt
        );
        if (admin) {
            view.put("assigneeUserId", report.assigneeUserId);
            view.put("linkedPenaltyId", report.linkedPenaltyId);
        }
        return view;
    }

    private Map<String, Object> ticketView(TicketRecord ticket, boolean admin) {
        List<Map<String, Object>> visibleMessages = ticket.messages.stream()
                .filter(message -> admin || !"INTERNAL_NOTE".equals(message.messageType))
                .map(message -> linkedMap(
                        "messageId", message.messageId,
                        "ticketId", message.ticketId,
                        "messageType", message.messageType,
                        "body", message.body,
                        "author", message.author,
                        "attachments", message.attachments,
                        "createdAt", message.createdAt))
                .toList();
        Map<String, Object> view = linkedMap(
                "ticketId", ticket.ticketId,
                "type", ticket.type,
                "title", ticket.title,
                "status", ticket.status,
                "priority", ticket.priority,
                "creator", ticket.creator,
                "relatedObject", ticket.relatedObject,
                "messages", visibleMessages,
                "lastReplyAt", ticket.lastReplyAt,
                "resolvedAt", ticket.resolvedAt,
                "closedAt", ticket.closedAt,
                "createdAt", ticket.createdAt,
                "updatedAt", ticket.updatedAt
        );
        if (admin) view.put("assigneeUserId", ticket.assigneeUserId);
        return view;
    }

    private Map<String, Object> penaltyView(PenaltyRecord penalty) {
        return linkedMap(
                "penaltyId", penalty.penaltyId,
                "targetUserId", penalty.targetUserId,
                "targetMemberId", penalty.targetMemberId,
                "type", penalty.type,
                "status", penalty.status,
                "reason", penalty.reason,
                "publicReason", penalty.publicReason,
                "evidenceReportId", penalty.evidenceReportId,
                "relatedPostId", penalty.relatedPostId,
                "relatedCommentId", penalty.relatedCommentId,
                "startsAt", penalty.startsAt,
                "expiresAt", penalty.expiresAt,
                "createdBy", penalty.createdBy,
                "revokedBy", penalty.revokedBy,
                "revokedAt", penalty.revokedAt,
                "revokeReason", penalty.revokeReason,
                "createdAt", penalty.createdAt,
                "updatedAt", penalty.updatedAt
        );
    }

    private Map<String, Object> author(CommunityUser user, boolean stale) {
        return linkedMap(
                "userId", user.userId(),
                "memberId", "member-" + user.userId(),
                "displayNameSnapshot", user.displayName(),
                "avatarUrlSnapshot", null,
                "memberGroupSnapshot", user.roles().contains("HELPER") ? "staff" : "default",
                "memberStatusSnapshot", "PENDING_PROFILE".equals(user.status()) ? "PENDING_PROFILE" : "ACTIVE",
                "minecraftIdSnapshot", user.userId() + "_mc",
                "profileSnapshotStale", stale
        );
    }

    private TicketMessageRecord ticketMessage(String ticketId, String messageType, String body, CommunityUser actor, List<Map<String, Object>> attachments) {
        TicketMessageRecord message = new TicketMessageRecord();
        message.messageId = "ticket-message-" + (++idSeq);
        message.ticketId = ticketId;
        message.messageType = messageType;
        message.body = body;
        message.author = actor == null ? null : author(actor, false);
        message.attachments = attachments;
        message.createdAt = NOW;
        return message;
    }

    private List<PollOption> pollOptions(Map<String, Object> body) {
        Object raw = body.get("options");
        if (!(raw instanceof List<?> list) || list.size() < 2 || list.size() > 10) throw new CommunityException(400, 40001, "invalid options");
        List<PollOption> options = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) throw new CommunityException(400, 40001, "invalid option");
            PollOption option = new PollOption();
            option.optionId = "option-" + (++idSeq);
            option.label = Objects.toString(map.get("label"), "");
            if (option.label.isBlank() || option.label.length() > 80) throw new CommunityException(400, 40001, "invalid option label");
            option.description = string(map.get("description"));
            options.add(option);
        }
        return options;
    }

    private Map<String, Object> linkedContent(Map<String, Object> body) {
        String id = string(body.get("linkedContentId"));
        if (id == null) return null;
        return linkedMap("contentId", id, "title", "关联内容", "slug", "linked-content", "publicStatus", "APPROVED", "linkedContentSnapshotStale", false);
    }

    private Map<String, Object> linkedResource(Map<String, Object> body) {
        String id = string(body.get("linkedResourceId"));
        if (id == null) return null;
        return linkedMap("resourceId", id, "title", "关联资源", "slug", "linked-resource", "version", "1.0.0", "publicStatus", "APPROVED", "linkedResourceSnapshotStale", false);
    }

    private void failLinked(Map<String, Object> body, HttpServletRequest request) {
        if (body.containsKey("linkedContentId")) {
            switch (testHeader(request, "X-Test-Content-Mode")) {
                case "unavailable" -> throw new CommunityException(502, 49230, "content unavailable");
                case "timeout" -> throw new CommunityException(504, 49231, "content timeout");
                case "bad-schema" -> throw new CommunityException(502, 49232, "content incompatible");
                default -> {
                }
            }
        }
        if (body.containsKey("linkedResourceId")) {
            switch (testHeader(request, "X-Test-Resource-Mode")) {
                case "unavailable" -> throw new CommunityException(502, 49240, "resource unavailable");
                case "timeout" -> throw new CommunityException(504, 49241, "resource timeout");
                case "bad-schema" -> throw new CommunityException(502, 49242, "resource incompatible");
                default -> {
                }
            }
        }
    }

    private void failProfile(HttpServletRequest request) {
        switch (testHeader(request, "X-Test-Profile-Mode")) {
            case "unavailable" -> throw new CommunityException(502, 49210, "profile unavailable");
            case "timeout" -> throw new CommunityException(504, 49211, "profile timeout");
            case "bad-schema" -> throw new CommunityException(502, 49212, "profile incompatible");
            default -> {
            }
        }
    }

    private String notificationStatus(HttpServletRequest request) {
        return notificationFailure(request) == null ? "DELIVERED" : "FAILED";
    }

    private Map<String, Object> notificationFailure(HttpServletRequest request) {
        return switch (testHeader(request, "X-Test-Notification-Mode")) {
            case "unavailable" -> linkedMap("status", "FAILED", "failureCode", "49220", "failureType", "UNAVAILABLE", "failureReason", "notification unavailable", "failedAt", NOW);
            case "timeout" -> linkedMap("status", "FAILED", "failureCode", "49221", "failureType", "TIMEOUT", "failureReason", "notification timeout", "failedAt", NOW);
            case "bad-schema" -> linkedMap("status", "FAILED", "failureCode", "49222", "failureType", "BAD_SCHEMA", "failureReason", "notification response incompatible", "failedAt", NOW);
            default -> null;
        };
    }

    private void auditNotificationFailure(CommunityUser actor, String targetType, String targetId, Map<String, Object> failure) {
        if (failure != null) audit(actor, targetType, targetId, "COMMUNITY_NOTIFICATION_FAILED", "LOW", null, "FAILED", Objects.toString(failure.get("failureType")) + ":" + failure.get("failureCode"));
    }

    private void audit(CommunityUser actor, String targetType, String targetId, String action, String risk, String beforeState, String afterState, String reason) {
        audits.add(linkedMap(
                "id", "audit-" + (++idSeq),
                "requestId", CommunityController.requestId(),
                "actorUserId", actor == null ? null : actor.userId(),
                "actorRole", actor == null ? null : actor.roles().stream().findFirst().orElse("USER"),
                "actorPermissions", List.of(),
                "sourceIp", "127.0.0.1",
                "targetType", targetType,
                "targetId", targetId,
                "action", action,
                "riskLevel", risk,
                "reason", reason,
                "paramsSummary", "summary",
                "beforeState", beforeState,
                "afterState", afterState,
                "result", "SUCCESS",
                "failureReason", null,
                "createdAt", NOW
        ));
    }

    private void persist(HttpServletRequest request, CommunityUser actor, String scope, String action, String targetType, String targetId, String risk, String beforeState, String afterState, String reason, Map<String, Object> snapshot, Map<String, Object> body, Map<String, Object> responseBody, int responseCode) {
        persistence.persistWrite(request, actor.userId(), actor.roles().stream().findFirst().orElse("USER"), scope, action, targetType, targetId, risk,
                beforeState, afterState, reason, idempotencyKey(body), canonical(body), snapshot, responseBody, responseCode);
    }

    private Map<String, Object> withSnapshotType(String type, Map<String, Object> value) {
        Map<String, Object> snapshot = new LinkedHashMap<>(value);
        snapshot.put("snapshotType", type);
        return snapshot;
    }

    private Map<String, Object> reactionSnapshot(String targetType, String targetId, CommunityUser actor, boolean active) {
        return linkedMap("snapshotType", "REACTION", "targetType", targetType, "targetId", targetId, "actorUserId", actor.userId(), "active", active);
    }

    private Map<String, Object> favoriteSnapshot(String postId, CommunityUser actor, boolean active) {
        return linkedMap("snapshotType", "FAVORITE", "postId", postId, "actorUserId", actor.userId(), "active", active);
    }

    private Map<String, Object> pollVoteSnapshot(String pollId, CommunityUser actor, List<String> optionIds) {
        return linkedMap("snapshotType", "POLL_VOTE", "pollId", pollId, "actorUserId", actor.userId(), "optionIds", optionIds);
    }

    private void failBeforeWrite(HttpServletRequest request) {
        if ("true".equals(testHeader(request, "X-Test-Fail-Audit"))) throw new CommunityException(500, 54001, "community audit failed");
        if ("true".equals(testHeader(request, "X-Test-Fail-Store"))) throw new CommunityException(500, 54002, "community state failed");
    }

    private void failReaction(HttpServletRequest request) {
        if ("true".equals(testHeader(request, "X-Test-Fail-Reaction"))) throw new CommunityException(500, 54003, "community reaction failed");
    }

    private String testHeader(HttpServletRequest request, String name) {
        return testControls.enabled() && request != null ? Objects.toString(request.getHeader(name), "") : "";
    }

    private int page(Map<String, String> query) {
        return intQuery(query, "page", 1, 1, Integer.MAX_VALUE, 40002);
    }

    private int pageSize(Map<String, String> query) {
        return intQuery(query, "pageSize", 20, 1, 100, 40002);
    }

    private int intQuery(Map<String, String> query, String key, int fallback, int min, int max, int code) {
        if (!query.containsKey(key)) return fallback;
        try {
            int value = Integer.parseInt(query.get(key));
            if (value < min || value > max) throw new CommunityException(400, code, "invalid number");
            return value;
        } catch (NumberFormatException ex) {
            throw new CommunityException(400, code, "invalid number");
        }
    }

    private String sort(Map<String, String> query, Set<String> allowed, String fallback) {
        String value = query.getOrDefault("sort", fallback);
        if (!allowed.contains(value)) throw new CommunityException(400, 40003, "invalid sort");
        return value;
    }

    private String enumQuery(Map<String, String> query, String key, Set<String> allowed) {
        if (!query.containsKey(key)) return null;
        String value = query.get(key);
        if (!allowed.contains(value)) throw new CommunityException(400, 40001, "invalid " + key);
        return value;
    }

    private String enumBody(Map<String, Object> body, String field, Set<String> allowed, String fallback) {
        String value = string(body.get(field));
        if (value == null) {
            if (fallback == null) throw new CommunityException(400, 40001, "invalid " + field);
            return fallback;
        }
        if (!allowed.contains(value)) throw new CommunityException(400, 40001, "invalid " + field);
        return value;
    }

    private String validateSlug(Map<String, Object> body) {
        String value = validateRequiredString(body, "slug", 2, 60);
        if (!value.matches("[a-z0-9-]+")) throw new CommunityException(400, 40001, "invalid slug");
        return value;
    }

    private String stringRequired(Map<String, Object> body, String field) {
        return validateRequiredString(body, field, 1, 120);
    }

    private void validateReason(Map<String, Object> body, int max) {
        validateRequiredString(body, "reason", 1, max);
    }

    private void validateIdempotencyKey(Map<String, Object> body) {
        if (!body.containsKey("idempotencyKey")) return;
        String value = string(body.get("idempotencyKey"));
        if (value == null || value.length() < 8 || value.length() > 80) throw new CommunityException(400, 40001, "invalid idempotency key");
    }

    private String idempotencyKey(Map<String, Object> body) {
        Object value = body.get("idempotencyKey");
        return value == null ? null : value.toString();
    }

    private IdempotencyRecord replay(String actorId, String operation, Map<String, Object> body) {
        String key = idempotencyKey(body);
        if (key == null) return null;
        IdempotencyRecord existing = idempotency.get(actorId + ":" + operation + ":" + key);
        if (existing != null && !existing.fingerprint().equals(canonical(body))) throw new CommunityException(409, 49017, "idempotency conflict");
        return existing;
    }

    private void remember(String actorId, String operation, Map<String, Object> body, Map<String, Object> value) {
        String key = idempotencyKey(body);
        if (key != null) idempotency.put(actorId + ":" + operation + ":" + key, new IdempotencyRecord(canonical(body), value));
    }

    private String canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> Objects.toString(entry.getKey())))
                    .forEach(entry -> builder.append(Objects.toString(entry.getKey())).append('=').append(canonical(entry.getValue())).append(';'));
            return builder.append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (Object item : list) builder.append(canonical(item)).append(';');
            return builder.append(']').toString();
        }
        return value == null ? "null" : value.getClass().getSimpleName() + ":" + value;
    }

    private Map<String, Object> pageRows(List<Map<String, Object>> rows, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return linkedMap("items", new ArrayList<>(rows.subList(from, to)), "page", page, "pageSize", pageSize, "total", rows.size());
    }

    private int countPrefix(Set<String> values, String suffix) {
        return (int) values.stream().filter(value -> value.endsWith(suffix)).count();
    }

    private void requireOwner(CommunityUser actor, String ownerUserId, int code) {
        if (!actor.userId().equals(ownerUserId)) throw new CommunityException(404, code, "not found");
    }

    private void validateTimeRange(Map<String, String> query) {
        try {
            Instant from = query.containsKey("from") ? Instant.parse(query.get("from")) : null;
            Instant to = query.containsKey("to") ? Instant.parse(query.get("to")) : null;
            if (from != null && to != null && from.isAfter(to)) throw new CommunityException(400, 40001, "invalid time range");
        } catch (DateTimeParseException ex) {
            throw new CommunityException(400, 40001, "invalid time");
        }
    }

    private List<String> stringList(Map<String, Object> body, String field, Set<String> allowed, int min, int max) {
        List<String> values = looseStringList(body, field, min, max, 80);
        for (String value : values) {
            if (!allowed.contains(value)) throw new CommunityException(400, 40001, "invalid " + field);
        }
        return values;
    }

    private List<String> looseStringList(Map<String, Object> body, String field, int min, int max, int maxLength) {
        Object raw = body.get(field);
        if (raw == null) {
            if (min == 0) return new ArrayList<>();
            throw new CommunityException(400, 40001, "invalid " + field);
        }
        if (!(raw instanceof List<?> list) || list.size() < min || list.size() > max) throw new CommunityException(400, 40001, "invalid " + field);
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            String value = string(item);
            if (value == null || value.isBlank() || value.length() > maxLength) throw new CommunityException(400, 40001, "invalid " + field);
            values.add(value);
        }
        return values;
    }

    private String validateRequiredString(Map<String, Object> body, String field, int min, int max) {
        String value = string(body.get(field));
        if (value == null || value.isBlank() || value.length() < min || value.length() > max) throw new CommunityException(400, 40001, "invalid " + field);
        return value;
    }

    private String optionalString(Map<String, Object> body, String field, int max) {
        String value = string(body.get(field));
        if (value == null) return null;
        if (value.length() > max) throw new CommunityException(400, 40001, "invalid " + field);
        return value;
    }

    private int intBody(Map<String, Object> body, String field, int fallback) {
        if (!body.containsKey(field)) return fallback;
        Object value = body.get(field);
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(Objects.toString(value));
        } catch (NumberFormatException ex) {
            throw new CommunityException(400, 40001, "invalid " + field);
        }
    }

    private boolean boolBody(Map<String, Object> body, String field, boolean fallback) {
        if (!body.containsKey(field)) return fallback;
        Object value = body.get(field);
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(Objects.toString(value));
    }

    private String lower(String value) {
        return value == null || value.isBlank() ? null : value.toLowerCase();
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    static Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(values[i].toString(), values[i + 1]);
        return map;
    }
}

class TestCommunityAuthProvider {
    CommunityUser requireUser(String authorization) {
        try {
            var trusted = TrustedGatewayAuth.from(currentRequest());
            if (trusted.isPresent()) {
                TrustedGatewayAuth.Actor actor = trusted.get();
                return new CommunityUser(actor.userId(), actor.userId(), actor.roles(), "ACTIVE", actor.authMode());
            }
        } catch (TrustedGatewayAuth.MalformedContextException exception) {
            throw new CommunityException(502, 49202, "auth incompatible");
        }
        if (authorization == null || authorization.isBlank()) throw new CommunityException(401, 41000, "not logged in");
        if (!authorization.startsWith("Bearer ")) throw new CommunityException(401, 41003, "bad token format");
        String token = authorization.substring("Bearer ".length());
        return switch (token) {
            case "auth-unavailable-token", "disabled-token", "banned-token", "deleted-token" -> throw new CommunityException(502, 49200, "auth unavailable");
            case "auth-timeout-token" -> throw new CommunityException(504, 49201, "auth timeout");
            case "auth-bad-token" -> throw new CommunityException(502, 49202, "auth incompatible");
            case "owner-token" -> local("owner", "Owner", Set.of("OWNER"), "ACTIVE");
            case "admin-token" -> local("admin", "Admin", Set.of("ADMIN"), "ACTIVE");
            case "helper-token" -> local("helper", "Helper", Set.of("HELPER"), "ACTIVE");
            case "user-token" -> local("user", "User", Set.of("USER"), "ACTIVE");
            case "member-user-1-token" -> local("member-user-1", "Member One", Set.of("USER"), "ACTIVE");
            case "member-user-2-token" -> local("member-user-2", "Member Two", Set.of("USER"), "ACTIVE");
            case "pending-profile-token" -> local("pending-profile", "Pending Profile", Set.of("USER"), "PENDING_PROFILE");
            case "other-token" -> local("other", "Other", Set.of("USER"), "ACTIVE");
            default -> throw new CommunityException(401, 41001, "invalid session");
        };
    }

    CommunityUser requireAny(String authorization, String... roles) {
        CommunityUser user = requireUser(authorization);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) throw new CommunityException(403, 42001, "role permission denied");
        return user;
    }

    private CommunityUser local(String userId, String displayName, Set<String> roles, String status) {
        return new CommunityUser(userId, displayName, roles, status, "TEST_STUB");
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}

record CommunityUser(String userId, String displayName, Set<String> roles, String status, String authMode) {
}

record CommunityTestControls(boolean enabled) {
}

interface CommunityFlowEvidenceRecorder {
    void recordBoardWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordPostWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordCommentWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordReportWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);
}

class NoopCommunityFlowEvidenceRecorder implements CommunityFlowEvidenceRecorder {
    @Override
    public void recordBoardWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordPostWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordCommentWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordReportWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }
}

record IdempotencyRecord(String fingerprint, Map<String, Object> value) {
}

record MutationResult(boolean created, Map<String, Object> value) {
}

class BoardRecord {
    String boardId;
    String slug;
    String name;
    String description;
    String visibility;
    String status;
    List<String> allowedPostTypes = new ArrayList<>();
    List<String> tags = new ArrayList<>();
    int sortOrder;
    String lastPostAt;
    String createdAt;
    String updatedAt;
    String archivedAt;
}

class PostRecord {
    String postId;
    String boardId;
    String type;
    String title;
    String summary;
    String body;
    List<String> tags = new ArrayList<>();
    String status;
    Map<String, Object> author;
    Map<String, Object> linkedContentSnapshot;
    Map<String, Object> linkedResourceSnapshot;
    String pollId;
    int likeCount;
    int favoriteCount;
    int viewCount;
    String acceptedCommentId;
    String lastCommentAt;
    String submittedAt;
    String reviewedAt;
    String reviewerUserId;
    String reviewComment;
    String notificationStatus;
    Map<String, Object> notificationFailure;
    String createdAt;
    String updatedAt;
    String offlineAt;
    String archivedAt;
    String deletedAt;

    String authorUserId() {
        return Objects.toString(author.get("userId"));
    }
}

class CommentRecord {
    String commentId;
    String postId;
    String parentCommentId;
    String body;
    String status;
    Map<String, Object> author;
    int likeCount;
    boolean isAcceptedAnswer;
    String submittedAt;
    String reviewedAt;
    String reviewComment;
    String createdAt;
    String updatedAt;
    String deletedAt;

    String authorUserId() {
        return Objects.toString(author.get("userId"));
    }
}

class PollRecord {
    String pollId;
    String postId;
    String title;
    String description;
    String status;
    List<PollOption> options = new ArrayList<>();
    boolean multipleChoice;
    int minChoices;
    int maxChoices;
    String eligibleVisibility;
    boolean anonymousResult;
    int voteCount;
    String opensAt;
    String closesAt;
    String createdAt;
    String updatedAt;
}

class PollOption {
    String optionId;
    String label;
    String description;
    int voteCount;
}

class ReportRecord {
    String reportId;
    String targetType;
    String targetId;
    String reasonType;
    String description;
    List<String> evidenceLinks = new ArrayList<>();
    String status;
    Map<String, Object> reporter;
    String assigneeUserId;
    String resolution;
    String linkedPenaltyId;
    String notificationStatus;
    String createdAt;
    String updatedAt;
    String resolvedAt;

    String reporterUserId() {
        return Objects.toString(reporter.get("userId"));
    }
}

class TicketRecord {
    String ticketId;
    String type;
    String title;
    String status;
    String priority;
    Map<String, Object> creator;
    String assigneeUserId;
    Map<String, Object> relatedObject;
    List<TicketMessageRecord> messages = new ArrayList<>();
    String lastReplyAt;
    String resolvedAt;
    String closedAt;
    String createdAt;
    String updatedAt;

    String creatorUserId() {
        return Objects.toString(creator.get("userId"));
    }
}

class TicketMessageRecord {
    String messageId;
    String ticketId;
    String messageType;
    String body;
    Map<String, Object> author;
    List<Map<String, Object>> attachments = List.of();
    String createdAt;
}

class PenaltyRecord {
    String penaltyId;
    String targetUserId;
    String targetMemberId;
    String type;
    String status;
    String reason;
    String publicReason;
    String evidenceReportId;
    String relatedPostId;
    String relatedCommentId;
    String startsAt;
    String expiresAt;
    String createdBy;
    String revokedBy;
    String revokedAt;
    String revokeReason;
    String createdAt;
    String updatedAt;
}

class CommunityException extends RuntimeException {
    final int httpStatus;
    final int code;

    CommunityException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}

@RestControllerAdvice(basePackageClasses = CommunityController.class)
class CommunityExceptionHandler {
    @ExceptionHandler(CommunityException.class)
    ResponseEntity<Map<String, Object>> handleCommunity(CommunityException ex) {
        return ResponseEntity.status(ex.httpStatus).body(error(ex.code, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(error(40400, "not found"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(500).body(error(54000, "community internal error"));
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        body.put("errors", List.of());
        body.put("requestId", CommunityController.requestId());
        return body;
    }
}

@Configuration
class CommunityRequestIdFilterConfig {
    @Bean
    CommunityRequestIdFilter communityRequestIdFilter() {
        return new CommunityRequestIdFilter();
    }
}

class CommunityRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) requestId = "req-" + UUID.randomUUID();
        request.setAttribute("requestId", requestId);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}
