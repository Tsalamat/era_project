package kz.kta.test;

import kz.kta.auth.UserAccount;
import kz.kta.auth.UserAccountRepository;
import kz.kta.common.QuestionType;
import kz.kta.common.Role;
import kz.kta.common.TestStatus;
import kz.kta.notification.NotificationEntity;
import kz.kta.notification.NotificationRepository;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static kz.kta.test.TestDtos.*;

@Service
public class TestService {

	private static final DateTimeFormatter REPORT_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Asia/Almaty"));

	private final TestRepository tests;
	private final QuestionRepository questions;
	private final TestAttemptRepository attempts;
	private final TestAssignmentRepository assignments;
	private final UserAccountRepository users;
	private final NotificationRepository notifications;
	private final WordQuestionImporter wordQuestionImporter;

	public TestService(TestRepository tests, QuestionRepository questions, TestAttemptRepository attempts, TestAssignmentRepository assignments, UserAccountRepository users, NotificationRepository notifications, WordQuestionImporter wordQuestionImporter) {
		this.tests = tests;
		this.questions = questions;
		this.attempts = attempts;
		this.assignments = assignments;
		this.users = users;
		this.notifications = notifications;
		this.wordQuestionImporter = wordQuestionImporter;
	}

	@Transactional(readOnly = true)
	public List<TestSummary> publishedTests() {
		return tests.findByStatusOrderByCreatedAtDesc(TestStatus.PUBLISHED).stream().map(this::summary).toList();
	}

	@Transactional(readOnly = true)
	public StudentTestDetails studentTest(String slug) {
		var test = tests.findBySlugAndStatus(slug, TestStatus.PUBLISHED)
			.orElseThrow(() -> notFound("Тест не найден"));
		return new StudentTestDetails(
			test.getId(), test.getSlug(), test.getTitle(), test.getSubject(), test.getDescription(), test.getTimeLimitMinutes(),
			test.getQuestions().stream().map(this::studentQuestion).toList()
		);
	}

	@Transactional
	public TestAttempt start(UUID testId) {
		var user = currentUser();
		var test = publishedTest(testId);
		var open = attempts.findFirstByTestIdAndUserIdAndCompletedAtIsNullOrderByStartedAtDesc(testId, user.getId());
		var attempt = open.orElseGet(() -> attempts.save(new TestAttemptEntity(test, user)));
		return new TestAttempt(attempt.getId(), testId, attempt.getStartedAt(), "STARTED");
	}

	@Transactional
	public TestResult submit(UUID testId, SubmitTestRequest request) {
		var user = currentUser();
		var attempt = attempts.findById(request.attemptId()).orElseThrow(() -> notFound("Попытка не найдена"));
		if (!attempt.getUser().getId().equals(user.getId()) || !attempt.getTest().getId().equals(testId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Попытка принадлежит другому пользователю или тесту");
		}
		if (attempt.getCompletedAt() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Эта попытка уже завершена");
		}

		var answersByQuestion = new HashMap<UUID, Set<UUID>>();
		for (var answer : request.answers()) {
			answersByQuestion.put(answer.questionId(), new HashSet<>(answer.answerOptionIds()));
		}

		var score = 0;
		for (var question : attempt.getTest().getQuestions()) {
			var selectedIds = answersByQuestion.getOrDefault(question.getId(), Set.of());
			var availableIds = question.getOptions().stream().map(AnswerOptionEntity::getId).collect(java.util.stream.Collectors.toSet());
			if (!availableIds.containsAll(selectedIds)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выбран вариант из другого вопроса");
			}
			if (question.getQuestionType() == QuestionType.SINGLE_CHOICE && selectedIds.size() > 1) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Для вопроса с одним ответом выбран лишний вариант");
			}
			var correctIds = correctOptionIds(question);
			if (correctIds.equals(selectedIds)) {
				score++;
			}
			question.getOptions().stream()
				.filter(option -> selectedIds.contains(option.getId()))
				.forEach(option -> attempt.addAnswer(question, option));
		}

		attempt.complete(score, attempt.getTest().getQuestions().size());
		attempts.save(attempt);
		assignments.findByTestIdAndStudentId(testId, user.getId()).ifPresent(assignment -> {
			assignment.markCompleted(attempt.getCompletedAt());
			assignments.save(assignment);
		});
		return result(attempt);
	}

