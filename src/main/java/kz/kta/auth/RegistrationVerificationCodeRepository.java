package kz.kta.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationVerificationCodeRepository extends JpaRepository<RegistrationVerificationCodeEntity, UUID> {
	Optional<RegistrationVerificationCodeEntity> findByEmailIgnoreCase(String email);

	void deleteByEmailIgnoreCase(String email);

	long deleteByExpiresAtBefore(Instant threshold);
}
