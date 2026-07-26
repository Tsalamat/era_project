package kz.kta.course;

import jakarta.validation.Valid;
import kz.kta.common.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static kz.kta.course.CourseDtos.*;

@RestController
@RequestMapping("/api")
public class CourseController {
	private final CourseService service;

	public CourseController(CourseService service) { this.service = service; }

	@GetMapping("/courses")
	List<CourseSummary> courses() { return service.publishedCourses(); }

	@GetMapping("/courses/{slug}")
	CourseDetails course(@PathVariable String slug) { return service.publishedCourse(slug); }

	@GetMapping("/users/me/learning-summary")
	LearningSummary learningSummary() { return service.learningSummary(); }

	@PostMapping("/admin/courses")
	CourseDetails createCourse(@Valid @RequestBody CourseRequest request) { return service.create(request); }

	@PatchMapping("/admin/courses/{id}")
	CourseDetails updateCourse(@PathVariable UUID id, @Valid @RequestBody CourseRequest request) { return service.update(id, request); }

	@DeleteMapping("/admin/courses/{id}")
	ApiResponse deleteCourse(@PathVariable UUID id) {
		service.delete(id);
		return ApiResponse.accepted("Курс удален");
	}
}
