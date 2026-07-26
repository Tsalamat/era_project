package kz.kta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"kta.demo-data.enabled=true",
	"kta.email-verification.enabled=false",
	"kta.email-verification.dev-code=123456"
})
@AutoConfigureMockMvc
class TestFlowIntegrationTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	void studentCanCompletePublishedTestWithoutCorrectAnswersLeaking() throws Exception {
		var token = login("student@kta.kz", "Student123!");
		var testResponse = mvc.perform(get("/api/tests/demo-kta"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.questions[0].options[0].isCorrect").doesNotExist())
			.andExpect(jsonPath("$.questions[0].explanation").doesNotExist())
			.andReturn().getResponse().getContentAsString();
		JsonNode test = objectMapper.readTree(testResponse);
		var testId = test.get("id").asText();

		var startResponse = mvc.perform(post("/api/tests/{id}/start", testId)
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		var attemptId = objectMapper.readTree(startResponse).get("id").asText();

		var first = test.get("questions").get(0);
		var second = test.get("questions").get(1);
		var request = Map.of(
			"attemptId", attemptId,
			"answers", List.of(
				Map.of("questionId", first.get("id").asText(), "answerOptionIds", List.of(first.get("options").get(2).get("id").asText())),
				Map.of("questionId", second.get("id").asText(), "answerOptionIds", List.of(second.get("options").get(0).get("id").asText()))
			)
		);

		mvc.perform(post("/api/tests/{id}/submit", testId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.score").value(2))
			.andExpect(jsonPath("$.maxScore").value(2))
			.andExpect(jsonPath("$.scorePercent").value(100.0))
			.andExpect(jsonPath("$.questions[0].explanation").isNotEmpty());
	}

	@Test
	void onlyTeacherCanCreateTest() throws Exception {
		var studentToken = login("student@kta.kz", "Student123!");
		var teacherToken = login("teacher@kta.kz", "Teacher123!");
		var adminToken = login("admin@kta.kz", "Admin123!");
		var request = Map.of(
			"title", "Интеграционный тест",
			"slug", "integration-test",
			"description", "Проверка API",
			"subject", "Логика",
			"timeLimitMinutes", 15
		);

		mvc.perform(post("/api/teacher/tests")
				.header("Authorization", "Bearer " + studentToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(request)))
			.andExpect(status().isForbidden());

		mvc.perform(post("/api/teacher/tests")
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRAFT"));

		var adminRequest = Map.of(
			"title", "Админский тест",
			"slug", "admin-integration-test",
			"description", "Проверка API для администратора",
			"subject", "Логика",
			"timeLimitMinutes", 20
		);
		mvc.perform(post("/api/teacher/tests")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(adminRequest)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRAFT"));

		var cookieOnlyAdminRequest = Map.of(
			"title", "Cookie admin test",
			"slug", "cookie-admin-test",
			"description", "Проверка API через cookie",
			"subject", "Логика",
			"timeLimitMinutes", 20
		);
		mvc.perform(post("/api/teacher/tests")
				.cookie(new MockCookie("kta_access_token", adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(cookieOnlyAdminRequest)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRAFT"));

		mvc.perform(post("/api/teacher/tests")
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(Map.of())))
			.andExpect(status().isBadRequest());
	}

	@Test
	void registrationCodeCanBeRequestedAgainBeforeVerification() throws Exception {
		var email = "repeat-" + UUID.randomUUID() + "@kta.kz";
		mvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(Map.of(
					"fullName", "Первый студент", "email", email, "password", "Student123!"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("accepted"));

		mvc.perform(post("/api/auth/register/request-code")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(Map.of(
					"fullName", "Второй студент", "email", email, "password", "Student456!"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("accepted"));

		mvc.perform(post("/api/auth/register/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(Map.of("email", email, "code", "123456"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Второй студент"));
		login(email, "Student456!");
	}

	@Test
	void newStudentProgressStartsAtZeroAndLessonCompletionIsIdempotent() throws Exception {
		var token = register("new-" + UUID.randomUUID() + "@example.kz");

		mvc.perform(get("/api/users/me/learning-summary").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completedLessons").value(0))
			.andExpect(jsonPath("$.progressPercent").value(0));

		mvc.perform(get("/api/users/me/results").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));

		var courseResponse = mvc.perform(get("/api/courses/kta-logika").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completedLessons").value(0))
			.andReturn().getResponse().getContentAsString();
		var lessonId = objectMapper.readTree(courseResponse).get("modules").get(0).get("lessons").get(0).get("id").asText();

		mvc.perform(post("/api/lessons/{id}/complete", lessonId).header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completed").value(true));
		mvc.perform(post("/api/lessons/{id}/complete", lessonId).header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());

		mvc.perform(get("/api/users/me/learning-summary").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completedLessons").value(1));
	}

	@Test
	void chatMessagesAreStoredWithRealUserNameAndRole() throws Exception {
		var studentToken = login("student@kta.kz", "Student123!");
		var teacherToken = login("teacher@kta.kz", "Teacher123!");
		var message = "Здравствуйте, нужен разбор задания";

		mvc.perform(post("/api/chat/messages")
				.header("Authorization", "Bearer " + studentToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(Map.of("message", message))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value(message))
			.andExpect(jsonPath("$.senderName").value("Студент КТА"))
			.andExpect(jsonPath("$.senderRole").value("STUDENT"))
			.andExpect(jsonPath("$.mine").value(true));

		mvc.perform(get("/api/chat/messages")
				.header("Authorization", "Bearer " + teacherToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].message").value(message))
			.andExpect(jsonPath("$[0].senderName").value("Студент КТА"))
			.andExpect(jsonPath("$[0].senderRole").value("STUDENT"))
			.andExpect(jsonPath("$[0].mine").value(false));
	}

	@Test
	void teacherCanImportQuestionsFromWordDocument() throws Exception {
		var teacherToken = login("teacher@kta.kz", "Teacher123!");
		var slug = "word-import-" + UUID.randomUUID();
		var testResponse = mvc.perform(post("/api/teacher/tests")
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(Map.of(
					"title", "Импорт из Word",
					"slug", slug,
					"description", "Проверка загрузки вопросов",
					"subject", "Комплексный",
					"timeLimitMinutes", 25
				))))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		var testId = objectMapper.readTree(testResponse).get("id").asText();

		var file = new MockMultipartFile(
			"file",
			"questions.docx",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			docx(
				"1. Какой блок проверяет работу с текстом?",
				"A) Математическая грамотность",
				"B) Оқу-аналитикалық сауаттылық",
				"C) Профильный предмет",
				"D) Иностранный язык",
				"Ответ: B",
				"Объяснение: Этот блок связан с анализом текста.",
				"2. Какие инструменты помогают учиться по результатам теста?",
				"A) Аналитика результата",
				"B) Объяснение ошибок",
				"C) Скачивание Word-отчета",
				"D) Цвет темы сайта",
				"Ответ: A,B,C",
				"Объяснение: Несколько букв создают MULTIPLE_CHOICE."
			)
		);

		mvc.perform(multipart("/api/teacher/tests/{id}/questions/import-docx", testId)
				.file(file)
				.header("Authorization", "Bearer " + teacherToken))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.importedCount").value(2))
			.andExpect(jsonPath("$.questions[0].questionType").value("SINGLE_CHOICE"))
			.andExpect(jsonPath("$.questions[0].options[1].isCorrect").value(true))
			.andExpect(jsonPath("$.questions[1].questionType").value("MULTIPLE_CHOICE"))
			.andExpect(jsonPath("$.questions[1].options[0].isCorrect").value(true))
			.andExpect(jsonPath("$.questions[1].options[1].isCorrect").value(true))
			.andExpect(jsonPath("$.questions[1].options[2].isCorrect").value(true));

		mvc.perform(get("/api/teacher/tests/{id}", testId)
				.header("Authorization", "Bearer " + teacherToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.questions.length()").value(2));
	}

	private String login(String email, String password) throws Exception {
		var response = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(Map.of("email", email, "password", password))))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("accessToken").asText();
	}

	private String register(String email) throws Exception {
		mvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(Map.of(
					"fullName", "Новый студент", "email", email, "password", "Student123!"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("accepted"));
		var response = mvc.perform(post("/api/auth/register/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(Map.of("email", email, "code", "123456"))))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("accessToken").asText();
	}

	private byte[] docx(String... lines) throws Exception {
		try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
			for (var line : lines) {
				document.createParagraph().createRun().setText(line);
			}
			document.write(output);
			return output.toByteArray();
		}
	}
}
