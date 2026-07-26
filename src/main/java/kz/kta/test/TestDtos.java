package kz.kta.test;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kz.kta.common.QuestionType;
import kz.kta.common.TestStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TestDtos {

	private TestDtos() {
	}

	public record TestSummary(
		UUID id,
		String slug,
		String title,
		String subject,
		int timeLimitMinutes,
		TestStatus status,
		long attempts,
		double averageScore
	) { }

	public record StudentTestDetails(
		UUID id,
		String slug,
		String title,
		String subject,
		String description,
		int timeLimitMinutes,
		List<StudentQuestion> questions
	) { }

	public record StudentQuestion(
		UUID id,
		String questionText,
		QuestionType questionType,
		int orderNumber,
		List<StudentOption> options
	) { }

	public record StudentOption(UUID id, String optionText, int orderNumber) { }

	public record TeacherTestDetails(
		UUID id,
		String slug,
		String title,
		String subject,
		String description,
		int timeLimitMinutes,
		TestStatus status,
		String createdBy,
		List<TeacherQuestion> questions
	) { }

	public record TeacherQuestion(
		UUID id,
		String questionText,
		QuestionType questionType,
		String explanation,
		int orderNumber,
		List<TeacherOption> options
	) { }

	public record TeacherOption(UUID id, String optionText, boolean isCorrect, int orderNumber) { }

	public record TeacherTestRequest(
		@NotBlank String title,
		@NotBlank String slug,
		String description,
		@NotBlank String subject,
		@Positive int timeLimitMinutes
	) { }

	public record QuestionRequest(
		@NotBlank String questionText,
		@NotNull QuestionType questionType,
		String explanation,
		@Positive int orderNumber,
		@Valid @NotNull List<AnswerOptionRequest> options
	) { }

	public record AnswerOptionRequest(UUID id, @NotBlank String optionText, boolean isCorrect, @Positive int orderNumber) { }

	public record QuestionImportResult(int importedCount, List<TeacherQuestion> questions) { }

	public record SubmitTestRequest(@NotNull UUID attemptId, @NotNull List<SubmittedAnswer> answers) { }

	public record SubmittedAnswer(@NotNull UUID questionId, @NotNull List<UUID> answerOptionIds) { }

	public record TestAttempt(UUID id, UUID testId, Instant startedAt, String status) { }

	public record TestResult(
		UUID testId,
		String testTitle,
		UUID attemptId,
		int score,
		int maxScore,
		double scorePercent,
		Instant completedAt,
		TestAnalytics analytics,
		List<QuestionResult> questions
	) { }

	public record TestAnalytics(
		int totalQuestions,
		int answeredQuestions,
		int correctAnswers,
		int wrongAnswers,
		int skippedQuestions,
		double accuracyPercent,
		long durationSeconds,
		double durationMinutes,
		String level,
		String recommendation
	) { }

	public record TestReportFile(String filename, byte[] content) { }

	public record QuestionResult(
		UUID questionId,
		boolean correct,
		List<UUID> selectedOptionIds,
		List<UUID> correctOptionIds,
		String explanation
	) { }

	public record TeacherTestStats(
		UUID testId,
		long attemptsCount,
		double averageScorePercent,
		double averageTimeMinutes,
		List<HardQuestion> hardestQuestions,
		List<StudentAttemptResult> students
	) { }

	public record HardQuestion(UUID questionId, String questionText, double wrongPercent) { }

	public record StudentAttemptResult(
		UUID attemptId,
		String studentName,
		String email,
		int score,
		int maxScore,
		double scorePercent,
		int answeredQuestions,
		int skippedQuestions,
		double durationMinutes,
		Instant completedAt
	) { }

	public record AssignTestRequest(@NotNull @Size(min = 1) List<UUID> studentIds) { }

	public record TestAssignmentDto(
		UUID id,
		UUID testId,
		String testTitle,
		String testSlug,
		String studentName,
		String studentEmail,
		String assignedBy,
		String status,
		Instant assignedAt,
		Instant completedAt
	) { }

	public record StudentAssignedTest(
		UUID assignmentId,
		UUID testId,
		String slug,
		String title,
		String subject,
		int timeLimitMinutes,
		String status,
		Instant assignedAt,
		Instant completedAt
	) { }
}
