package kz.kta.test;

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
@Table(name = "test_assignments")
public class TestAssignmentEntity {
	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "test_id", nullable = false)
	private TestEntity test;

	@ManyToOne
	@JoinColumn(name = "student_id", nullable = false)
	private UserAccount student;

	@ManyToOne
	@JoinColumn(name = "assigned_by")
	private UserAccount assignedBy;

	@Column(name = "assigned_at", nullable = false)
	private Instant assignedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected TestAssignmentEntity() {
	}

	public TestAssignmentEntity(TestEntity test, UserAccount student, UserAccount assignedBy) {
		this.id = UUID.randomUUID();
		this.test = test;
		this.student = student;
		this.assignedBy = assignedBy;
	}

	@PrePersist
	void onCreate() {
		if (id == null) id = UUID.randomUUID();
		assignedAt = Instant.now();
	}

	public void markCompleted(Instant completedAt) {
		this.completedAt = completedAt;
	}

	public UUID getId() { return id; }
	public TestEntity getTest() { return test; }
	public UserAccount getStudent() { return student; }
	public UserAccount getAssignedBy() { return assignedBy; }
	public Instant getAssignedAt() { return assignedAt; }
	public Instant getCompletedAt() { return completedAt; }
}