	@Transactional(readOnly = true)
	public TestResult latestResult(UUID testId) {
		var user = currentUser();
		var attempt = attempts.findFirstByTestIdAndUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(testId, user.getId())
			.orElseThrow(() -> notFound("Завершенная попытка не найдена"));
		return result(attempt);
	}

	@Transactional(readOnly = true)
	public List<TestSummary> teacherTests() {
		var user = currentUser();
		var source = isAdmin(user) ? tests.findAll() : tests.findByCreatedByIdOrderByUpdatedAtDesc(user.getId());
		return source.stream().map(this::summary).toList();
	}

	@Transactional(readOnly = true)
	public TeacherTestDetails teacherTest(UUID id) {
		return teacherDetails(ownedTest(id));
	}

	@Transactional
	public TeacherTestDetails create(TeacherTestRequest request) {
		var user = currentUser();
		if (tests.existsBySlug(request.slug())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug уже используется");
		}
		return teacherDetails(tests.save(new TestEntity(
			request.title().trim(), request.slug().trim(), request.description(), request.subject().trim(), request.timeLimitMinutes(), user
		)));
	}

	@Transactional
	public TeacherTestDetails update(UUID id, TeacherTestRequest request) {
		var test = ownedTest(id);
		if (tests.existsBySlugAndIdNot(request.slug(), id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug уже используется");
		}
		test.update(request.title().trim(), request.slug().trim(), request.description(), request.subject().trim(), request.timeLimitMinutes());
		return teacherDetails(tests.save(test));
	}

	@Transactional
	public void delete(UUID id) {
		tests.delete(ownedTest(id));
	}

	@Transactional
	public TeacherQuestion addQuestion(UUID testId, QuestionRequest request) {
		var test = ownedTest(testId);
		validateQuestion(request);
		var question = new QuestionEntity(test, request.questionText().trim(), request.questionType(), request.explanation(), request.orderNumber());
		question.replaceOptions(optionEntities(question, request));
		question = questions.save(question);
		return teacherQuestion(question);
	}

	@Transactional
	public QuestionImportResult importQuestions(UUID testId, MultipartFile file) {
		var test = ownedTest(testId);
		var nextOrderNumber = test.getQuestions().stream().mapToInt(QuestionEntity::getOrderNumber).max().orElse(0) + 1;
		var requests = wordQuestionImporter.parse(file, nextOrderNumber);
		var imported = new ArrayList<TeacherQuestion>();
		for (var request : requests) {
			validateQuestion(request);
			var question = new QuestionEntity(test, request.questionText().trim(), request.questionType(), request.explanation(), request.orderNumber());
			question.replaceOptions(optionEntities(question, request));
			imported.add(teacherQuestion(questions.save(question)));
		}
		return new QuestionImportResult(imported.size(), imported);
	}

	@Transactional
	public TeacherQuestion updateQuestion(UUID questionId, QuestionRequest request) {
		var question = questions.findById(questionId).orElseThrow(() -> notFound("Вопрос не найден"));
		assertOwner(question.getTest());
		validateQuestion(request);
		question.update(request.questionText().trim(), request.questionType(), request.explanation(), request.orderNumber());
		updateOptions(question, request);
		return teacherQuestion(questions.save(question));
	}

	@Transactional
	public void deleteQuestion(UUID questionId) {
		var question = questions.findById(questionId).orElseThrow(() -> notFound("Вопрос не найден"));
		assertOwner(question.getTest());
		questions.delete(question);
	}

	@Transactional
	public TeacherTestDetails publish(UUID id, boolean published) {
		var test = ownedTest(id);
		if (published) {
			if (test.getQuestions().isEmpty()) {
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Добавьте хотя бы один вопрос перед публикацией");
			}
			for (var question : test.getQuestions()) {
				validateStoredQuestion(question);
			}
		}
		test.setStatus(published ? TestStatus.PUBLISHED : TestStatus.DRAFT);
		return teacherDetails(tests.save(test));
	}

	@Transactional(readOnly = true)
	public TeacherTestStats stats(UUID id) {
		var test = ownedTest(id);
		var completed = attempts.findByTestIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(id);
		var averageScore = completed.stream().mapToDouble(this::scorePercent).average().orElse(0);
		var averageTime = completed.stream()
			.mapToDouble(attempt -> Duration.between(attempt.getStartedAt(), attempt.getCompletedAt()).toSeconds() / 60.0)
			.average().orElse(0);

		var hardest = new ArrayList<HardQuestion>();
		for (var question : test.getQuestions()) {
			var wrong = completed.stream().filter(attempt -> !answeredCorrectly(attempt, question)).count();
			var percent = completed.isEmpty() ? 0 : wrong * 100.0 / completed.size();
			hardest.add(new HardQuestion(question.getId(), question.getQuestionText(), round(percent)));
		}
		hardest.sort(Comparator.comparingDouble(HardQuestion::wrongPercent).reversed());

		var students = completed.stream().map(attempt -> {
			var analytics = analytics(attempt, questionResults(attempt));
			return new StudentAttemptResult(
				attempt.getId(), attempt.getUser().getFullName(), attempt.getUser().getEmail(),
				attempt.getScore(), attempt.getMaxScore(), round(scorePercent(attempt)),
				analytics.answeredQuestions(), analytics.skippedQuestions(), analytics.durationMinutes(),
				attempt.getCompletedAt()
			);
		}).toList();
		return new TeacherTestStats(id, completed.size(), round(averageScore), round(averageTime), hardest, students);
	}

	@Transactional(readOnly = true)
	public List<TestResult> resultsForCurrentUser() {
		var user = currentUser();
		return attempts.findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(user.getId()).stream().map(this::result).toList();
	}

	@Transactional(readOnly = true)
	public List<StudentAssignedTest> assignedTestsForCurrentUser() {
		var user = currentUser();
		return assignments.findByStudentIdOrderByAssignedAtDesc(user.getId()).stream().map(this::studentAssignedTest).toList();
	}

	@Transactional(readOnly = true)
	public TestReportFile latestResultReport(UUID testId) {
		var user = currentUser();
		var attempt = attempts.findFirstByTestIdAndUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(testId, user.getId())
			.orElseThrow(() -> notFound("Завершенная попытка не найдена"));
		return studentReport(attempt);
	}

	@Transactional(readOnly = true)
	public TestReportFile teacherResultsReport(UUID testId) {
		var test = ownedTest(testId);
		var completed = attempts.findByTestIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(testId);
		return teacherReport(test, completed);
	}

	@Transactional(readOnly = true)
	public List<TestAssignmentDto> teacherAssignments(UUID testId) {
		ownedTest(testId);
		return assignments.findByTestIdOrderByAssignedAtDesc(testId).stream().map(this::assignmentDto).toList();
	}

	@Transactional
	public List<TestAssignmentDto> assignTest(UUID testId, AssignTestRequest request) {
		var teacher = currentUser();
		var test = ownedTest(testId);
		if (test.getStatus() != TestStatus.PUBLISHED) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Сначала опубликуйте тест");
		}
		var uniqueStudentIds = request.studentIds().stream().distinct().toList();
		for (var studentId : uniqueStudentIds) {
			var student = users.findById(studentId).orElseThrow(() -> notFound("Студент не найден"));
			if (!hasRole(student, Role.STUDENT)) {
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Назначать тест можно только студентам");
			}
			var created = assignments.findByTestIdAndStudentId(testId, studentId).isEmpty();
			var assignment = assignments.findByTestIdAndStudentId(testId, studentId)
				.orElseGet(() -> new TestAssignmentEntity(test, student, teacher));
			assignments.save(assignment);
			if (created) {
				notifications.save(new NotificationEntity(
					student,
					"TEST_ASSIGNED",
					"Назначен тест",
					"Учитель назначил тест: " + test.getTitle(),
					"/tests/" + test.getSlug()
				));
			}
		}
		return teacherAssignments(testId);
	}

