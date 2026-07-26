package kz.kta.test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "answer_options")
public class AnswerOptionEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private QuestionEntity question;

	@Column(name = "option_text", nullable = false, columnDefinition = "text")
	private String optionText;

	@Column(name = "is_correct", nullable = false)
	private boolean correct;

	@Column(name = "order_number", nullable = false)
	private int orderNumber;

	protected AnswerOptionEntity() {
	}

	public AnswerOptionEntity(QuestionEntity question, String optionText, boolean correct, int orderNumber) {
		this.id = UUID.randomUUID();
		this.question = question;
		this.optionText = optionText;
		this.correct = correct;
		this.orderNumber = orderNumber;
	}

	@PrePersist
	void onCreate() { if (id == null) id = UUID.randomUUID(); }

	public void update(String optionText, boolean correct, int orderNumber) {
		this.optionText = optionText;
		this.correct = correct;
		this.orderNumber = orderNumber;
	}

	public UUID getId() { return id; }
	public QuestionEntity getQuestion() { return question; }
	public String getOptionText() { return optionText; }
	public boolean isCorrect() { return correct; }
	public int getOrderNumber() { return orderNumber; }
}
