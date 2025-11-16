package com.dmdr.personal.portal.users.repository;

import com.dmdr.personal.portal.users.model.User;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findUserById(UUID id);

    // Find all users that have a role with the given name (e.g., "ROLE_USER")
    List<User> findByRoles_Name(String name);
}

