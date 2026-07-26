package kz.kta.auth;

import kz.kta.common.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Order(5)
public class BootstrapAdminInitializer implements ApplicationRunner {

	private final RoleRepository roles;
	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;
	private final String fullName;

	public BootstrapAdminInitializer(
		RoleRepository roles,
		UserAccountRepository users,
		PasswordEncoder passwordEncoder,
		@Value("${kta.bootstrap-admin.email:}") String email,
		@Value("${kta.bootstrap-admin.password:}") String password,
		@Value("${kta.bootstrap-admin.full-name:Админ Тест Магистратура}") String fullName
	) {
		this.roles = roles;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
		this.fullName = fullName;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
			return;
		}
		var adminRole = roles.findByName(Role.ADMIN).orElseGet(() -> roles.save(new RoleEntity(Role.ADMIN)));
		var normalizedEmail = email.trim();
		var account = users.findByEmailIgnoreCase(normalizedEmail).orElseGet(() -> {
			var created = new UserAccount(fullName.trim(), normalizedEmail, passwordEncoder.encode(password));
			return users.save(created);
		});
		account.addRole(adminRole);
		users.save(account);
	}
}
