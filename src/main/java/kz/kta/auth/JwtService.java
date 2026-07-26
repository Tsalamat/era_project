package kz.kta.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
	private static final String HEADER = ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

	private final ObjectMapper objectMapper;
	private final byte[] secret;
	private final Duration accessDuration;
	private final Duration refreshDuration;

	public JwtService(
		ObjectMapper objectMapper,
		@Value("${kta.jwt.secret}") String secret,
		@Value("${kta.jwt.access-token-minutes}") long accessMinutes,
		@Value("${kta.jwt.refresh-token-days}") long refreshDays
	) {
		this.objectMapper = objectMapper;
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.accessDuration = Duration.ofMinutes(accessMinutes);
		this.refreshDuration = Duration.ofDays(refreshDays);
	}

	public String accessToken(UserDetails user) {
		return createToken(user.getUsername(), "access", accessDuration);
	}

	public String refreshToken(UserDetails user) {
		return createToken(user.getUsername(), "refresh", refreshDuration);
	}

	public String subject(String token, String expectedType) {
		var claims = claims(token);
		if (!expectedType.equals(claims.get("type"))) {
			throw new IllegalArgumentException("Неверный тип токена");
		}
		var expiresAt = ((Number) claims.get("exp")).longValue();
		if (Instant.now().getEpochSecond() >= expiresAt) {
			throw new IllegalArgumentException("Срок действия токена истек");
		}
		return String.valueOf(claims.get("sub"));
	}

	private String createToken(String subject, String type, Duration duration) {
		try {
			var now = Instant.now();
			var payload = new LinkedHashMap<String, Object>();
			payload.put("sub", subject);
			payload.put("type", type);
			payload.put("iat", now.getEpochSecond());
			payload.put("exp", now.plus(duration).getEpochSecond());
			var encodedPayload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
			var unsigned = HEADER + "." + encodedPayload;
			return unsigned + "." + sign(unsigned);
		} catch (Exception exception) {
			throw new IllegalStateException("Не удалось выпустить токен", exception);
		}
	}

	private Map<String, Object> claims(String token) {
		try {
			var parts = token.split("\\.");
			if (parts.length != 3 || !MessageDigest.isEqual(sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
				throw new IllegalArgumentException("Некорректная подпись токена");
			}
			return objectMapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() { });
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IllegalArgumentException("Некорректный токен", exception);
		}
	}

	private String sign(String value) throws Exception {
		var mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret, "HmacSHA256"));
		return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
	}
}
