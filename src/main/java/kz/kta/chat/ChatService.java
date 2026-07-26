package kz.kta.chat;

import kz.kta.auth.RoleEntity;
import kz.kta.auth.UserAccount;
import kz.kta.auth.UserAccountRepository;
import kz.kta.common.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static kz.kta.chat.ChatDtos.*;

@Service
public class ChatService {

	private final ChatMessageRepository messages;
	private final UserAccountRepository users;

	public ChatService(ChatMessageRepository messages, UserAccountRepository users) {
		this.messages = messages;
		this.users = users;
	}

	@Transactional(readOnly = true)
	public List<ChatMessage> messages() {
		return messagesFor(currentUser().getEmail());
	}

	@Transactional(readOnly = true)
	public List<ChatMessage> messagesFor(String viewerEmail) {
		var user = userByEmail(viewerEmail);
		return messages.findTop80ByOrderByCreatedAtDesc().stream()
			.sorted(Comparator.comparing(ChatMessageEntity::getCreatedAt))
			.map(message -> dto(message, user))
			.toList();
	}

	@Transactional
	public ChatMessage send(SendChatMessageRequest request) {
		var user = currentUser();
		var message = save(user.getEmail(), request.message());
		return dto(message, user);
	}

	@Transactional
	public ChatMessageEntity save(String senderEmail, String message) {
		var text = normalize(message);
		var user = userByEmail(senderEmail);
		return messages.save(new ChatMessageEntity(user, text));
	}

	@Transactional(readOnly = true)
	public ChatMessage messageFor(UUID messageId, String viewerEmail) {
		var message = messages.findById(messageId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сообщение не найдено"));
		return dto(message, userByEmail(viewerEmail));
	}

	private ChatMessage dto(ChatMessageEntity message, UserAccount currentUser) {
		var sender = message.getUser();
		return new ChatMessage(
			message.getId(),
			message.getMessage(),
			sender.getFullName(),
			primaryRole(sender),
			sender.getId().equals(currentUser.getId()),
			message.getCreatedAt()
		);
	}

	private Role primaryRole(UserAccount account) {
		return account.getRoles().stream()
			.map(RoleEntity::getName)
			.max(Comparator.comparingInt(this::priority))
			.orElse(Role.STUDENT);
	}

	private int priority(Role role) {
		return switch (role) {
			case ADMIN -> 3;
			case TEACHER -> 2;
			case STUDENT -> 1;
		};
	}

	private UserAccount currentUser() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход");
		}
		return userByEmail(authentication.getName());
	}

	private UserAccount userByEmail(String email) {
		return users.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
	}

	private String normalize(String message) {
		if (message == null || message.trim().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Сообщение не может быть пустым");
		}
		var text = message.trim();
		if (text.length() > 1000) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Сообщение слишком длинное");
		}
		return text;
	}
}