	private TestSummary summary(TestEntity test) {
		var completed = attempts.findByTestIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(test.getId());
		return new TestSummary(test.getId(), test.getSlug(), test.getTitle(), test.getSubject(), test.getTimeLimitMinutes(), test.getStatus(), completed.size(), round(completed.stream().mapToDouble(this::scorePercent).average().orElse(0)));
	}

	private StudentQuestion studentQuestion(QuestionEntity question) {
		return new StudentQuestion(
			question.getId(), question.getQuestionText(), question.getQuestionType(), question.getOrderNumber(),
			question.getOptions().stream().map(option -> new StudentOption(option.getId(), option.getOptionText(), option.getOrderNumber())).toList()
		);
	}

	private TeacherTestDetails teacherDetails(TestEntity test) {
		return new TeacherTestDetails(
			test.getId(), test.getSlug(), test.getTitle(), test.getSubject(), test.getDescription(), test.getTimeLimitMinutes(), test.getStatus(),
			test.getCreatedBy() == null ? null : test.getCreatedBy().getFullName(), test.getQuestions().stream().map(this::teacherQuestion).toList()
		);
	}

	private TestAssignmentDto assignmentDto(TestAssignmentEntity assignment) {
		return new TestAssignmentDto(
			assignment.getId(),
			assignment.getTest().getId(),
			assignment.getTest().getTitle(),
			assignment.getTest().getSlug(),
			assignment.getStudent().getFullName(),
			assignment.getStudent().getEmail(),
			assignment.getAssignedBy() == null ? null : assignment.getAssignedBy().getFullName(),
			assignment.getCompletedAt() == null ? "ASSIGNED" : "COMPLETED",
			assignment.getAssignedAt(),
			assignment.getCompletedAt()
		);
	}

