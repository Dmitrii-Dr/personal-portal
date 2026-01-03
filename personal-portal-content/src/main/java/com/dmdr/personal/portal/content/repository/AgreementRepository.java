package com.dmdr.personal.portal.content.repository;

import com.dmdr.personal.portal.content.model.Agreement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, UUID> {

    Optional<Agreement> findByName(String name);

    Optional<Agreement> findBySlug(String slug);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Agreement a SET a.slug = :slug WHERE a.id = :id")
    void updateSlug(@org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("slug") String slug);

}
