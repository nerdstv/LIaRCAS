package com.liarcas.rootcause.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import com.liarcas.rootcause.document.LogEventDocument;
import com.liarcas.rootcause.dto.LogEntryResponse;
import com.liarcas.rootcause.dto.LogQueryFilter;
import com.liarcas.rootcause.dto.PagedResponse;
import com.liarcas.rootcause.index.IndexNameUtil;

/**
 * Queries tenant-scoped Elasticsearch indices for log entries.
 *
 * Tenant isolation is enforced by always resolving the index from the caller's
 * authenticated tenantId; no request input can select a different tenant's index.
 */
@Service
public class LogQueryService {

    /** Log levels accepted by the ingestion pipeline; used to validate the level filter. */
    public static final Set<String> VALID_LEVELS = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL");

    private static final int MAX_PAGE_SIZE = 200;
    private static final String TIMESTAMP_FIELD = "timestamp";

    private final ElasticsearchOperations elasticsearchOperations;
    private final IndexNameUtil indexNameUtil;

    public LogQueryService(ElasticsearchOperations elasticsearchOperations, IndexNameUtil indexNameUtil) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.indexNameUtil = indexNameUtil;
    }

    /**
     * Queries logs belonging to a single tenant, applying the given filters and pagination.
     *
     * @param tenantId tenant identifier resolved from the authenticated caller
     * @param filter optional filters to narrow the result set
     * @param page zero-based page index
     * @param size page size, capped at {@value #MAX_PAGE_SIZE}
     * @return a page of matching log entries scoped to the tenant
     */
    public PagedResponse<LogEntryResponse> queryLogs(String tenantId, LogQueryFilter filter, int page, int size) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (filter.level() != null && !VALID_LEVELS.contains(filter.level().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("level must be one of " + VALID_LEVELS);
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new IllegalArgumentException("from must not be after to");
        }

        String tenantIndexName = indexNameUtil.getTenantIndexName(tenantId);
        IndexCoordinates indexCoordinates = IndexCoordinates.of(tenantIndexName);

        if (!elasticsearchOperations.indexOps(indexCoordinates).exists()) {
            return new PagedResponse<>(List.of(), page, size, 0, 0);
        }

        Criteria criteria = buildCriteria(filter);
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, TIMESTAMP_FIELD)));

        SearchHits<LogEventDocument> searchHits =
                elasticsearchOperations.search(query, LogEventDocument.class, indexCoordinates);

        List<LogEntryResponse> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .toList();

        long totalElements = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PagedResponse<>(content, page, size, totalElements, totalPages);
    }

    private Criteria buildCriteria(LogQueryFilter filter) {
        // "tenantId exists" is always true for a valid document; it anchors the query so an
        // empty filter set still produces a valid match-all-in-index Elasticsearch query.
        Criteria criteria = Criteria.where("tenantId").exists();

        if (filter.serviceName() != null && !filter.serviceName().isBlank()) {
            criteria = criteria.and(Criteria.where("serviceName").is(filter.serviceName()));
        }
        if (filter.level() != null && !filter.level().isBlank()) {
            criteria = criteria.and(Criteria.where("level").is(filter.level().toUpperCase(java.util.Locale.ROOT)));
        }
        if (filter.from() != null) {
            criteria = criteria.and(Criteria.where(TIMESTAMP_FIELD).greaterThanEqual(filter.from()));
        }
        if (filter.to() != null) {
            criteria = criteria.and(Criteria.where(TIMESTAMP_FIELD).lessThanEqual(filter.to()));
        }

        return criteria;
    }

    private LogEntryResponse toResponse(LogEventDocument document) {
        return new LogEntryResponse(
                document.getId(),
                document.getServiceName(),
                document.getComponent(),
                document.getEnvironment(),
                document.getServiceVersion(),
                document.getInstanceId(),
                document.getTraceId(),
                document.getLevel(),
                document.getMessage(),
                document.getExceptionType(),
                document.getStackTraceHash(),
                document.getTimestamp()
        );
    }
}