	private StudentAssignedTest studentAssignedTest(TestAssignmentEntity assignment) {
		return new StudentAssignedTest(
			assignment.getId(),
			assignment.getTest().getId(),
			assignment.getTest().getSlug(),
			assignment.getTest().getTitle(),
			assignment.getTest().getSubject(),
			assignment.getTest().getTimeLimitMinutes(),
			assignment.getCompletedAt() == null ? "ASSIGNED" : "COMPLETED",
			assignment.getAssignedAt(),
			assignment.getCompletedAt()
		);
	}

	private TeacherQuestion teacherQuestion(QuestionEntity question) {
		return new TeacherQuestion(
			question.getId(), question.getQuestionText(), question.getQuestionType(), question.getExplanation(), question.getOrderNumber(),
			question.getOptions().stream().map(option -> new TeacherOption(option.getId(), option.getOptionText(), option.isCorrect(), option.getOrderNumber())).toList()
		);
	}

	private List<QuestionResult> questionResults(TestAttemptEntity attempt) {
		return attempt.getTest().getQuestions().stream().map(question -> {
			var selected = selectedOptionIds(attempt, question);
			var correct = correctOptionIds(question);
			return new QuestionResult(question.getId(), correct.equals(selected), selected.stream().toList(), correct.stream().toList(), question.getExplanation());
		}).toList();
	}

	private TestResult result(TestAttemptEntity attempt) {
		var questionResults = questionResults(attempt);
		return new TestResult(
			attempt.getTest().getId(), attempt.getTest().getTitle(), attempt.getId(), attempt.getScore(), attempt.getMaxScore(),
			round(scorePercent(attempt)), attempt.getCompletedAt(), analytics(attempt, questionResults), questionResults
		);
	}

	private TestAnalytics analytics(TestAttemptEntity attempt, List<QuestionResult> questionResults) {
		var total = questionResults.size();
		var answered = (int) questionResults.stream().filter(question -> !question.selectedOptionIds().isEmpty()).count();
		var correct = (int) questionResults.stream().filter(QuestionResult::correct).count();
		var skipped = Math.max(0, total - answered);
		var wrong = Math.max(0, answered - correct);
		var seconds = durationSeconds(attempt);
		var accuracy = round(scorePercent(attempt));
		return new TestAnalytics(
			total,
			answered,
			correct,
			wrong,
			skipped,
			accuracy,
			seconds,
			round(seconds / 60.0),
			level(accuracy),
			recommendation(accuracy, wrong, skipped)
		);
	}

	private long durationSeconds(TestAttemptEntity attempt) {
		if (attempt.getStartedAt() == null || attempt.getCompletedAt() == null) return 0;
		return Math.max(0, Duration.between(attempt.getStartedAt(), attempt.getCompletedAt()).toSeconds());
	}

	private String level(double accuracy) {
		if (accuracy >= 85) return "Отличный результат";
		if (accuracy >= 70) return "Хорошая готовность";
		if (accuracy >= 50) return "Нужно закрепить темы";
		return "Нужна повторная подготовка";
	}

	private String recommendation(double accuracy, int wrong, int skipped) {
		if (skipped > 0) return "Разберите пропущенные вопросы и повторите темы, где не хватило времени.";
		if (wrong > 0 && accuracy >= 70) return "Результат хороший. Повторите вопросы с ошибками и объяснения к ним.";
		if (wrong > 0) return "Сначала повторите теорию, затем пройдите тест повторно и сравните результат.";
		return "Ошибок нет. Можно переходить к следующему тесту или более сложному варианту.";
	}

