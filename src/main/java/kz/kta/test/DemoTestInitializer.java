package kz.kta.test;

import kz.kta.auth.UserAccountRepository;
import kz.kta.common.QuestionType;
import kz.kta.common.TestStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "kta.demo-data.enabled", havingValue = "true")
@Order(20)
public class DemoTestInitializer implements ApplicationRunner {

	private final TestRepository tests;
	private final UserAccountRepository users;

	public DemoTestInitializer(TestRepository tests, UserAccountRepository users) {
		this.tests = tests;
		this.users = users;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (tests.existsBySlug("demo-kta")) {
			return;
		}
		var teacher = users.findByEmailIgnoreCase("teacher@kta.kz").orElseThrow();
		var test = new TestEntity(
			"Пробный КТА", "demo-kta", "Короткая диагностика по логике для проверки формата.", "Логика", 20, teacher
		);

		var first = new QuestionEntity(test, "Какое число продолжает ряд: 2, 4, 8, 16?", QuestionType.SINGLE_CHOICE, "Каждое следующее число в два раза больше предыдущего, поэтому ответ 32.", 1);
		first.replaceOptions(java.util.List.of(
			new AnswerOptionEntity(first, "18", false, 1),
			new AnswerOptionEntity(first, "24", false, 2),
			new AnswerOptionEntity(first, "32", true, 3),
			new AnswerOptionEntity(first, "64", false, 4)
		));
		test.addQuestion(first);

		var second = new QuestionEntity(test, "Все магистранты изучают методологию. Айжан - магистрант. Какой вывод верен?", QuestionType.SINGLE_CHOICE, "Из двух посылок прямо следует, что Айжан изучает методологию.", 2);
		second.replaceOptions(java.util.List.of(
			new AnswerOptionEntity(second, "Айжан изучает методологию", true, 1),
			new AnswerOptionEntity(second, "Айжан преподает методологию", false, 2),
			new AnswerOptionEntity(second, "Все изучающие методологию - магистранты", false, 3),
			new AnswerOptionEntity(second, "Вывод сделать нельзя", false, 4)
		));
		test.addQuestion(second);
		test.setStatus(TestStatus.PUBLISHED);
		tests.save(test);
	}
}
