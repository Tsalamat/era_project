package kz.kta.test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "test_attempt_answers")
public class TestAttemptAnswerEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_id", nullable = false)
	private TestAttemptEntity attempt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private QuestionEntity question;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "answer_option_id", nullable = false)
	private AnswerOptionEntity answerOption;

	protected TestAttemptAnswerEntity() {
	}

	public TestAttemptAnswerEntity(TestAttemptEntity attempt, QuestionEntity question, AnswerOptionEntity answerOption) {
		this.id = UUID.randomUUID();
		this.attempt = attempt;
		this.question = question;
		this.answerOption = answerOption;
	}

	@PrePersist
	void onCreate() { if (id == null) id = UUID.randomUUID(); }

	public QuestionEntity getQuestion() { return question; }
	public AnswerOptionEntity getAnswerOption() { return answerOption; }
}
