package kz.kta.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

	private final ChatService chatService;
	private final ObjectMapper objectMapper;
	private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

	public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
		this.chatService = chatService;
		this.objectMapper = objectMapper;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		if (email(session) == null) {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Требуется вход"));
			return;
		}
		sessions.add(session);
		send(session, Map.of("type", "history", "messages", chatService.messagesFor(email(session))));
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
		var senderEmail = email(session);
		if (senderEmail == null) {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Требуется вход"));
			return;
		}

		try {
			var request = objectMapper.readValue(textMessage.getPayload(), IncomingMessage.class);
			var saved = chatService.save(senderEmail, request.message());
			for (WebSocketSession recipient : sessions) {
				if (recipient.isOpen()) {
					send(recipient, Map.of("type", "message", "message", chatService.messageFor(saved.getId(), email(recipient))));
				}
			}
		} catch (ResponseStatusException exception) {
			send(session, Map.of("type", "error", "message", exception.getReason() == null ? "Не удалось отправить сообщение" : exception.getReason()));
		} catch (Exception exception) {
			send(session, Map.of("type", "error", "message", "Не удалось отправить сообщение"));
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sessions.remove(session);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		sessions.remove(session);
	}

	private String email(WebSocketSession session) {
		var value = session.getAttributes().get(ChatWebSocketInterceptor.EMAIL_ATTRIBUTE);
		return value instanceof String email ? email : null;
	}

	private void send(WebSocketSession session, Object payload) throws Exception {
		synchronized (session) {
			if (session.isOpen()) {
				session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
			}
		}
	}

	private record IncomingMessage(String message) {
	}
}
