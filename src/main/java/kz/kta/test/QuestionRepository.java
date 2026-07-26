package kz.kta.test;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuestionRepository extends JpaRepository<QuestionEntity, UUID> {
}
