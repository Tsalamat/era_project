package kz.kta.blog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import kz.kta.auth.UserAccount;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blog_likes")
public class BlogLikeEntity {
	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "post_id", nullable = false)
	private BlogPostEntity post;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected BlogLikeEntity() {
	}

	public BlogLikeEntity(BlogPostEntity post, UserAccount user) {
		this.id = UUID.randomUUID();
		this.post = post;
		this.user = user;
	}

	@PrePersist
	void onCreate() {
		if (id == null) id = UUID.randomUUID();
		createdAt = Instant.now();
	}

	public UUID getId() { return id; }
}
