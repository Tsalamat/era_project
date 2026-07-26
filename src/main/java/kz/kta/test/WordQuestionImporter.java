package kz.kta.test;

import kz.kta.common.QuestionType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static kz.kta.test.TestDtos.*;

@Component
public class WordQuestionImporter {

	private static final Pattern QUESTION = Pattern.compile("^(?:Вопрос\\s*)?\\d+\\s*[).:\\-]\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern OPTION = Pattern.compile("^([A-DА-Га-г])\\s*[).:\\-]\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern ANSWER = Pattern.compile("^(?:Ответ|Правильный\\s+ответ|Answer|Correct)\\s*[:\\-]\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern EXPLANATION = Pattern.compile("^(?:Объяснение|Разбор|Explanation)\\s*[:\\-]\\s*(.*)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern TYPE = Pattern.compile("^(?:Тип|Type)\\s*[:\\-]\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	public List<QuestionRequest> parse(MultipartFile file, int firstOrderNumber) {
		validateFile(file);
		var lines = lines(file);
		var imported = new ArrayList<ImportedQuestion>();
		ImportedQuestion current = null;
		var readingExplanation = false;

		for (var line : lines) {
			var questionMatcher = QUESTION.matcher(line);
			if (questionMatcher.matches()) {
				if (current != null) imported.add(current);
				current = new ImportedQuestion();
				current.questionText.append(questionMatcher.group(1).trim());
				readingExplanation = false;
				continue;
			}
			if (current == null) {
				throw badRequest("Файл должен начинаться со строки вопроса, например: 1. Текст вопроса");
			}

			var optionMatcher = OPTION.matcher(line);
			if (optionMatcher.matches()) {
				if (current.options.size() >= 4) {
					throw badRequest("У вопроса \"" + preview(current.questionText) + "\" больше 4 вариантов ответа");
				}
				current.options.add(new ImportedOption(labelIndex(optionMatcher.group(1)), optionMatcher.group(2).trim()));
				readingExplanation = false;
				continue;
			}

			var answerMatcher = ANSWER.matcher(line);
			if (answerMatcher.matches()) {
				current.correctIndexes.clear();
				current.correctIndexes.addAll(answerIndexes(answerMatcher.group(1)));
				readingExplanation = false;
				continue;
			}

			var typeMatcher = TYPE.matcher(line);
			if (typeMatcher.matches()) {
				current.type = questionType(typeMatcher.group(1));
				readingExplanation = false;
				continue;
			}

			var explanationMatcher = EXPLANATION.matcher(line);
			if (explanationMatcher.matches()) {
				append(current.explanation, explanationMatcher.group(1).trim());
				readingExplanation = true;
				continue;
			}

			if (readingExplanation || !current.correctIndexes.isEmpty()) {
				append(current.explanation, line);
			} else if (current.options.isEmpty()) {
				append(current.questionText, line);
			} else {
				current.options.get(current.options.size() - 1).append(line);
			}
		}

		if (current != null) imported.add(current);
		if (imported.isEmpty()) {
			throw badRequest("В Word-файле не найдено ни одного вопроса");
		}

		var requests = new ArrayList<QuestionRequest>();
		for (var i = 0; i < imported.size(); i++) {
			requests.add(request(imported.get(i), firstOrderNumber + i));
		}
		return requests;
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw badRequest("Загрузите Word-файл .docx с вопросами");
		}
		var name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
		if (!name.endsWith(".docx")) {
			throw badRequest("Поддерживается только формат .docx");
		}
	}

	private List<String> lines(MultipartFile file) {
		try (var document = new XWPFDocument(file.getInputStream())) {
			var lines = new ArrayList<String>();
			document.getParagraphs().forEach(paragraph -> addLine(lines, paragraph.getText()));
			for (XWPFTable table : document.getTables()) {
				table.getRows().forEach(row -> row.getTableCells().forEach(cell ->
					cell.getParagraphs().forEach(paragraph -> addLine(lines, paragraph.getText()))
				));
			}
			return lines;
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Не удалось прочитать Word-файл", exception);
		}
	}

	private void addLine(List<String> lines, String value) {
		var line = value == null ? "" : value.replace('\u00A0', ' ').trim();
		if (!line.isBlank()) lines.add(line);
	}

