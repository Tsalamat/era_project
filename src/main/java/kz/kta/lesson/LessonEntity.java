package kz.kta.lesson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import kz.kta.course.CourseModuleEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lessons")
public class LessonEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "module_id", nullable = false)
	private CourseModuleEntity module;

	@Column(nullable = false, length = 220)
	private String title;

	@Column(name = "video_url", length = 500)
	private String videoUrl;

	@Column(columnDefinition = "text")
	private String content;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Column(name = "order_number", nullable = false)
	private int orderNumber;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected LessonEntity() {
	}

	public LessonEntity(CourseModuleEntity module, String title, String content, int durationMinutes, int orderNumber) {
		this.id = UUID.randomUUID();
		this.module = module;
		this.title = title;
		this.content = content;
		this.durationMinutes = durationMinutes;
		this.orderNumber = orderNumber;
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

	public UUID getId() { return id; }
	public CourseModuleEntity getModule() { return module; }
	public String getTitle() { return title; }
	public String getVideoUrl() { return videoUrl; }
	public String getContent() { return content; }
	public int getDurationMinutes() { return durationMinutes; }
	public int getOrderNumber() { return orderNumber; }
}
