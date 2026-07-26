package kz.kta.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.kta.common.Role;

import java.time.Instant;
import java.util.UUID;

public final class ChatDtos {

	private ChatDtos() {
	}

	public record ChatMessage(
		UUID id,
		String message,
		String senderName,
		Role senderRole,
		boolean mine,
		Instant createdAt
	) { }

	public record SendChatMessageRequest(
		@NotBlank
		@Size(max = 1000)
		String message
	) { }
}
