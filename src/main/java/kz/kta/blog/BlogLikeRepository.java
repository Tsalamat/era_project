package kz.kta.blog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogLikeRepository extends JpaRepository<BlogLikeEntity, UUID> {
	Optional<BlogLikeEntity> findByPostIdAndUserId(UUID postId, UUID userId);
	long countByPostId(UUID postId);
	boolean existsByPostIdAndUserId(UUID postId, UUID userId);
}
