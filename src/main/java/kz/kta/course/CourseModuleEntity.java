package kz.kta.course;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import kz.kta.lesson.LessonEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "course_modules")
public class CourseModuleEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private CourseEntity course;

	@Column(nullable = false, length = 220)
	private String title;

	@Column(name = "order_number", nullable = false)
	private int orderNumber;

	@OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orderNumber asc")
	private List<LessonEntity> lessons = new ArrayList<>();

	protected CourseModuleEntity() {
	}

	public CourseModuleEntity(CourseEntity course, String title, int orderNumber) {
		this.id = UUID.randomUUID();
		this.course = course;
		this.title = title;
		this.orderNumber = orderNumber;
	}

	@PrePersist
	void onCreate() { if (id == null) id = UUID.randomUUID(); }

	public void addLesson(LessonEntity lesson) { lessons.add(lesson); }

	public UUID getId() { return id; }
	public CourseEntity getCourse() { return course; }
	public String getTitle() { return title; }
	public int getOrderNumber() { return orderNumber; }
	public List<LessonEntity> getLessons() { return lessons; }
}
