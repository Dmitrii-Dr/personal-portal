package com.dmdr.personal.portal.content.repository;

import com.dmdr.personal.portal.content.model.HomePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HomePageRepository extends JpaRepository<HomePage, UUID> {
    
    Optional<HomePage> findFirstByOrderByCreatedAtAsc();
}

