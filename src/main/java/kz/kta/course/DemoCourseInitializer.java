package kz.kta.course;

import kz.kta.lesson.LessonEntity;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "kta.demo-data.enabled", havingValue = "true")
@Order(30)
public class DemoCourseInitializer implements ApplicationRunner {
	private final CourseRepository courses;

	public DemoCourseInitializer(CourseRepository courses) { this.courses = courses; }

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (courses.count() > 0) return;
		courses.save(course(
			"КТА: логическое мышление", "kta-logika", "Последовательности, аргументы и логические выводы для комплексного тестирования.", "Логика",
			"",
			new String[][] {
				{"Структура логического блока", "Разберем типы заданий и порядок работы с условием.", "16"},
				{"Числовые последовательности", "Научимся находить шаг, множитель и чередующиеся закономерности.", "24"},
				{"Аргументы и выводы", "Проверим, какой вывод действительно следует из данных посылок.", "28"}
			}
		));
		courses.save(course(
			"Английский язык для КТА", "kta-english", "Грамматика, академическая лексика и работа с текстом в формате КТА.", "Английский язык",
			"",
			new String[][] {
				{"Диагностика уровня", "Определим сильные темы и грамматические пробелы перед подготовкой.", "18"},
				{"Academic vocabulary", "Разберем частотную лексику и способы понимать значение по контексту.", "22"},
				{"Reading strategies", "Потренируем поиск основной мысли и деталей без дословного перевода.", "26"}
			}
		));
		courses.save(course(
			"Профильный предмет", "kta-profile", "План подготовки по профильным темам: диагностика, теория и закрепление.", "Профиль",
			"",
			new String[][] {
				{"Диагностический разбор", "Составим карту тем по результатам стартовой диагностики.", "18"},
				{"Работа с теорией", "Разберем способ конспектировать определения и связи между понятиями.", "20"},
				{"Закрепление темы", "Соберем цикл практики, проверки и повторения ошибок.", "24"}
			}
		));
	}

	private CourseEntity course(String title, String slug, String description, String subject, String cover, String[][] lessonData) {
		var course = new CourseEntity(title, slug, description, subject, cover);
		var module = new CourseModuleEntity(course, "Основы", 1);
		for (var index = 0; index < lessonData.length; index++) {
			var data = lessonData[index];
			module.addLesson(new LessonEntity(module, data[0], data[1], Integer.parseInt(data[2]), index + 1));
		}
		course.addModule(module);
		return course;
	}
}
