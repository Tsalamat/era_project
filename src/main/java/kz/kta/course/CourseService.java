package kz.kta.course;

import kz.kta.auth.UserAccount;
import kz.kta.auth.UserAccountRepository;
import kz.kta.lesson.LessonEntity;
import kz.kta.lesson.LessonProgressEntity;
import kz.kta.lesson.LessonProgressRepository;
import kz.kta.lesson.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static kz.kta.course.CourseDtos.*;

@Service
public class CourseService {
	private static final String PUBLISHED = "PUBLISHED";

	private final CourseRepository courses;
	private final LessonRepository lessons;
	private final LessonProgressRepository progress;
	private final UserAccountRepository users;

	public CourseService(CourseRepository courses, LessonRepository lessons, LessonProgressRepository progress, UserAccountRepository users) {
		this.courses = courses;
		this.lessons = lessons;
		this.progress = progress;
		this.users = users;
	}

	@Transactional(readOnly = true)
	public List<CourseSummary> publishedCourses() {
		var completedIds = currentUser().map(user -> progress.completedLessonIds(user.getId())).orElse(Set.of());
		return courses.findByStatusOrderByCreatedAtAsc(PUBLISHED).stream()
			.map(course -> summary(course, completedIds))
			.toList();
	}

	@Transactional(readOnly = true)
	public CourseDetails publishedCourse(String slug) {
		var course = courses.findBySlugAndStatus(slug, PUBLISHED).orElseThrow(() -> notFound("Курс не найден"));
		var completedIds = currentUser().map(user -> progress.completedLessonIds(user.getId())).orElse(Set.of());
		var lessonCount = lessons(course).size();
		var completedCount = (int) lessons(course).stream().filter(lesson -> completedIds.contains(lesson.getId())).count();
		var modules = course.getModules().stream().map(module -> new CourseModule(
			module.getId(), module.getTitle(), module.getOrderNumber(), module.getLessons().stream()
				.map(lesson -> new LessonSummary(lesson.getId(), lesson.getTitle(), lesson.getDurationMinutes(), lesson.getOrderNumber(), completedIds.contains(lesson.getId())))
				.toList()
		)).toList();
		return new CourseDetails(
			course.getId(), course.getSlug(), course.getTitle(), course.getSubject(), course.getDescription(), course.getCoverUrl(),
			lessonCount, completedCount, percent(completedCount, lessonCount), modules
		);
	}

	@Transactional(readOnly = true)
	public LessonDetails lesson(UUID id) {
		var lesson = lessons.findById(id).orElseThrow(() -> notFound("Урок не найден"));
		var user = requiredUser();
		return lessonDetails(lesson, progress.existsByUserIdAndLessonId(user.getId(), id));
	}

	@Transactional
	public LessonDetails completeLesson(UUID id) {
		var lesson = lessons.findById(id).orElseThrow(() -> notFound("Урок не найден"));
		var user = requiredUser();
		if (!progress.existsByUserIdAndLessonId(user.getId(), id)) {
			progress.save(new LessonProgressEntity(user, lesson));
		}
		return lessonDetails(lesson, true);
	}

	@Transactional(readOnly = true)
	public CourseProgress courseProgress(UUID courseId) {
		var course = courses.findById(courseId).orElseThrow(() -> notFound("Курс не найден"));
		var total = lessons(course).size();
		var completed = (int) progress.completedInCourse(requiredUser().getId(), courseId);
		return new CourseProgress(courseId, total, completed, percent(completed, total));
	}

	@Transactional(readOnly = true)
	public LearningSummary learningSummary() {
		var total = courses.findByStatusOrderByCreatedAtAsc(PUBLISHED).stream().mapToInt(course -> lessons(course).size()).sum();
		var completed = (int) progress.countByUserId(requiredUser().getId());
		return new LearningSummary(total, completed, percent(completed, total));
	}

	@Transactional
	public CourseDetails create(CourseRequest request) {
		if (courses.existsBySlug(request.slug().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug уже используется");
		var course = courses.save(new CourseEntity(request.title().trim(), request.slug().trim(), request.description(), request.subject().trim(), request.cover()));
		return detailsWithoutProgress(course);
	}

	@Transactional
	public CourseDetails update(UUID id, CourseRequest request) {
		var course = courses.findById(id).orElseThrow(() -> notFound("Курс не найден"));
		if (courses.existsBySlugAndIdNot(request.slug().trim(), id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug уже используется");
		course.update(request.title().trim(), request.slug().trim(), request.description(), request.subject().trim(), request.cover());
		return detailsWithoutProgress(courses.save(course));
	}

	@Transactional
	public void delete(UUID id) {
		if (!courses.existsById(id)) throw notFound("Курс не найден");
		courses.deleteById(id);
	}

	private CourseSummary summary(CourseEntity course, Set<UUID> completedIds) {
		var source = lessons(course);
		var completed = (int) source.stream().filter(lesson -> completedIds.contains(lesson.getId())).count();
		return new CourseSummary(
			course.getId(), course.getSlug(), course.getTitle(), course.getSubject(), course.getDescription(), course.getCoverUrl(),
			source.size(), completed, percent(completed, source.size())
		);
	}

	private CourseDetails detailsWithoutProgress(CourseEntity course) {
		var source = lessons(course);
		var modules = course.getModules().stream().map(module -> new CourseModule(
			module.getId(), module.getTitle(), module.getOrderNumber(), module.getLessons().stream()
				.map(lesson -> new LessonSummary(lesson.getId(), lesson.getTitle(), lesson.getDurationMinutes(), lesson.getOrderNumber(), false)).toList()
		)).toList();
		return new CourseDetails(course.getId(), course.getSlug(), course.getTitle(), course.getSubject(), course.getDescription(), course.getCoverUrl(), source.size(), 0, 0, modules);
	}

	private LessonDetails lessonDetails(LessonEntity lesson, boolean completed) {
		var course = lesson.getModule().getCourse();
		return new LessonDetails(
			lesson.getId(), course.getId(), course.getSlug(), course.getTitle(), lesson.getModule().getTitle(),
			lesson.getTitle(), lesson.getContent(), lesson.getVideoUrl(), lesson.getDurationMinutes(), completed
		);
	}

	private List<LessonEntity> lessons(CourseEntity course) {
		return course.getModules().stream().flatMap(module -> module.getLessons().stream()).toList();
	}

	private int percent(int completed, int total) { return total == 0 ? 0 : Math.round(completed * 100f / total); }

	private Optional<UserAccount> currentUser() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) return Optional.empty();
		return users.findByEmailIgnoreCase(authentication.getName());
	}

	private UserAccount requiredUser() {
		return currentUser().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход"));
	}

	private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
}
