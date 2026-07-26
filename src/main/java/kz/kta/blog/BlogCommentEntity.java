package kz.kta.blog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import kz.kta.auth.UserAccount;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blog_comments")
public class BlogCommentEntity {
	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "post_id", nullable = false)
	private BlogPostEntity post;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BlogCommentEntity() {
	}

	public BlogCommentEntity(BlogPostEntity post, UserAccount user, String content) {
		this.id = UUID.randomUUID();
		this.post = post;
		this.user = user;
		this.content = content;
	}

	@PrePersist
	void onCreate() {
		var now = Instant.now();
		if (id == null) id = UUID.randomUUID();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public BlogPostEntity getPost() { return post; }
	public UserAccount getUser() { return user; }
	public String getContent() { return content; }
	public Instant getCreatedAt() { return createdAt; }
}
