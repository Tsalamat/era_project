package kz.kta.lesson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kz.kta.auth.UserAccount;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
public class LessonProgressEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lesson_id", nullable = false)
	private LessonEntity lesson;

	@Column(name = "completed_at", nullable = false)
	private Instant completedAt;

	protected LessonProgressEntity() {
	}

	public LessonProgressEntity(UserAccount user, LessonEntity lesson) {
		this.id = UUID.randomUUID();
		this.user = user;
		this.lesson = lesson;
	}

	@PrePersist
	void onCreate() {
		if (id == null) id = UUID.randomUUID();
		if (completedAt == null) completedAt = Instant.now();
	}
}
