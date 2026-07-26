package kz.kta.config;

import kz.kta.chat.ChatWebSocketHandler;
import kz.kta.chat.ChatWebSocketInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final ChatWebSocketHandler chatHandler;
	private final ChatWebSocketInterceptor chatInterceptor;

	public WebSocketConfig(ChatWebSocketHandler chatHandler, ChatWebSocketInterceptor chatInterceptor) {
		this.chatHandler = chatHandler;
		this.chatInterceptor = chatInterceptor;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(chatHandler, "/ws/chat")
			.addInterceptors(chatInterceptor)
			.setAllowedOriginPatterns("*");
	}
}
