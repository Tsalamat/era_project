package kz.kta.course;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class CourseEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 220)
	private String title;

	@Column(nullable = false, unique = true, length = 240)
	private String slug;

	@Column(columnDefinition = "text")
	private String description;

	@Column(nullable = false, length = 120)
	private String subject;

	@Column(name = "cover_url", length = 500)
	private String coverUrl;

	@Column(nullable = false, length = 32)
	private String status = "PUBLISHED";

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orderNumber asc")
	private List<CourseModuleEntity> modules = new ArrayList<>();

	protected CourseEntity() {
	}

	public CourseEntity(String title, String slug, String description, String subject, String coverUrl) {
		this.id = UUID.randomUUID();
		update(title, slug, description, subject, coverUrl);
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

	public void update(String title, String slug, String description, String subject, String coverUrl) {
		this.title = title;
		this.slug = slug;
		this.description = description;
		this.subject = subject;
		this.coverUrl = coverUrl;
	}

	public void addModule(CourseModuleEntity module) { modules.add(module); }

	public UUID getId() { return id; }
	public String getTitle() { return title; }
	public String getSlug() { return slug; }
	public String getDescription() { return description; }
	public String getSubject() { return subject; }
	public String getCoverUrl() { return coverUrl; }
	public String getStatus() { return status; }
	public List<CourseModuleEntity> getModules() { return modules; }
}
