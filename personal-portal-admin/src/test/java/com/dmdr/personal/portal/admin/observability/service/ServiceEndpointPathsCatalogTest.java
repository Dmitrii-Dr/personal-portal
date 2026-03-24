package com.dmdr.personal.portal.admin.observability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.admin.observability.routing.DefaultRequestLoggingPathPolicy;
import com.dmdr.personal.portal.admin.observability.routing.RequestLoggingPathPolicy;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.method.HandlerMethod;

class ServiceEndpointPathsCatalogTest {

    @Test
    void shouldCollectUniqueSortedObservedPathsFromMappings() throws Exception {
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        RequestLoggingPathPolicy pathPolicy = new DefaultRequestLoggingPathPolicy();

        Map<RequestMappingInfo, HandlerMethod> mappings = new LinkedHashMap<>();
        mappings.put(mapping("/api/v1/public/articles"), handlerMethod("listArticles"));
        mappings.put(mapping("/api/v1/public/articles"), handlerMethod("createArticle"));
        mappings.put(mapping("/api/v1/public/articles/{id}"), handlerMethod("getArticle"));
        mappings.put(mapping("/actuator/health"), handlerMethod("actuatorHealth"));
        mappings.put(mapping("/admin"), handlerMethod("adminIndex"));
        mappings.put(mapping("/assets/app.js"), handlerMethod("asset"));

        when(handlerMapping.getHandlerMethods()).thenReturn(mappings);

        ServiceEndpointPathsCatalog catalog = new ServiceEndpointPathsCatalog(handlerMapping, pathPolicy);

        assertThat(catalog.paths()).containsExactly(
            "/api/v1/public/articles",
            "/api/v1/public/articles/{id}"
        );
    }

    private static RequestMappingInfo mapping(String path) {
        return RequestMappingInfo.paths(path).build();
    }

    private static HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        DummyController controller = new DummyController();
        Method method = DummyController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(controller, Objects.requireNonNull(method));
    }

    @SuppressWarnings("unused")
    private static final class DummyController {

        @GetMapping
        void listArticles() {
        }

        @PostMapping
        void createArticle() {
        }

        @GetMapping
        void getArticle() {
        }

        @GetMapping
        void actuatorHealth() {
        }

        @GetMapping
        void adminIndex() {
        }

        @GetMapping
        void asset() {
        }
    }
}
