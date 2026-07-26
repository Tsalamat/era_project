package kz.kta.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {
	List<CourseEntity> findByStatusOrderByCreatedAtAsc(String status);
	Optional<CourseEntity> findBySlugAndStatus(String slug, String status);
	boolean existsBySlug(String slug);
	boolean existsBySlugAndIdNot(String slug, UUID id);
}
