package kz.kta.auth;

import kz.kta.common.Role;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
public class RoleInitializer implements ApplicationRunner {

	private final RoleRepository roles;

	public RoleInitializer(RoleRepository roles) {
		this.roles = roles;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (var role : Role.values()) {
			roles.findByName(role).orElseGet(() -> roles.save(new RoleEntity(role)));
		}
	}
}
