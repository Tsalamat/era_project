package kz.kta.blog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPostEntity, UUID> {
	List<BlogPostEntity> findByStatusOrderByCreatedAtDesc(String status);
	List<BlogPostEntity> findAllByOrderByUpdatedAtDesc();
	List<BlogPostEntity> findByAuthorIdOrderByUpdatedAtDesc(UUID authorId);
	Optional<BlogPostEntity> findBySlugAndStatus(String slug, String status);
	boolean existsBySlug(String slug);
	boolean existsBySlugAndIdNot(String slug, UUID id);
}
