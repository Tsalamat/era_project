package kz.kta.auth;

import kz.kta.common.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

	Optional<RoleEntity> findByName(Role name);
}
