package kz.kta.auth;

import kz.kta.common.Role;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;

import java.util.EnumMap;

@Component
@ConditionalOnProperty(name = "kta.demo-data.enabled", havingValue = "true")
@Order(10)
public class DemoAccountInitializer implements ApplicationRunner {

	private final RoleRepository roles;
	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;

	public DemoAccountInitializer(RoleRepository roles, UserAccountRepository users, PasswordEncoder passwordEncoder) {
		this.roles = roles;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		var roleMap = new EnumMap<Role, RoleEntity>(Role.class);
		for (var role : Role.values()) {
			roleMap.put(role, roles.findByName(role).orElseGet(() -> roles.save(new RoleEntity(role))));
		}
		create("Студент КТА", "student@kta.kz", "Student123!", roleMap.get(Role.STUDENT));
		create("Учитель КТА", "teacher@kta.kz", "Teacher123!", roleMap.get(Role.TEACHER));
		create("Администратор КТА", "admin@kta.kz", "Admin123!", roleMap.get(Role.ADMIN));
	}

	private void create(String fullName, String email, String password, RoleEntity role) {
		if (users.existsByEmailIgnoreCase(email)) {
			return;
		}
		var account = new UserAccount(fullName, email, passwordEncoder.encode(password));
		account.addRole(role);
		users.save(account);
	}
}
