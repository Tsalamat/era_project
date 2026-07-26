package kz.kta.chat;

import kz.kta.auth.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class ChatWebSocketInterceptor implements HandshakeInterceptor {

	static final String EMAIL_ATTRIBUTE = "email";
	private static final String ACCESS_TOKEN_COOKIE = "kta_access_token";

	private final JwtService jwtService;

	public ChatWebSocketInterceptor(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
		var token = accessToken(request);
		if (token == null) {
			return false;
		}
		try {
			attributes.put(EMAIL_ATTRIBUTE, jwtService.subject(token, "access"));
			return true;
		} catch (RuntimeException exception) {
			return false;
		}
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
	}

	private String accessToken(ServerHttpRequest request) {
		var header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		if (request instanceof ServletServerHttpRequest servletRequest && servletRequest.getServletRequest().getCookies() != null) {
			for (var cookie : servletRequest.getServletRequest().getCookies()) {
				if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}
}
