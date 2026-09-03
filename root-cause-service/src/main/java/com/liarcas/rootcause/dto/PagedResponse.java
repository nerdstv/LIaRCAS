package com.liarcas.rootcause.dto;

import java.util.List;

/**
 * Generic paginated API response envelope.
 *
 * @param content page content
 * @param page zero-based page index
 * @param size requested page size
 * @param totalElements total number of matching elements across all pages
 * @param totalPages total number of pages
 * @param <T> content element type
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
