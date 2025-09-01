package com.dmdr.personal.portal.repository;

import com.dmdr.personal.portal.model.PersonalContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalContentRepository extends JpaRepository<PersonalContent, Long> {
}
