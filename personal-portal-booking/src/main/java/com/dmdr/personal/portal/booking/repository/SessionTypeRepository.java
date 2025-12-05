package com.dmdr.personal.portal.booking.repository;

import com.dmdr.personal.portal.booking.model.SessionType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionTypeRepository extends JpaRepository<SessionType, Long> {
	List<SessionType> findByActiveTrue();
}

