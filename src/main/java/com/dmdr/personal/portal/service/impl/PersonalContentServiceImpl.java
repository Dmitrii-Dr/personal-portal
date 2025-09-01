package com.dmdr.personal.portal.service.impl;

import com.dmdr.personal.portal.model.PersonalContent;
import com.dmdr.personal.portal.repository.PersonalContentRepository;
import com.dmdr.personal.portal.service.PersonalContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalContentServiceImpl implements PersonalContentService {

    private final PersonalContentRepository repository;

    @Override
    public List<PersonalContent> findAll() {
        return repository.findAll();
    }
}
