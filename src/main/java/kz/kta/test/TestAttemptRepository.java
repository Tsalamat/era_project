package kz.kta.test;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestAttemptRepository extends JpaRepository<TestAttemptEntity, UUID> {

	Optional<TestAttemptEntity> findFirstByTestIdAndUserIdAndCompletedAtIsNullOrderByStartedAtDesc(UUID testId, UUID userId);

	Optional<TestAttemptEntity> findFirstByTestIdAndUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(UUID testId, UUID userId);

	List<TestAttemptEntity> findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(UUID userId);

	List<TestAttemptEntity> findByTestIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(UUID testId);
}
