package kz.kta.chat;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static kz.kta.chat.ChatDtos.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService service;

	public ChatController(ChatService service) {
		this.service = service;
	}

	@GetMapping("/messages")
	List<ChatMessage> messages() {
		return service.messages();
	}

	@PostMapping("/messages")
	@ResponseStatus(HttpStatus.CREATED)
	ChatMessage send(@Valid @RequestBody SendChatMessageRequest request) {
		return service.send(request);
	}
}
