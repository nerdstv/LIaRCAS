package com.liarcas.processing.service;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import com.liarcas.processing.document.LogEventDocument;
import com.liarcas.processing.index.IndexNameUtil;

/**
 * Service for persisting LogEventDocuments to tenant-scoped Elasticsearch indices.
 * 
 * Each tenant gets its own index (e.g., logs-tenant-001, logs-tenant-002),
 * ensuring complete data isolation and independent index management per tenant.
 */
@Service
public class TenantScopedDocumentService {

    private final ElasticsearchOperations elasticsearchOperations;

    public TenantScopedDocumentService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * Save a log event document to the tenant-scoped index.
     * 
     * The index name is derived from the document's tenantId.
     * 
     * @param document the LogEventDocument to save
     * @return the saved document
     * @throws IllegalArgumentException if tenantId is null or blank
     */
    public LogEventDocument save(LogEventDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        if (document.getTenantId() == null || document.getTenantId().isBlank()) {
            throw new IllegalArgumentException("Document tenantId cannot be null or blank");
        }

        String tenantIndexName = IndexNameUtil.getTenantIndexName(document.getTenantId());
        
        // Create an IndexQuery with the tenant-specific index
        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(document.getId())
                .withObject(document)
                .build();
        
        // Index the document to the tenant-scoped index using IndexCoordinates.of()
        IndexCoordinates indexCoordinates = IndexCoordinates.of(tenantIndexName);
        elasticsearchOperations.index(indexQuery, indexCoordinates);
        
        return document;
    }
}
