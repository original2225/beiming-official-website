package cn.beiming.exam;

import cn.beiming.admission.AdmissionCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 题库回读契约：管理员建的题必须能在重启之后仍然考得到。
 *
 * <p>锁的是那个不报错的 bug：{@code ExamPersistence} 只有写、没有读，
 * 进程重启后内存里只剩硬编码的 5 道种子题，接口建的题全部消失——
 * 后台看得到记录、库里也有行，但考生永远抽不到，全程无异常。
 *
 * <p>必须在真实 PostgreSQL 上跑：内存模式下 loadQuestions 返回空，
 * 「读得回来」根本无从验证（这正是假持久化能一直绿的原因）。
 */
@SpringBootTest(
        classes = AdmissionCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true",
                "exam.test-controls.enabled=false",
                "exam.auth.stub-tokens-enabled=true"
        }
)
@Testcontainers
class ExamBankReloadIntegrationTest {

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_exam_reload_pg")
            .withUsername("beiming")
            .withPassword("beiming");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("EXAM-RELOAD-001 新建题目落库后能读回，serverType 不丢、options 仍是结构化列表")
    void createdQuestionIsReloadable() throws Exception {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("reviewDirection", "GENERAL");
        question.put("difficulty", "NORMAL");
        question.put("type", "TRUE_FALSE");
        // 生电专属：服型必须往返，否则重启后所有卷退化成通用卷
        question.put("serverType", "SURVIVAL");
        question.put("stem", "回读探针：生电服禁止无意义死亡");
        question.put("options", List.of(
                Map.of("optionId", "A", "label", "对", "text", "对"),
                Map.of("optionId", "B", "label", "错", "text", "错")));
        question.put("correctOptionIds", List.of("A"));
        question.put("score", 5);
        question.put("reason", "reload probe");
        question.put("idempotencyKey", "exam-reload-create-" + UUID.randomUUID());

        JsonNode created = send(HttpMethod.POST, "/api/v1/exams/admin/question-bank/questions", question, 201);
        String questionId = created.at("/data/questionId").asText();
        assertThat(questionId).isNotBlank();

        // 激活——未激活的题试卷永远选不中（本轮修的另一个缺陷）
        send(HttpMethod.PATCH, "/api/v1/exams/admin/question-bank/questions/" + questionId,
                Map.of("status", "ACTIVE", "reason", "activate",
                        "idempotencyKey", "exam-reload-activate-" + UUID.randomUUID()), 200);

        // 直接查库：断言落库的列里确实有 server_type，且是 SURVIVAL
        String status;
        String serverType;
        String optionsJson;
        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             java.sql.PreparedStatement statement = connection.prepareStatement(
                     "SELECT status, server_type, options::text FROM exam_questions WHERE question_id = ?")) {
            statement.setString(1, questionId);
            try (java.sql.ResultSet rows = statement.executeQuery()) {
                assertThat(rows.next()).as("题目必须真的落库，否则重启即消失").isTrue();
                status = rows.getString("status");
                serverType = rows.getString("server_type");
                optionsJson = rows.getString("options");
            }
        }
        assertThat(status).isEqualTo("ACTIVE");
        assertThat(serverType).as("服型分卷靠这一列落库，NULL 等于重启后退化成通用卷").isEqualTo("SURVIVAL");
        assertThat(optionsJson).contains("optionId");

        // 再走一次对外读取路径，确认重启后考生侧还能看见这道题
        List<JsonNode> reloaded = listBankContaining(questionId);
        assertThat(reloaded).as("读回路径必须能看到这道题").hasSize(1);
        assertThat(reloaded.get(0).at("/serverType").asText()).isEqualTo("SURVIVAL");
    }

    private List<JsonNode> listBankContaining(String questionId) throws Exception {
        JsonNode page = send(HttpMethod.GET, "/api/v1/exams/admin/question-bank/questions?pageSize=100", null, 200);
        List<JsonNode> matched = new ArrayList<>();
        for (JsonNode item : page.at("/data/items")) {
            if (questionId.equals(item.at("/questionId").asText())) {
                matched.add(item);
            }
        }
        return matched;
    }

    private JsonNode send(HttpMethod method, String path, Map<String, Object> body, int expectedStatus) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (body == null) {
            builder.method(method.name(), HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        }
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as("%s %s 期望 %s，实际 %s，body=%s", method, path, expectedStatus, response.statusCode(), response.body())
                .isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body());
    }
}
