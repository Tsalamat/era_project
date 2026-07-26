package kz.kta.auth;

import kz.kta.common.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class RegistrationVerificationService {
	private static final Logger log = LoggerFactory.getLogger(RegistrationVerificationService.class);
	private static final int MAX_ATTEMPTS = 5;

	private final UserAccountRepository users;
	private final RoleRepository roles;
	private final RegistrationVerificationCodeRepository codes;
	private final PasswordEncoder passwordEncoder;
	private final JavaMailSender mailSender;
	private final SecureRandom secureRandom = new SecureRandom();
	private final boolean emailEnabled;
	private final String from;
	private final Duration codeTtl;
	private final String devCode;

	public RegistrationVerificationService(
		UserAccountRepository users,
		RoleRepository roles,
		RegistrationVerificationCodeRepository codes,
		PasswordEncoder passwordEncoder,
		ObjectProvider<JavaMailSender> mailSender,
		@Value("${kta.email-verification.enabled:false}") boolean emailEnabled,
		@Value("${kta.email-verification.from:}") String from,
		@Value("${kta.email-verification.code-ttl-minutes:10}") long codeTtlMinutes,
		@Value("${kta.email-verification.dev-code:}") String devCode
	) {
		this.users = users;
		this.roles = roles;
		this.codes = codes;
		this.passwordEncoder = passwordEncoder;
		this.mailSender = mailSender.getIfAvailable();
		this.emailEnabled = emailEnabled;
		this.from = from == null ? "" : from.trim();
		this.codeTtl = Duration.ofMinutes(codeTtlMinutes);
		this.devCode = devCode == null ? "" : devCode.trim();
	}

	@Transactional
	public void requestCode(String fullName, String email, String password) {
		var normalizedEmail = normalizeEmail(email);
		if (users.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже зарегистрирован");
		}
		codes.deleteByExpiresAtBefore(Instant.now());
		var code = verificationCode();
		var passwordHash = passwordEncoder.encode(password);
		var hash = codeHash(normalizedEmail, code);
		var expiresAt = Instant.now().plus(codeTtl);
		var entity = codes.findByEmailIgnoreCase(normalizedEmail)
			.orElseGet(() -> new RegistrationVerificationCodeEntity(normalizedEmail, fullName.trim(), passwordHash, hash, expiresAt));
		entity.refresh(fullName.trim(), passwordHash, hash, expiresAt);
		codes.save(entity);
		sendCode(normalizedEmail, code);
	}

	@Transactional
	public UserAccount verify(String email, String code) {
		var normalizedEmail = normalizeEmail(email);
		var entity = codes.findByEmailIgnoreCase(normalizedEmail)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Код не найден или уже истек"));
		if (Instant.now().isAfter(entity.getExpiresAt())) {
			codes.deleteByEmailIgnoreCase(normalizedEmail);
			throw new ResponseStatusException(HttpStatus.GONE, "Код истек, запросите новый");
		}
		if (entity.getAttempts() >= MAX_ATTEMPTS) {
			codes.deleteByEmailIgnoreCase(normalizedEmail);
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Слишком много неверных попыток, запросите новый код");
		}
		if (!MessageDigest.isEqual(entity.getCodeHash().getBytes(StandardCharsets.UTF_8), codeHash(normalizedEmail, code).getBytes(StandardCharsets.UTF_8))) {
			entity.incrementAttempts();
			codes.save(entity);
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Неверный код подтверждения");
		}
		if (users.existsByEmailIgnoreCase(normalizedEmail)) {
			codes.deleteByEmailIgnoreCase(normalizedEmail);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже зарегистрирован");
		}
		var studentRole = roles.findByName(Role.STUDENT)
			.orElseThrow(() -> new IllegalStateException("Роль STUDENT не настроена"));
		var account = new UserAccount(entity.getFullName(), normalizedEmail, entity.getPasswordHash());
		account.addRole(studentRole);
		users.save(account);
		codes.deleteByEmailIgnoreCase(normalizedEmail);
		return account;
	}

	private void sendCode(String email, String code) {
		if (!emailEnabled) {
			log.info("Email verification code for {} is {}", email, code);
			return;
		}
		if (from.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email отправитель не настроен");
		}
		if (mailSender == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP не настроен");
		}
		var message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(email);
		message.setSubject("Код подтверждения Тест Магистратура");
		message.setText("""
			Ваш код подтверждения Тест Магистратура: %s

			Код действует %d минут. Если вы не регистрировались на сайте, просто проигнорируйте письмо.
			""".formatted(code, codeTtl.toMinutes()));
		try {
			mailSender.send(message);
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Не удалось отправить код на email", exception);
		}
	}

	private String verificationCode() {
		if (!devCode.isBlank()) return devCode;
		return "%06d".formatted(secureRandom.nextInt(1_000_000));
	}

	private String codeHash(String email, String code) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest((email.toLowerCase() + ":" + code.trim()).getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Не удалось проверить код", exception);
		}
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email обязателен");
		}
		return email.trim().toLowerCase();
	}
}
