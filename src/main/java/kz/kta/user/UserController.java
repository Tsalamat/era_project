package kz.kta.user;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kz.kta.auth.RoleEntity;
import kz.kta.auth.RoleRepository;
import kz.kta.auth.UserAccount;
import kz.kta.auth.UserAccountRepository;
import kz.kta.common.ApiResponse;
import kz.kta.common.Role;
import kz.kta.notification.NotificationEntity;
import kz.kta.notification.NotificationRepository;
import kz.kta.test.TestDtos.StudentAssignedTest;
import kz.kta.test.TestDtos.TestResult;
import kz.kta.test.TestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UserController {

	private final UserAccountRepository users;
	private final RoleRepository roles;
	private final TestService testService;
	private final NotificationRepository notifications;

	public UserController(UserAccountRepository users, RoleRepository roles, TestService testService, NotificationRepository notifications) {
		this.users = users;
		this.roles = roles;
		this.testService = testService;
		this.notifications = notifications;
	}

	@GetMapping("/users/me")
	UserProfile me(Authentication authentication) {
		return profile(account(authentication));
	}

	@PatchMapping("/users/me")
	@Transactional
	UserProfile updateMe(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
		var account = account(authentication);
		account.setFullName(request.fullName().trim());
		return profile(users.save(account));
	}

	@GetMapping("/users/me/results")
	List<TestResult> myResults() {
		return testService.resultsForCurrentUser();
	}

	@GetMapping("/users/me/assignments")
	List<StudentAssignedTest> myAssignments() {
		return testService.assignedTestsForCurrentUser();
	}

	@GetMapping("/users/me/notifications")
	List<UserNotification> myNotifications(Authentication authentication) {
		var account = account(authentication);
		return notifications.findByUserIdOrderByCreatedAtDesc(account.getId()).stream().map(this::notificationDto).toList();
	}

	@PostMapping("/users/me/notifications/{id}/read")
	@Transactional
	ApiResponse readNotification(Authentication authentication, @PathVariable UUID id) {
		var account = account(authentication);
		var notification = notifications.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Уведомление не найдено"));
		if (!notification.getUser().getId().equals(account.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Уведомление принадлежит другому пользователю");
		}
		notification.markRead();
		notifications.save(notification);
		return ApiResponse.accepted("Уведомление прочитано");
	}

	@PostMapping("/users/me/notifications/read-all")
	@Transactional
	ApiResponse readAllNotifications(Authentication authentication) {
		var account = account(authentication);
		var items = notifications.findByUserIdOrderByCreatedAtDesc(account.getId());
		items.forEach(NotificationEntity::markRead);
		notifications.saveAll(items);
		return ApiResponse.accepted("Уведомления прочитаны");
	}

	@GetMapping("/teacher/students")
	List<UserProfile> students() {
		return users.findByRole(Role.STUDENT).stream().map(this::profile).toList();
	}

	@GetMapping("/admin/users")
	List<UserProfile> users() {
		return users.findAll().stream().map(this::profile).toList();
	}

	@PatchMapping("/admin/users/{id}/role")
	@Transactional
	ApiResponse assignRole(@PathVariable UUID id, @Valid @RequestBody AssignRoleRequest request) {
		var account = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
		var role = roles.findByName(request.role()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Роль не найдена"));
		account.addRole(role);
		users.save(account);
		return ApiResponse.accepted("Роль " + request.role() + " назначена");
	}

	private UserAccount account(Authentication authentication) {
		return users.findByEmailIgnoreCase(authentication.getName())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
	}

	private UserProfile profile(UserAccount account) {
		return new UserProfile(
			account.getId(), account.getFullName(), account.getEmail(),
			account.getRoles().stream().map(RoleEntity::getName).sorted().toList()
		);
	}

	private UserNotification notificationDto(NotificationEntity notification) {
		return new UserNotification(
			notification.getId(), notification.getType(), notification.getTitle(), notification.getMessage(),
			notification.getLink(), notification.getReadAt() != null, notification.getCreatedAt()
		);
	}

	public record UserProfile(UUID id, String fullName, String email, List<Role> roles) { }

	public record UserNotification(UUID id, String type, String title, String message, String link, boolean read, Instant createdAt) { }

	public record UpdateProfileRequest(@NotBlank String fullName) { }

	public record AssignRoleRequest(@NotNull Role role) { }
}
