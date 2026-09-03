package com.liarcas.rootcause.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.liarcas.rootcause.document.LogEventDocument;
import com.liarcas.rootcause.dto.LogEntryResponse;
import com.liarcas.rootcause.dto.LogQueryFilter;
import com.liarcas.rootcause.dto.PagedResponse;
import com.liarcas.rootcause.index.IndexNameUtil;

/**
 * Integration test proving that tenant-scoped log queries never leak data across tenants.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class LogQueryServiceIT {

    @Container
    static ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.12.0")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @Autowired
    private LogQueryService logQueryService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private IndexNameUtil indexNameUtil;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
    }

    private static final LogQueryFilter NO_FILTER = new LogQueryFilter(null, null, null, null);

    @Test
    void shouldNotReturnOtherTenantsLogsEvenWhenQueriedDirectly() {
        seed("tenant-001", "log-t1-1", "checkout-service", "ERROR", "tenant-001 failure");
        seed("tenant-002", "log-t2-1", "checkout-service", "ERROR", "tenant-002 failure");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            PagedResponse<LogEntryResponse> tenant001Results =
                    logQueryService.queryLogs("tenant-001", NO_FILTER, 0, 20);

            assertThat(tenant001Results.content())
                    .extracting(LogEntryResponse::id)
                    .containsExactly("log-t1-1");

            PagedResponse<LogEntryResponse> tenant002Results =
                    logQueryService.queryLogs("tenant-002", NO_FILTER, 0, 20);

            assertThat(tenant002Results.content())
                    .extracting(LogEntryResponse::id)
                    .containsExactly("log-t2-1");
        });
    }

    @Test
    void shouldNotLeakOtherTenantDataViaServiceNameFilter() {
        seed("tenant-003", "log-t3-1", "shared-service-name", "INFO", "tenant-003 event");
        seed("tenant-004", "log-t4-1", "shared-service-name", "INFO", "tenant-004 event");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            LogQueryFilter filter = new LogQueryFilter("shared-service-name", null, null, null);

            PagedResponse<LogEntryResponse> tenant003Results =
                    logQueryService.queryLogs("tenant-003", filter, 0, 20);

            assertThat(tenant003Results.content())
                    .hasSize(1)
                    .extracting(LogEntryResponse::id)
                    .containsExactly("log-t3-1");
        });
    }

    @Test
    void shouldReturnEmptyPageWhenTenantIndexDoesNotExist() {
        PagedResponse<LogEntryResponse> results =
                logQueryService.queryLogs("tenant-with-no-logs", NO_FILTER, 0, 20);

        assertThat(results.content()).isEmpty();
        assertThat(results.totalElements()).isZero();
    }

    private void seed(String tenantId, String id, String serviceName, String level, String message) {
        LogEventDocument document = new LogEventDocument();
        document.setId(id);
        document.setTenantId(tenantId);
        document.setServiceName(serviceName);
        document.setLevel(level);
        document.setMessage(message);
        document.setTimestamp(Instant.now());

        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexNameUtil.getTenantIndexName(tenantId));
        // Mirror log-processing-service's write path: create the tenant index with the
        // document's explicit mapping (timestamp as date/epoch_millis) before indexing,
        // instead of relying on Elasticsearch's dynamic mapping inference.
        var indexOps = elasticsearchOperations.indexOps(indexCoordinates);
        if (!indexOps.exists()) {
            var documentIndexOps = elasticsearchOperations.indexOps(LogEventDocument.class);
            indexOps.create(documentIndexOps.createSettings());
            indexOps.putMapping(documentIndexOps.createMapping());
        }
        elasticsearchOperations.save(document, indexCoordinates);
        indexOps.refresh();
    }
}
