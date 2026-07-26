package kz.kta.test;

import kz.kta.common.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestRepository extends JpaRepository<TestEntity, UUID> {

	List<TestEntity> findByStatusOrderByCreatedAtDesc(TestStatus status);

	List<TestEntity> findByCreatedByIdOrderByUpdatedAtDesc(UUID createdBy);

	Optional<TestEntity> findBySlugAndStatus(String slug, TestStatus status);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, UUID id);
}
