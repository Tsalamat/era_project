package kz.kta.course;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public final class CourseDtos {
	private CourseDtos() { }

	public record CourseSummary(
		UUID id, String slug, String title, String subject, String description, String cover,
		int lessonsCount, int completedLessons, int progressPercent
	) { }

	public record CourseDetails(
		UUID id, String slug, String title, String subject, String description, String cover,
		int lessonsCount, int completedLessons, int progressPercent, List<CourseModule> modules
	) { }

	public record CourseModule(UUID id, String title, int orderNumber, List<LessonSummary> lessons) { }

	public record LessonSummary(UUID id, String title, int minutes, int orderNumber, boolean completed) { }

	public record LessonDetails(
		UUID id, UUID courseId, String courseSlug, String courseTitle, String moduleTitle,
		String title, String content, String videoUrl, int minutes, boolean completed
	) { }

	public record CourseProgress(UUID courseId, int totalLessons, int completedLessons, int percent) { }

	public record LearningSummary(int totalLessons, int completedLessons, int progressPercent) { }

	public record CourseRequest(
		@NotBlank String title, @NotBlank String slug, String description,
		@NotBlank String subject, String cover
	) { }
}
