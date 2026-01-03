package com.dmdr.personal.portal.content.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgreementDictionaryItem {

    private UUID id;
    private String name;
    private String slug;
}
