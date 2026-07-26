package kz.kta.lesson;

import kz.kta.course.CourseDtos.CourseProgress;
import kz.kta.course.CourseDtos.LessonDetails;
import kz.kta.course.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LessonController {
	private final CourseService service;

	public LessonController(CourseService service) { this.service = service; }

	@GetMapping("/lessons/{id}")
	LessonDetails lesson(@PathVariable UUID id) { return service.lesson(id); }

	@PostMapping("/lessons/{id}/complete")
	LessonDetails complete(@PathVariable UUID id) { return service.completeLesson(id); }

	@GetMapping("/courses/{id}/progress")
	CourseProgress progress(@PathVariable UUID id) { return service.courseProgress(id); }
}
