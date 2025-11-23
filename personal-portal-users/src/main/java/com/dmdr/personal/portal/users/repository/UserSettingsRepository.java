package com.dmdr.personal.portal.users.repository;

import com.dmdr.personal.portal.users.model.UserSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
	Optional<UserSettings> findByUserId(UUID userId);
}
