package kz.kta.test;

import jakarta.validation.Valid;
import kz.kta.common.ApiResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static kz.kta.test.TestDtos.*;

@RestController
@RequestMapping("/api")
public class TestController {

	private final TestService service;

	public TestController(TestService service) {
		this.service = service;
	}

	@GetMapping("/tests")
	List<TestSummary> publishedTests() {
		return service.publishedTests();
	}

	@GetMapping("/tests/{slug}")
	StudentTestDetails testBySlug(@PathVariable String slug) {
		return service.studentTest(slug);
	}

	@PostMapping("/tests/{id}/start")
	@ResponseStatus(HttpStatus.CREATED)
	TestAttempt start(@PathVariable UUID id) {
		return service.start(id);
	}

	@PostMapping("/tests/{id}/submit")
	TestResult submit(@PathVariable UUID id, @Valid @RequestBody SubmitTestRequest request) {
		return service.submit(id, request);
	}

	@GetMapping("/tests/{id}/result")
	TestResult result(@PathVariable UUID id) {
		return service.latestResult(id);
	}

	@GetMapping("/tests/{id}/result/docx")
	ResponseEntity<byte[]> resultReport(@PathVariable UUID id) {
		return wordFile(service.latestResultReport(id));
	}

	@PostMapping("/teacher/tests")
	@ResponseStatus(HttpStatus.CREATED)
	TeacherTestDetails createTeacherTest(@Valid @RequestBody TeacherTestRequest request) {
		return service.create(request);
	}

	@GetMapping("/teacher/tests")
	List<TestSummary> teacherTests() {
		return service.teacherTests();
	}

	@GetMapping("/teacher/tests/{id}")
	TeacherTestDetails teacherTest(@PathVariable UUID id) {
		return service.teacherTest(id);
	}

	@PatchMapping("/teacher/tests/{id}")
	TeacherTestDetails updateTeacherTest(@PathVariable UUID id, @Valid @RequestBody TeacherTestRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/teacher/tests/{id}")
	ApiResponse deleteTeacherTest(@PathVariable UUID id) {
		service.delete(id);
		return ApiResponse.accepted("Тест удален");
	}

	@PostMapping("/teacher/tests/{id}/questions")
	@ResponseStatus(HttpStatus.CREATED)
	TeacherQuestion addQuestion(@PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
		return service.addQuestion(id, request);
	}

	@PostMapping(value = "/teacher/tests/{id}/questions/import-docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	QuestionImportResult importQuestions(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
		return service.importQuestions(id, file);
	}

	@PatchMapping("/teacher/questions/{id}")
	TeacherQuestion updateQuestion(@PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
		return service.updateQuestion(id, request);
	}

	@DeleteMapping("/teacher/questions/{id}")
	ApiResponse deleteQuestion(@PathVariable UUID id) {
		service.deleteQuestion(id);
		return ApiResponse.accepted("Вопрос удален");
	}

	@PostMapping("/teacher/tests/{id}/publish")
	TeacherTestDetails publish(@PathVariable UUID id) {
		return service.publish(id, true);
	}

	@PostMapping("/teacher/tests/{id}/unpublish")
	TeacherTestDetails unpublish(@PathVariable UUID id) {
		return service.publish(id, false);
	}

	@GetMapping("/teacher/tests/{id}/results")
	TeacherTestStats stats(@PathVariable UUID id) {
		return service.stats(id);
	}

	@GetMapping("/teacher/tests/{id}/results/docx")
	ResponseEntity<byte[]> teacherResultsReport(@PathVariable UUID id) {
		return wordFile(service.teacherResultsReport(id));
	}

	@GetMapping("/teacher/tests/{id}/assignments")
	List<TestAssignmentDto> testAssignments(@PathVariable UUID id) {
		return service.teacherAssignments(id);
	}

	@PostMapping("/teacher/tests/{id}/assignments")
	List<TestAssignmentDto> assignTest(@PathVariable UUID id, @Valid @RequestBody AssignTestRequest request) {
		return service.assignTest(id, request);
	}

	private ResponseEntity<byte[]> wordFile(TestReportFile file) {
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
			.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
			.contentLength(file.content().length)
			.body(file.content());
	}
}
