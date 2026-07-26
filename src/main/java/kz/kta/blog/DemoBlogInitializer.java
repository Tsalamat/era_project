package kz.kta.blog;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kta.demo-data.enabled", havingValue = "true")
@Order(40)
public class DemoBlogInitializer implements ApplicationRunner {
	private final BlogPostRepository posts;

	public DemoBlogInitializer(BlogPostRepository posts) { this.posts = posts; }

	@Override
	public void run(ApplicationArguments args) {
		if (posts.count() > 0) return;
		posts.save(new BlogPostEntity(
			"Как подготовиться к КТА в магистратуру", "kak-podgotovitsya-k-kta",
			"Практический план: диагностика, работа по темам и регулярная проверка результата.",
			"Начните со стартового теста и отметьте темы, в которых ошибки повторяются. Так план подготовки будет опираться на факты, а не на ощущение сложности.\n\nРазделите неделю на короткие занятия: теория, несколько задач и обязательный разбор ответов. После каждой темы снова проходите тест и сравнивайте результат со своей предыдущей попыткой.\n\nПрогресс полезно оценивать по завершенным урокам и сохраненным результатам. Платформа считает эти данные автоматически после входа в аккаунт.",
			"Стратегия", 4, "PUBLISHED"
		));
		posts.save(new BlogPostEntity(
			"Как работать над ошибками в пробном тесте", "rabota-nad-oshibkami-kta",
			"Способ превратить каждый неверный ответ в конкретную тему для повторения.",
			"После теста не ограничивайтесь итоговым баллом. Прочитайте объяснение, восстановите ход решения и сформулируйте причину ошибки одним предложением.\n\nЕсли ошибка связана с правилом, вернитесь к уроку. Если причина в невнимательности, повторите похожее задание с ограничением времени. Следующая попытка покажет, сработал ли выбранный способ.",
			"Практика", 3, "PUBLISHED"
		));
	}
}
