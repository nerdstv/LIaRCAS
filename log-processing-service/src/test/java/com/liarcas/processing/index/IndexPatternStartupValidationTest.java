package com.liarcas.processing.index;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class IndexPatternStartupValidationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(IndexNameUtil.class);

    @Test
    void shouldStartWhenIndexPatternIsValid() {
        contextRunner
                .withPropertyValues("liarcas.elasticsearch.index-pattern=liarcas-logs-{tenantId}")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IndexNameUtil.class);
                });
    }

    @Test
    void shouldFailStartupWhenPlaceholderIsMissing() {
        contextRunner
                .withPropertyValues("liarcas.elasticsearch.index-pattern=logs-static")
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable rootCause = getRootCause(context.getStartupFailure());
                    assertThat(rootCause)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("must include {tenantId}");
                });
    }

    @Test
    void shouldFailStartupWhenPatternContainsInvalidCharacters() {
        contextRunner
                .withPropertyValues("liarcas.elasticsearch.index-pattern=Logs-{tenantId}")
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable rootCause = getRootCause(context.getStartupFailure());
                    assertThat(rootCause)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("invalid characters");
                });
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
