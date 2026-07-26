package kz.kta.seo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kz.kta.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SeoController {
	private final String publicUrl;

	public SeoController(@Value("${kta.public-url:https://invest-gold.asia}") String publicUrl) {
		this.publicUrl = publicUrl.replaceAll("/$", "");
	}

	@GetMapping("/seo")
	SeoMetadata metadata(@RequestParam String path) {
		var normalizedPath = path.startsWith("/") ? path : "/" + path;
		var page = pageMetadata(normalizedPath);
		return new SeoMetadata(normalizedPath, page.title(), page.description(), publicUrl + normalizedPath, true);
	}

	@PatchMapping("/admin/seo")
	ApiResponse update(@Valid @RequestBody SeoMetadata metadata) {
		return ApiResponse.accepted("SEO metadata updated for " + metadata.path());
	}

	public record SeoMetadata(
		@NotBlank String path,
		@NotBlank String title,
		@NotBlank String description,
		String canonical,
		boolean indexable
	) {
	}

	private PageMetadata pageMetadata(String path) {
		return switch (path) {
			case "/" -> new PageMetadata(
				"Подготовка к КТА в магистратуру онлайн",
				"Бесплатная подготовка к КТА в магистратуру: курсы, уроки, пробные тесты, объяснения ошибок и личный прогресс."
			);
			case "/courses" -> new PageMetadata(
				"Бесплатные курсы КТА для магистратуры",
				"Онлайн-курсы по логике, английскому, профильным предметам и стратегии поступления в магистратуру."
			);
			case "/tests" -> new PageMetadata(
				"Пробные тесты КТА онлайн",
				"Бесплатные тесты КТА с таймером, результатом, аналитикой и объяснением ошибок."
			);
			case "/blog" -> new PageMetadata(
				"Блог о КТА и поступлении в магистратуру",
				"Статьи о подготовке к КТА, баллах, пробных тестах, логике, английском и профильных предметах."
			);
			case "/magistratura" -> new PageMetadata(
				"Магистратура и КТА: подготовка к поступлению",
				"Материалы о поступлении в магистратуру через КТА: предметы, баллы, стратегия и план занятий."
			);
			default -> new PageMetadata(
				"Тест Магистратура",
				"Бесплатная образовательная платформа для подготовки к КТА в магистратуру."
			);
		};
	}

	private record PageMetadata(String title, String description) {
	}
}
