package kz.kta.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String ACCESS_TOKEN_COOKIE = "kta_access_token";

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		var token = accessToken(request);
		if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			var email = jwtService.subject(token, "access");
			var user = userDetailsService.loadUserByUsername(email);
			var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (RuntimeException ignored) {
			SecurityContextHolder.clearContext();
		}
		filterChain.doFilter(request, response);
	}

	private String accessToken(HttpServletRequest request) {
		var header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
