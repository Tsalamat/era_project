package kz.kta.test;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestAssignmentRepository extends JpaRepository<TestAssignmentEntity, UUID> {
	Optional<TestAssignmentEntity> findByTestIdAndStudentId(UUID testId, UUID studentId);
	List<TestAssignmentEntity> findByTestIdOrderByAssignedAtDesc(UUID testId);
	List<TestAssignmentEntity> findByStudentIdOrderByAssignedAtDesc(UUID studentId);
}
