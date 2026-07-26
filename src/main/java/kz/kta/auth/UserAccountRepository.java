package kz.kta.auth;

import kz.kta.common.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	Optional<UserAccount> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	List<UserAccount> findByIdIn(List<UUID> ids);

	@Query("select distinct user from UserAccount user join user.roles role where role.name = :role order by user.fullName")
	List<UserAccount> findByRole(@Param("role") Role role);
}
