package com.liarcas.processing.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.liarcas.processing.document.LogEventDocument;

public interface LogEventRepository extends ElasticsearchRepository<LogEventDocument, String> {
    
}
