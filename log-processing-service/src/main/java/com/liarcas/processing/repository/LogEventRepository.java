package com.liarcas.processing.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.liarcas.processing.document.LogEventDocument;

/**
 * Spring Data repository for persisted log event documents.
 */
public interface LogEventRepository extends ElasticsearchRepository<LogEventDocument, String> {

}
