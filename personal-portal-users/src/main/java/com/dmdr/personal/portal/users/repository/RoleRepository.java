package com.dmdr.personal.portal.users.repository;

import com.dmdr.personal.portal.core.user.domain.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Optional<Role> findRoleById(Long id);

}

