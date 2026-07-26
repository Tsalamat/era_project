package kz.kta.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.kta.common.ApiResponse;
import kz.kta.common.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final UserAccountRepository users;
	private final UserDetailsService userDetailsService;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final RegistrationVerificationService registrationVerificationService;

	public AuthController(
		UserAccountRepository users,
		UserDetailsService userDetailsService,
		AuthenticationManager authenticationManager,
		JwtService jwtService,
		RegistrationVerificationService registrationVerificationService
	) {
		this.users = users;
		this.userDetailsService = userDetailsService;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.registrationVerificationService = registrationVerificationService;
	}

	@PostMapping("/register")
	ApiResponse register(@Valid @RequestBody RegisterRequest request) {
		registrationVerificationService.requestCode(request.fullName(), request.email(), request.password());
		return ApiResponse.accepted("Код подтверждения отправлен на email");
	}

	@PostMapping("/register/request-code")
	ApiResponse requestRegistrationCode(@Valid @RequestBody RegisterRequest request) {
		return register(request);
	}

	@PostMapping("/register/verify")
	AuthTokens verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest request) {
		var account = registrationVerificationService.verify(request.email(), request.code());
		return tokens(userDetailsService.loadUserByUsername(account.getEmail()), account);
	}

	@PostMapping("/login")
	AuthTokens login(@Valid @RequestBody LoginRequest request) {
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		var details = userDetailsService.loadUserByUsername(request.email());
		var account = users.findByEmailIgnoreCase(request.email()).orElseThrow();
		return tokens(details, account);
	}

	@PostMapping("/refresh")
	AuthTokens refresh(@Valid @RequestBody RefreshRequest request) {
		var email = jwtService.subject(request.refreshToken(), "refresh");
		var details = userDetailsService.loadUserByUsername(email);
		var account = users.findByEmailIgnoreCase(email).orElseThrow();
		return tokens(details, account);
	}

	@PostMapping("/logout")
	ApiResponse logout() {
		return ApiResponse.accepted("Сессия завершена");
	}

	private AuthTokens tokens(UserDetails details, UserAccount account) {
		var primaryRole = account.getRoles().stream()
			.map(RoleEntity::getName)
			.sorted((left, right) -> Integer.compare(priority(right), priority(left)))
			.findFirst()
			.orElse(Role.STUDENT);
		return new AuthTokens(jwtService.accessToken(details), jwtService.refreshToken(details), primaryRole, account.getFullName(), account.getEmail());
	}

	private int priority(Role role) {
		return switch (role) {
			case ADMIN -> 3;
			case TEACHER -> 2;
			case STUDENT -> 1;
		};
	}


	public record RegisterRequest(
		@NotBlank String fullName,
		@Email @NotBlank String email,
		@Size(min = 8) String password
	) {
	}

	public record VerifyRegistrationRequest(@Email @NotBlank String email, @NotBlank @Size(min = 4, max = 12) String code) {
	}

	public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
	}

	public record RefreshRequest(@NotBlank String refreshToken) {
	}

	public record AuthTokens(String accessToken, String refreshToken, Role role, String fullName, String email) {
	}
}
