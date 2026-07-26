package kz.kta.test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import kz.kta.auth.UserAccount;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_attempts")
public class TestAttemptEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "test_id", nullable = false)
	private TestEntity test;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	private Integer score;

	@Column(name = "max_score")
	private Integer maxScore;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TestAttemptAnswerEntity> answers = new ArrayList<>();

	protected TestAttemptEntity() {
	}

	public TestAttemptEntity(TestEntity test, UserAccount user) {
		this.id = UUID.randomUUID();
		this.test = test;
		this.user = user;
	}

	@PrePersist
	void onCreate() {
		if (id == null) id = UUID.randomUUID();
		if (startedAt == null) startedAt = Instant.now();
	}

	public void addAnswer(QuestionEntity question, AnswerOptionEntity option) {
		answers.add(new TestAttemptAnswerEntity(this, question, option));
	}

	public void complete(int score, int maxScore) {
		this.score = score;
		this.maxScore = maxScore;
		this.completedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public TestEntity getTest() { return test; }
	public UserAccount getUser() { return user; }
	public Integer getScore() { return score; }
	public Integer getMaxScore() { return maxScore; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public List<TestAttemptAnswerEntity> getAnswers() { return answers; }
}
