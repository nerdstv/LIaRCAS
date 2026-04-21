package com.liarcas.processing.config;

import java.time.Instant;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

@Configuration
public class ElasticsearchConfig {

    @Bean
    ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(List.of(
                new InstantToLongConverter(),
                new LongToInstantConverter()
        ));
    }

    @WritingConverter
    static class InstantToLongConverter implements Converter<Instant, Long> {
        @Override
        public Long convert(Instant source) {
            return source == null ? null : source.toEpochMilli();
        }
    }

    @ReadingConverter
    static class LongToInstantConverter implements Converter<Long, Instant> {
        @Override
        public Instant convert(Long source) {
            return source == null ? null : Instant.ofEpochMilli(source);
        }
    }
}