package com.dmdr.personal.portal.admin.observability.classification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HttpOutcomeClassifierTest {

    @Test
    void classify_shouldMapStatusesToExpectedBuckets() {
        assertThat(HttpOutcomeClassifier.classify(200)).isEqualTo(HttpOutcomeBucket.SUCCESS_2XX);
        assertThat(HttpOutcomeClassifier.classify(204)).isEqualTo(HttpOutcomeBucket.SUCCESS_2XX);
        assertThat(HttpOutcomeClassifier.classify(301)).isEqualTo(HttpOutcomeBucket.OTHER_NON_SUCCESS);
        assertThat(HttpOutcomeClassifier.classify(400)).isEqualTo(HttpOutcomeBucket.CLIENT_ERROR_4XX_OTHER);
        assertThat(HttpOutcomeClassifier.classify(404)).isEqualTo(HttpOutcomeBucket.CLIENT_ERROR_4XX_OTHER);
        assertThat(HttpOutcomeClassifier.classify(401)).isEqualTo(HttpOutcomeBucket.AUTH_ERROR_401_403);
        assertThat(HttpOutcomeClassifier.classify(403)).isEqualTo(HttpOutcomeBucket.AUTH_ERROR_401_403);
        assertThat(HttpOutcomeClassifier.classify(500)).isEqualTo(HttpOutcomeBucket.SERVER_ERROR_5XX);
        assertThat(HttpOutcomeClassifier.classify(503)).isEqualTo(HttpOutcomeBucket.SERVER_ERROR_5XX);
    }

    @Test
    void isSuccess_shouldBeTrueOnlyFor2xx() {
        assertThat(HttpOutcomeClassifier.isSuccess(200)).isTrue();
        assertThat(HttpOutcomeClassifier.isSuccess(299)).isTrue();
        assertThat(HttpOutcomeClassifier.isSuccess(199)).isFalse();
        assertThat(HttpOutcomeClassifier.isSuccess(300)).isFalse();
        assertThat(HttpOutcomeClassifier.isSuccess(500)).isFalse();
    }
}
