package kz.kta.blog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kz.kta.auth.UserAccount;
import kz.kta.auth.UserAccountRepository;
import kz.kta.common.ApiResponse;
import kz.kta.common.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BlogController {
	private static final String PUBLISHED = "PUBLISHED";
	private final BlogPostRepository posts;
	private final BlogCommentRepository comments;
	private final BlogLikeRepository likes;
	private final UserAccountRepository users;

	public BlogController(BlogPostRepository posts, BlogCommentRepository comments, BlogLikeRepository likes, UserAccountRepository users) {
		this.posts = posts;
		this.comments = comments;
		this.likes = likes;
		this.users = users;
	}

	@GetMapping("/blog")
	@Transactional(readOnly = true)
	List<BlogPost> posts() { return posts.findByStatusOrderByCreatedAtDesc(PUBLISHED).stream().map(this::dto).toList(); }

	@GetMapping("/blog/{slug}")
	@Transactional(readOnly = true)
	BlogPost post(@PathVariable String slug) {
		return dto(posts.findBySlugAndStatus(slug, PUBLISHED).orElseThrow(() -> notFound("Статья не найдена")));
	}

	@GetMapping("/blog/{slug}/comments")
	@Transactional(readOnly = true)
	List<BlogComment> comments(@PathVariable String slug) {
		var post = publishedPost(slug);
		return comments.findByPostIdOrderByCreatedAtAsc(post.getId()).stream().map(this::commentDto).toList();
	}

	@PostMapping("/blog/{slug}/comments")
	@Transactional
	BlogComment addComment(@PathVariable String slug, @Valid @RequestBody BlogCommentRequest request) {
		var post = publishedPost(slug);
		var user = currentUser();
		return commentDto(comments.save(new BlogCommentEntity(post, user, request.content().trim())));
	}

	@DeleteMapping("/blog/comments/{id}")
	@Transactional
	ApiResponse deleteComment(@PathVariable UUID id) {
		var comment = comments.findById(id).orElseThrow(() -> notFound("Комментарий не найден"));
		var user = currentUser();
		var ownsComment = comment.getUser().getId().equals(user.getId());
		var ownsPost = comment.getPost().getAuthor() != null && comment.getPost().getAuthor().getId().equals(user.getId());
		if (!ownsComment && !ownsPost && !isAdmin(user)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можно удалить только свой комментарий");
		}
		comments.delete(comment);
		return ApiResponse.accepted("Комментарий удален");
	}

	@PostMapping("/blog/{slug}/like")
	@Transactional
	BlogReaction toggleLike(@PathVariable String slug) {
		var post = publishedPost(slug);
		var user = currentUser();
		var existing = likes.findByPostIdAndUserId(post.getId(), user.getId());
		if (existing.isPresent()) {
			likes.delete(existing.get());
			return new BlogReaction(likes.countByPostId(post.getId()), false);
		}
		likes.save(new BlogLikeEntity(post, user));
		return new BlogReaction(likes.countByPostId(post.getId()), true);
	}

	@GetMapping("/teacher/blog")
	@Transactional(readOnly = true)
	List<BlogPost> teacherPosts() {
		var user = currentUser();
		var source = isAdmin(user) ? posts.findAllByOrderByUpdatedAtDesc() : posts.findByAuthorIdOrderByUpdatedAtDesc(user.getId());
		return source.stream().map(this::dto).toList();
	}

	@GetMapping("/teacher/blog/{id}")
	@Transactional(readOnly = true)
	BlogPost teacherPost(@PathVariable UUID id) {
		return dto(ownedPost(id));
	}

	@PostMapping("/teacher/blog")
	@Transactional
	BlogPost teacherCreate(@Valid @RequestBody BlogPostRequest request) {
		return createFor(currentUser(), request);
	}

	@PatchMapping("/teacher/blog/{id}")
	@Transactional
	BlogPost teacherUpdate(@PathVariable UUID id, @Valid @RequestBody BlogPostRequest request) {
		return updatePost(ownedPost(id), request);
	}

	@DeleteMapping("/teacher/blog/{id}")
	@Transactional
	ApiResponse teacherDelete(@PathVariable UUID id) {
		posts.delete(ownedPost(id));
		return ApiResponse.accepted("Статья удалена");
	}

	@PostMapping("/admin/blog")
	@Transactional
	BlogPost create(@Valid @RequestBody BlogPostRequest request) {
		return createFor(currentUser(), request);
	}

	private BlogPost createFor(UserAccount author, BlogPostRequest request) {
		if (posts.existsBySlug(request.slug().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug уже используется");
		return dto(posts.save(new BlogPostEntity(
			request.title().trim(), request.slug().trim(), request.excerpt(), request.content(), request.category().trim(), request.readMinutes(), status(request.status()), author
		)));
	}

	@PatchMapping("/admin/blog/{id}")
	@Transactional
	BlogPost update(@PathVariable UUID id, @Valid @RequestBody BlogPostRequest request) {
		var post = posts.findById(id).orElseThrow(() -> notFound("Статья не найдена"));
		return updatePost(post, request);
	}

	private BlogPost updatePost(BlogPostEntity post, BlogPostRequest request) {
		if (posts.existsBySlugAndIdNot(request.slug().trim(), post.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug уже используется");
		post.update(request.title().trim(), request.slug().trim(), request.excerpt(), request.content(), request.category().trim(), request.readMinutes(), status(request.status()));
		return dto(posts.save(post));
	}

	@DeleteMapping("/admin/blog/{id}")
	@Transactional
	ApiResponse delete(@PathVariable UUID id) {
		if (!posts.existsById(id)) throw notFound("Статья не найдена");
		posts.deleteById(id);
		return ApiResponse.accepted("Статья удалена");
	}

	private BlogPost dto(BlogPostEntity post) {
		var likedByMe = currentUserOptional()
			.map(user -> likes.existsByPostIdAndUserId(post.getId(), user.getId()))
			.orElse(false);
		return new BlogPost(
			post.getId(), post.getSlug(), post.getTitle(), post.getExcerpt(), post.getContent(),
			post.getCategory(), post.getReadMinutes(), post.getStatus(),
			post.getAuthor() == null ? null : post.getAuthor().getFullName(),
			likes.countByPostId(post.getId()), comments.countByPostId(post.getId()), likedByMe, post.getCreatedAt()
		);
	}

	private BlogComment commentDto(BlogCommentEntity comment) {
		return new BlogComment(
			comment.getId(), comment.getUser().getFullName(), comment.getUser().getEmail(), primaryRole(comment.getUser()),
			comment.getContent(), comment.getCreatedAt()
		);
	}

	private BlogPostEntity publishedPost(String slug) {
		return posts.findBySlugAndStatus(slug, PUBLISHED).orElseThrow(() -> notFound("Статья не найдена"));
	}

	private BlogPostEntity ownedPost(UUID id) {
		var post = posts.findById(id).orElseThrow(() -> notFound("Статья не найдена"));
		var user = currentUser();
		if (!isAdmin(user) && (post.getAuthor() == null || !post.getAuthor().getId().equals(user.getId()))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можно редактировать только свои статьи");
		}
		return post;
	}

	private UserAccount currentUser() {
		return currentUserOptional().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход"));
	}

	private Optional<UserAccount> currentUserOptional() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) return Optional.empty();
		return users.findByEmailIgnoreCase(authentication.getName());
	}

	private boolean isAdmin(UserAccount user) {
		return user.getRoles().stream().anyMatch(role -> role.getName() == Role.ADMIN);
	}

	private Role primaryRole(UserAccount user) {
		return user.getRoles().stream().map(role -> role.getName()).sorted((left, right) -> Integer.compare(priority(right), priority(left))).findFirst().orElse(Role.STUDENT);
	}

	private int priority(Role role) {
		return switch (role) {
			case ADMIN -> 3;
			case TEACHER -> 2;
			case STUDENT -> 1;
		};
	}

	private String status(String status) { return PUBLISHED.equals(status) ? PUBLISHED : "DRAFT"; }
	private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }

	public record BlogPost(UUID id, String slug, String title, String excerpt, String content, String category, int readMinutes, String status, String authorName, long likesCount, long commentsCount, boolean likedByMe, Instant createdAt) { }

	public record BlogComment(UUID id, String authorName, String authorEmail, Role authorRole, String content, Instant createdAt) { }

	public record BlogReaction(long likesCount, boolean likedByMe) { }

	public record BlogPostRequest(
		@NotBlank String title, @NotBlank String slug, String excerpt, @NotBlank String content,
		@NotBlank String category, @Positive int readMinutes, String status
	) { }

	public record BlogCommentRequest(@NotBlank @Size(max = 1200) String content) { }
}
