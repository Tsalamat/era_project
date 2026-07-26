package kz.kta.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registration_verification_codes")
public class RegistrationVerificationCodeEntity {
	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 180)
	private String email;

	@Column(name = "full_name", nullable = false, length = 180)
	private String fullName;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "code_hash", nullable = false, length = 128)
	private String codeHash;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected RegistrationVerificationCodeEntity() {
	}

	public RegistrationVerificationCodeEntity(String email, String fullName, String passwordHash, String codeHash, Instant expiresAt) {
		this.id = UUID.randomUUID();
		this.email = email.toLowerCase();
		this.fullName = fullName;
		this.passwordHash = passwordHash;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
	}

	public void refresh(String fullName, String passwordHash, String codeHash, Instant expiresAt) {
		this.fullName = fullName;
		this.passwordHash = passwordHash;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
		this.attempts = 0;
	}

	@PrePersist
	void onCreate() {
		if (id == null) id = UUID.randomUUID();
		createdAt = Instant.now();
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public int getAttempts() {
		return attempts;
	}

	public void incrementAttempts() {
		attempts += 1;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
