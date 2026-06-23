package com.liarcas.processing.config;

import java.time.Instant;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

/**
 * Registers Elasticsearch converters used by the processing service.
 */
@Configuration
public class ElasticsearchConfig {

    /**
     * Provides custom conversions for Instant persistence in Elasticsearch.
     *
     * @return custom conversion registry
     */
    @Bean
    ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(List.of(
                new InstantToLongConverter(),
                new LongToInstantConverter()
        ));
    }

    /**
     * Converts Instant values to epoch milliseconds for indexing.
     */
    @WritingConverter
    static class InstantToLongConverter implements Converter<Instant, Long> {

        /**
         * Converts an Instant to epoch milliseconds.
         *
         * @param source source instant
         * @return epoch milliseconds or null when source is null
         */
        @Override
        public Long convert(Instant source) {
            return source == null ? null : source.toEpochMilli();
        }
    }

    /**
     * Converts epoch milliseconds back to Instant values.
     */
    @ReadingConverter
    static class LongToInstantConverter implements Converter<Long, Instant> {

        /**
         * Converts epoch milliseconds to an Instant.
         *
         * @param source epoch milliseconds
         * @return instant or null when source is null
         */
        @Override
        public Instant convert(Long source) {
            return source == null ? null : Instant.ofEpochMilli(source);
        }
    }
}