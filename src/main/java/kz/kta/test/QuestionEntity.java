package kz.kta.test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import kz.kta.common.QuestionType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class QuestionEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "test_id", nullable = false)
	private TestEntity test;

	@Column(name = "question_text", nullable = false, columnDefinition = "text")
	private String questionText;

	@Enumerated(EnumType.STRING)
	@Column(name = "question_type", nullable = false, length = 32)
	private QuestionType questionType;

	@Column(columnDefinition = "text")
	private String explanation;

	@Column(name = "order_number", nullable = false)
	private int orderNumber;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orderNumber asc")
	private List<AnswerOptionEntity> options = new ArrayList<>();

	protected QuestionEntity() {
	}

	public QuestionEntity(TestEntity test, String questionText, QuestionType questionType, String explanation, int orderNumber) {
		this.id = UUID.randomUUID();
		this.test = test;
		update(questionText, questionType, explanation, orderNumber);
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

	public void update(String questionText, QuestionType questionType, String explanation, int orderNumber) {
		this.questionText = questionText;
		this.questionType = questionType;
		this.explanation = explanation;
		this.orderNumber = orderNumber;
	}

	public void replaceOptions(List<AnswerOptionEntity> options) {
		this.options.clear();
		this.options.addAll(options);
	}

	public UUID getId() { return id; }
	public TestEntity getTest() { return test; }
	public String getQuestionText() { return questionText; }
	public QuestionType getQuestionType() { return questionType; }
	public String getExplanation() { return explanation; }
	public int getOrderNumber() { return orderNumber; }
	public List<AnswerOptionEntity> getOptions() { return options; }
}
