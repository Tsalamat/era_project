package kz.kta.notification;

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
@Table(name = "notifications")
public class NotificationEntity {
	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@Column(nullable = false, length = 64)
	private String type;

	@Column(nullable = false, length = 220)
	private String title;

	@Column(columnDefinition = "text")
	private String message;

	@Column(length = 500)
	private String link;

	@Column(name = "read_at")
	private Instant readAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected NotificationEntity() {
	}

	public NotificationEntity(UserAccount user, String type, String title, String message, String link) {
		this.id = UUID.randomUUID();
		this.user = user;
		this.type = type;
		this.title = title;
		this.message = message;
		this.link = link;
	}

	@PrePersist
	void onCreate() {
		if (id == null) id = UUID.randomUUID();
		createdAt = Instant.now();
	}

	public void markRead() {
		if (readAt == null) readAt = Instant.now();
	}

	public UUID getId() { return id; }
	public UserAccount getUser() { return user; }
	public String getType() { return type; }
	public String getTitle() { return title; }
	public String getMessage() { return message; }
	public String getLink() { return link; }
	public Instant getReadAt() { return readAt; }
	public Instant getCreatedAt() { return createdAt; }
}
