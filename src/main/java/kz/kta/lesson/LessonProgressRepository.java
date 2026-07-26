package kz.kta.lesson;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity, UUID> {

	boolean existsByUserIdAndLessonId(UUID userId, UUID lessonId);

	long countByUserId(UUID userId);

	@Query("select p.lesson.id from LessonProgressEntity p where p.user.id = :userId")
	Set<UUID> completedLessonIds(@Param("userId") UUID userId);

	@Query("select count(p) from LessonProgressEntity p where p.user.id = :userId and p.lesson.module.course.id = :courseId")
	long completedInCourse(@Param("userId") UUID userId, @Param("courseId") UUID courseId);
}
