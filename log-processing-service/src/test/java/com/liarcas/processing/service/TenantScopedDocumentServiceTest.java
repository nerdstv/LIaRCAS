package com.liarcas.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

import com.liarcas.processing.document.LogEventDocument;

@ExtendWith(MockitoExtension.class)
class TenantScopedDocumentServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private IndexOperations tenantIndexOperations;

    @Mock
    private IndexOperations documentIndexOperations;

    @InjectMocks
    private TenantScopedDocumentService tenantScopedDocumentService;

    @Test
    void shouldCreateTenantIndexWithMappingBeforeIndexingWhenMissing() {
        LogEventDocument document = new LogEventDocument(
                "log-123",
                "tenant-001",
                "payment-service",
                null,
                null,
                null,
                null,
                null,
                "ERROR",
                "Database timeout",
                null,
                null,
                Instant.parse("2026-04-21T10:15:30Z")
        );
        Settings settings = new Settings();
        Document mapping = Document.create();

        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class))).thenReturn(tenantIndexOperations);
        when(elasticsearchOperations.indexOps(LogEventDocument.class)).thenReturn(documentIndexOperations);
        when(tenantIndexOperations.exists()).thenReturn(false);
        when(documentIndexOperations.createSettings()).thenReturn(settings);
        when(documentIndexOperations.createMapping()).thenReturn(mapping);

        LogEventDocument saved = tenantScopedDocumentService.save(document);

        ArgumentCaptor<IndexCoordinates> indexCoordinatesCaptor = ArgumentCaptor.forClass(IndexCoordinates.class);
        verify(elasticsearchOperations).index(any(IndexQuery.class), indexCoordinatesCaptor.capture());
        assertThat(indexCoordinatesCaptor.getValue().getIndexName()).isEqualTo("liarcas-logs-tenant-001");
        verify(tenantIndexOperations).create(settings);
        verify(tenantIndexOperations).putMapping(mapping);
        verify(tenantIndexOperations).refresh();
        assertThat(saved).isSameAs(document);
    }

    @Test
    void shouldReuseExistingTenantIndex() {
        LogEventDocument document = new LogEventDocument(
                "log-456",
                "tenant-002",
                "billing-service",
                null,
                null,
                null,
                null,
                null,
                "WARN",
                "Slow downstream dependency",
                null,
                null,
                Instant.parse("2026-04-21T10:15:30Z")
        );

        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class))).thenReturn(tenantIndexOperations);
        when(tenantIndexOperations.exists()).thenReturn(true);

        tenantScopedDocumentService.save(document);

        verify(elasticsearchOperations, never()).indexOps(LogEventDocument.class);
        verify(tenantIndexOperations, never()).create(any(Settings.class));
        verify(tenantIndexOperations, never()).putMapping(any(Document.class));
        verify(elasticsearchOperations).index(any(IndexQuery.class), any(IndexCoordinates.class));
        verify(tenantIndexOperations).refresh();
    }

    @Test
    void shouldRejectDocumentWithoutTenantId() {
        LogEventDocument document = new LogEventDocument(
                "log-no-tenant",
                "payment-service",
                "ERROR",
                "Missing tenant",
                Instant.now()
        );

        assertThatThrownBy(() -> tenantScopedDocumentService.save(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId cannot be null or blank");
    }
}