	private TestReportFile studentReport(TestAttemptEntity attempt) {
		var result = result(attempt);
		var analytics = result.analytics();
		try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
			addTitle(document, "Аналитика результата теста");
			addLine(document, "Студент: " + attempt.getUser().getFullName());
			addLine(document, "Email: " + attempt.getUser().getEmail());
			addLine(document, "Тест: " + attempt.getTest().getTitle());
			addLine(document, "Дата завершения: " + REPORT_DATE.format(attempt.getCompletedAt()));
			addLine(document, "Результат: " + result.scorePercent() + "% (" + result.score() + " из " + result.maxScore() + ")");
			addLine(document, "Ответил на вопросов: " + analytics.answeredQuestions());
			addLine(document, "Не ответил на вопросов: " + analytics.skippedQuestions());
			addLine(document, "Правильно: " + analytics.correctAnswers());
			addLine(document, "Ошибок: " + analytics.wrongAnswers());
			addLine(document, "Время прохождения: " + analytics.durationMinutes() + " мин");
			addLine(document, "Уровень: " + analytics.level());
			addLine(document, "Рекомендация: " + analytics.recommendation());
			addBlank(document);
			addTitle(document, "Разбор вопросов");
			for (var question : attempt.getTest().getQuestions()) {
				var questionResult = result.questions().stream()
					.filter(item -> item.questionId().equals(question.getId()))
					.findFirst()
					.orElseThrow();
				addLine(document, question.getOrderNumber() + ". " + question.getQuestionText());
				addLine(document, "Статус: " + (questionResult.correct() ? "правильно" : questionResult.selectedOptionIds().isEmpty() ? "не отвечено" : "ошибка"));
				addLine(document, "Ответ студента: " + optionTexts(question, questionResult.selectedOptionIds()));
				addLine(document, "Правильный ответ: " + optionTexts(question, questionResult.correctOptionIds()));
				if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
					addLine(document, "Объяснение: " + question.getExplanation());
				}
				addBlank(document);
			}
			document.write(output);
			return new TestReportFile("student-test-analytics-" + attempt.getId() + ".docx", output.toByteArray());
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сформировать Word-отчет", exception);
		}
	}

	private TestReportFile teacherReport(TestEntity test, List<TestAttemptEntity> completed) {
		var stats = stats(test.getId());
		try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
			addTitle(document, "Аналитика теста для учителя");
			addLine(document, "Тест: " + test.getTitle());
			addLine(document, "Предмет: " + test.getSubject());
			addLine(document, "Количество прохождений: " + stats.attemptsCount());
			addLine(document, "Средний балл: " + stats.averageScorePercent() + "%");
			addLine(document, "Среднее время: " + stats.averageTimeMinutes() + " мин");
			addBlank(document);

			addTitle(document, "Результаты студентов");
			var table = document.createTable(Math.max(1, completed.size()) + 1, 8);
			setRow(table, 0, "Студент", "Email", "Процент", "Балл", "Ответил", "Не ответил", "Время", "Дата");
			for (var i = 0; i < completed.size(); i++) {
				var attempt = completed.get(i);
				var analytics = analytics(attempt, questionResults(attempt));
				setRow(
					table,
					i + 1,
					attempt.getUser().getFullName(),
					attempt.getUser().getEmail(),
					round(scorePercent(attempt)) + "%",
					attempt.getScore() + " / " + attempt.getMaxScore(),
					String.valueOf(analytics.answeredQuestions()),
					String.valueOf(analytics.skippedQuestions()),
					analytics.durationMinutes() + " мин",
					REPORT_DATE.format(attempt.getCompletedAt())
				);
			}

			addBlank(document);
			addTitle(document, "Самые сложные вопросы");
			for (var question : stats.hardestQuestions()) {
				addLine(document, question.questionText() + " — " + question.wrongPercent() + "% ошибок");
			}

			document.write(output);
			return new TestReportFile("teacher-test-analytics-" + test.getId() + ".docx", output.toByteArray());
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сформировать Word-отчет", exception);
		}
	}

	private void addTitle(XWPFDocument document, String text) {
		var paragraph = document.createParagraph();
		paragraph.setAlignment(ParagraphAlignment.LEFT);
		var run = paragraph.createRun();
		run.setBold(true);
		run.setFontSize(16);
		run.setText(text);
	}

	private void addLine(XWPFDocument document, String text) {
		var run = document.createParagraph().createRun();
		run.setFontSize(11);
		run.setText(text);
	}

	private void addBlank(XWPFDocument document) {
		document.createParagraph();
	}

	private void setRow(XWPFTable table, int rowIndex, String... values) {
		var row = table.getRow(rowIndex);
		for (var i = 0; i < values.length; i++) {
			var cell = row.getCell(i);
			if (cell == null) cell = row.addNewTableCell();
			cell.removeParagraph(0);
			cell.setText(values[i]);
		}
	}

	private String optionTexts(QuestionEntity question, List<UUID> ids) {
		if (ids.isEmpty()) return "не выбрано";
		return question.getOptions().stream()
			.filter(option -> ids.contains(option.getId()))
			.sorted(Comparator.comparingInt(AnswerOptionEntity::getOrderNumber))
			.map(AnswerOptionEntity::getOptionText)
			.collect(java.util.stream.Collectors.joining("; "));
	}

	private List<AnswerOptionEntity> optionEntities(QuestionEntity question, QuestionRequest request) {
		return request.options().stream()
			.map(option -> new AnswerOptionEntity(question, option.optionText().trim(), option.isCorrect(), option.orderNumber()))
			.toList();
	}

	private void updateOptions(QuestionEntity question, QuestionRequest request) {
		var existingById = question.getOptions().stream().collect(java.util.stream.Collectors.toMap(AnswerOptionEntity::getId, option -> option));
		var requestedIds = request.options().stream().map(AnswerOptionRequest::id).collect(java.util.stream.Collectors.toSet());
		if (requestedIds.contains(null) || !requestedIds.equals(existingById.keySet())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "При редактировании сохраните идентификаторы четырех вариантов ответа");
		}
		for (var option : request.options()) {
			existingById.get(option.id()).update(option.optionText().trim(), option.isCorrect(), option.orderNumber());
		}
	}

	private void validateQuestion(QuestionRequest request) {
		if (request.options().size() != 4) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "У вопроса должно быть ровно 4 варианта ответа");
		}
		var correctCount = request.options().stream().filter(AnswerOptionRequest::isCorrect).count();
		if (correctCount == 0 || request.questionType() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Укажите корректное количество правильных ответов");
		}
	}

	private void validateStoredQuestion(QuestionEntity question) {
		if (question.getOptions().size() != 4 || question.getOptions().stream().noneMatch(AnswerOptionEntity::isCorrect)) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Проверьте варианты ответа у вопроса: " + question.getQuestionText());
		}
	}

	private boolean answeredCorrectly(TestAttemptEntity attempt, QuestionEntity question) {
		return correctOptionIds(question).equals(selectedOptionIds(attempt, question));
	}

	private Set<UUID> selectedOptionIds(TestAttemptEntity attempt, QuestionEntity question) {
		var result = new HashSet<UUID>();
		attempt.getAnswers().stream()
			.filter(answer -> answer.getQuestion().getId().equals(question.getId()))
			.map(answer -> answer.getAnswerOption().getId())
			.forEach(result::add);
		return result;
	}

	private Set<UUID> correctOptionIds(QuestionEntity question) {
		var result = new HashSet<UUID>();
		question.getOptions().stream().filter(AnswerOptionEntity::isCorrect).map(AnswerOptionEntity::getId).forEach(result::add);
		return result;
	}

	private double scorePercent(TestAttemptEntity attempt) {
		return attempt.getMaxScore() == null || attempt.getMaxScore() == 0 ? 0 : attempt.getScore() * 100.0 / attempt.getMaxScore();
	}

	private double round(double value) {
		return Math.round(value * 10.0) / 10.0;
	}

	private TestEntity publishedTest(UUID id) {
		var test = tests.findById(id).orElseThrow(() -> notFound("Тест не найден"));
		if (test.getStatus() != TestStatus.PUBLISHED) {
			throw notFound("Тест не опубликован");
		}
		return test;
	}

	private TestEntity ownedTest(UUID id) {
		var test = tests.findById(id).orElseThrow(() -> notFound("Тест не найден"));
		assertOwner(test);
		return test;
	}

	private void assertOwner(TestEntity test) {
		var user = currentUser();
		if (!isAdmin(user) && (test.getCreatedBy() == null || !test.getCreatedBy().getId().equals(user.getId()))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можно редактировать только свои тесты");
		}
	}

	private boolean isAdmin(UserAccount user) {
		return user.getRoles().stream().anyMatch(role -> role.getName() == Role.ADMIN);
	}

	private boolean hasRole(UserAccount user, Role role) {
		return user.getRoles().stream().anyMatch(item -> item.getName() == role);
	}

	private UserAccount currentUser() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход");
		}
		return users.findByEmailIgnoreCase(authentication.getName())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
	}

	private ResponseStatusException notFound(String message) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
	}
}
