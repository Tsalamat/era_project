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
@Table(name = "blog_posts")
public class BlogPostEntity {
	@Id
	private UUID id;

	@Column(nullable = false, length = 220)
	private String title;

	@Column(nullable = false, unique = true, length = 240)
	private String slug;

	@Column(columnDefinition = "text")
	private String excerpt;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(nullable = false, length = 120)
	private String category;

	@Column(name = "read_minutes", nullable = false)
	private int readMinutes;

	@Column(nullable = false, length = 32)
	private String status;

	@ManyToOne
	@JoinColumn(name = "author_id")
	private UserAccount author;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BlogPostEntity() { }

	public BlogPostEntity(String title, String slug, String excerpt, String content, String category, int readMinutes, String status) {
		this(title, slug, excerpt, content, category, readMinutes, status, null);
	}

	public BlogPostEntity(String title, String slug, String excerpt, String content, String category, int readMinutes, String status, UserAccount author) {
		this.id = UUID.randomUUID();
		this.author = author;
		update(title, slug, excerpt, content, category, readMinutes, status);
	}

	@PrePersist
	void onCreate() {
		var now = Instant.now();
		if (id == null) id = UUID.randomUUID();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() { updatedAt = Instant.now(); }

	public void update(String title, String slug, String excerpt, String content, String category, int readMinutes, String status) {
		this.title = title;
		this.slug = slug;
		this.excerpt = excerpt;
		this.content = content;
		this.category = category;
		this.readMinutes = readMinutes;
		this.status = status;
	}

	public UUID getId() { return id; }
	public String getTitle() { return title; }
	public String getSlug() { return slug; }
	public String getExcerpt() { return excerpt; }
	public String getContent() { return content; }
	public String getCategory() { return category; }
	public int getReadMinutes() { return readMinutes; }
	public String getStatus() { return status; }
	public UserAccount getAuthor() { return author; }
	public Instant getCreatedAt() { return createdAt; }
}