	private QuestionRequest request(ImportedQuestion source, int orderNumber) {
		if (source.questionText.toString().trim().isBlank()) {
			throw badRequest("В одном из вопросов не заполнен текст");
		}
		if (source.options.size() != 4) {
			throw badRequest("У вопроса \"" + preview(source.questionText) + "\" должно быть ровно 4 варианта A-D");
		}
		if (source.correctIndexes.isEmpty()) {
			throw badRequest("У вопроса \"" + preview(source.questionText) + "\" не указана строка Ответ: A");
		}
		for (var index : source.correctIndexes) {
			if (index < 1 || index > 4) {
				throw badRequest("У вопроса \"" + preview(source.questionText) + "\" правильный ответ должен быть A, B, C или D");
			}
		}
		var type = source.type == null
			? source.correctIndexes.size() > 1 ? QuestionType.MULTIPLE_CHOICE : QuestionType.SINGLE_CHOICE
			: source.type;
		if (type == QuestionType.SINGLE_CHOICE && source.correctIndexes.size() != 1) {
			throw badRequest("Для SINGLE_CHOICE у вопроса \"" + preview(source.questionText) + "\" должен быть один правильный ответ");
		}

		var options = new ArrayList<AnswerOptionRequest>();
		for (var i = 0; i < source.options.size(); i++) {
			var option = source.options.get(i);
			if (option.labelIndex != i + 1) {
				throw badRequest("У вопроса \"" + preview(source.questionText) + "\" варианты должны идти по порядку A, B, C, D");
			}
			if (option.text.toString().trim().isBlank()) {
				throw badRequest("У вопроса \"" + preview(source.questionText) + "\" есть пустой вариант ответа");
			}
			options.add(new AnswerOptionRequest((UUID) null, option.text.toString().trim(), source.correctIndexes.contains(i + 1), i + 1));
		}
		return new QuestionRequest(
			source.questionText.toString().trim(),
			type,
			source.explanation.toString().trim(),
			orderNumber,
			options
		);
	}

	private Set<Integer> answerIndexes(String value) {
		var result = new HashSet<Integer>();
		for (var token : value.split("[,;\\s]+")) {
			var clean = token.trim();
			if (clean.isBlank()) continue;
			result.add(labelIndex(clean.substring(0, 1)));
		}
		if (result.isEmpty()) {
			throw badRequest("Строка Ответ должна содержать букву A, B, C или D");
		}
		return result;
	}

	private int labelIndex(String label) {
		return switch (label.trim().toUpperCase(Locale.ROOT)) {
			case "A", "А" -> 1;
			case "B", "Б" -> 2;
			case "C", "В" -> 3;
			case "D", "Г" -> 4;
			default -> throw badRequest("Варианты должны быть помечены A, B, C, D");
		};
	}

	private QuestionType questionType(String value) {
		var normalized = value.trim().toUpperCase(Locale.ROOT);
		if (normalized.contains("MULTIPLE") || normalized.contains("НЕСКОЛЬ")) {
			return QuestionType.MULTIPLE_CHOICE;
		}
		if (normalized.contains("SINGLE") || normalized.contains("ОДИН")) {
			return QuestionType.SINGLE_CHOICE;
		}
		throw badRequest("Тип вопроса должен быть SINGLE_CHOICE или MULTIPLE_CHOICE");
	}

	private void append(StringBuilder builder, String value) {
		if (value == null || value.isBlank()) return;
		if (!builder.isEmpty()) builder.append('\n');
		builder.append(value.trim());
	}

	private String preview(StringBuilder value) {
		var text = value.toString().trim();
		return text.length() <= 80 ? text : text.substring(0, 77) + "...";
	}

	private ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
	}

	private static final class ImportedQuestion {
		private final StringBuilder questionText = new StringBuilder();
		private final List<ImportedOption> options = new ArrayList<>();
		private final Set<Integer> correctIndexes = new HashSet<>();
		private final StringBuilder explanation = new StringBuilder();
		private QuestionType type;
	}

	private static final class ImportedOption {
		private final int labelIndex;
		private final StringBuilder text = new StringBuilder();

		private ImportedOption(int labelIndex, String text) {
			this.labelIndex = labelIndex;
			this.text.append(text);
		}

		private void append(String value) {
			if (!text.isEmpty()) text.append(' ');
			text.append(value.trim());
		}
	}
}
