package kz.kta.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAccountDetailsService implements UserDetailsService {

	private final UserAccountRepository users;

	public UserAccountDetailsService(UserAccountRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		var account = users.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
		var authorities = account.getRoles().stream()
			.map(role -> "ROLE_" + role.getName().name())
			.toArray(String[]::new);
		return User.withUsername(account.getEmail())
			.password(account.getPasswordHash())
			.disabled(!account.isEnabled())
			.authorities(authorities)
			.build();
	}
}
