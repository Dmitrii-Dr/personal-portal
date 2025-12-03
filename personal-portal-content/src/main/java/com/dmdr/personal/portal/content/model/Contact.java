package com.dmdr.personal.portal.content.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Contact {

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "description")
    private String description;
}

