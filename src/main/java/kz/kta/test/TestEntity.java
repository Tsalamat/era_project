package kz.kta.test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import kz.kta.auth.UserAccount;
import kz.kta.common.TestStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "KtaTest")
@Table(name = "tests")
public class TestEntity {

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

	@Column(name = "time_limit_minutes", nullable = false)
	private int timeLimitMinutes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TestStatus status = TestStatus.DRAFT;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private UserAccount createdBy;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orderNumber asc")
	private List<QuestionEntity> questions = new ArrayList<>();

	protected TestEntity() {
	}

	public TestEntity(String title, String slug, String description, String subject, int timeLimitMinutes, UserAccount createdBy) {
		this.id = UUID.randomUUID();
		update(title, slug, description, subject, timeLimitMinutes);
		this.createdBy = createdBy;
	}

	@PrePersist
	void onCreate() {
		var now = Instant.now();
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public void update(String title, String slug, String description, String subject, int timeLimitMinutes) {
		this.title = title;
		this.slug = slug;
		this.description = description;
		this.subject = subject;
		this.timeLimitMinutes = timeLimitMinutes;
	}

	public void setStatus(TestStatus status) {
		this.status = status;
	}

	public void addQuestion(QuestionEntity question) {
		questions.add(question);
	}

	public UUID getId() { return id; }
	public String getTitle() { return title; }
	public String getSlug() { return slug; }
	public String getDescription() { return description; }
	public String getSubject() { return subject; }
	public int getTimeLimitMinutes() { return timeLimitMinutes; }
	public TestStatus getStatus() { return status; }
	public UserAccount getCreatedBy() { return createdBy; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public List<QuestionEntity> getQuestions() { return questions; }
}
