package kz.kta.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {

	@Id
	private UUID id;

	@Column(name = "full_name", nullable = false, length = 180)
	private String fullName;

	@Column(nullable = false, unique = true, length = 180)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "user_roles",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	private Set<RoleEntity> roles = new HashSet<>();

	protected UserAccount() {
	}

	public UserAccount(String fullName, String email, String passwordHash) {
		this.id = UUID.randomUUID();
		this.fullName = fullName;
		this.email = email.toLowerCase();
		this.passwordHash = passwordHash;
	}

	@PrePersist
	void onCreate() {
		var now = Instant.now();
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Set<RoleEntity> getRoles() {
		return roles;
	}

	public void addRole(RoleEntity role) {
		roles.add(role);
	}
}
