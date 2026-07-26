package kz.kta.blog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BlogCommentRepository extends JpaRepository<BlogCommentEntity, UUID> {
	List<BlogCommentEntity> findByPostIdOrderByCreatedAtAsc(UUID postId);
	long countByPostId(UUID postId);
}
