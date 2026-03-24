package com.dmdr.personal.portal.admin.observability.classification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestLogOutcomeClassifierTest {

    @Test
    void fromHttpStatus_shouldReturnSuccessOnlyFor2xx() {
        assertThat(RequestLogOutcomeClassifier.fromHttpStatus(200)).isEqualTo(RequestLogOutcomeClass.SUCCESS);
        assertThat(RequestLogOutcomeClassifier.fromHttpStatus(204)).isEqualTo(RequestLogOutcomeClass.SUCCESS);
        assertThat(RequestLogOutcomeClassifier.fromHttpStatus(199)).isEqualTo(RequestLogOutcomeClass.FAILURE);
        assertThat(RequestLogOutcomeClassifier.fromHttpStatus(300)).isEqualTo(RequestLogOutcomeClass.FAILURE);
        assertThat(RequestLogOutcomeClassifier.fromHttpStatus(500)).isEqualTo(RequestLogOutcomeClass.FAILURE);
    }
}
