package com.liarcas.processing.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IndexNameUtilTest {

    @Test
    void shouldGenerateTenantIndexNameWithValidTenantId() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        String indexName = indexNameUtil.getTenantIndexName("tenant-001");

        assertThat(indexName).isEqualTo("liarcas-logs-tenant-001");
    }

    @Test
    void shouldGenerateTenantIndexNameAndSanitizeTenantId() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        String indexName = indexNameUtil.getTenantIndexName("TENANT_001");

        assertThat(indexName).isEqualTo("liarcas-logs-tenant_001");
    }

    @Test
    void shouldSanitizeSpecialCharactersToHyphens() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        String indexName = indexNameUtil.getTenantIndexName("tenant@001#test");

        assertThat(indexName).isEqualTo("liarcas-logs-tenant-001-test");
    }

    @Test
    void shouldHandleMultipleTenants() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        String index1 = indexNameUtil.getTenantIndexName("tenant-001");
        String index2 = indexNameUtil.getTenantIndexName("tenant-002");
        String index3 = indexNameUtil.getTenantIndexName("org-acme");

        assertThat(index1).isEqualTo("liarcas-logs-tenant-001");
        assertThat(index2).isEqualTo("liarcas-logs-tenant-002");
        assertThat(index3).isEqualTo("liarcas-logs-org-acme");
    }

    @Test
    void shouldThrowExceptionForNullTenantId() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        assertThatThrownBy(() -> indexNameUtil.getTenantIndexName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId cannot be null or blank");
    }

    @Test
    void shouldThrowExceptionForBlankTenantId() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        assertThatThrownBy(() -> indexNameUtil.getTenantIndexName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId cannot be null or blank");
    }

    @Test
    void shouldRemoveLeadingSpecialCharacters() {
        // Index names cannot start with . or -
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        String indexName = indexNameUtil.getTenantIndexName("---tenant-001");

        assertThat(indexName).isEqualTo("liarcas-logs-tenant-001");
    }

    @Test
    void shouldPreserveDotsUnderscoresAndHyphens() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        String indexName = indexNameUtil.getTenantIndexName("tenant.org_name-001");

        assertThat(indexName).isEqualTo("liarcas-logs-tenant.org_name-001");
    }

    @Test
    void shouldBeConsistentForSameTenantId() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        String index1 = indexNameUtil.getTenantIndexName("tenant-001");
        String index2 = indexNameUtil.getTenantIndexName("tenant-001");

        assertThat(index1).isEqualTo(index2);
    }

    @Test
    void shouldAlwaysLowercaseIndexName() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("liarcas-logs-{tenantId}");

        String index1 = indexNameUtil.getTenantIndexName("TENANT-001");
        String index2 = indexNameUtil.getTenantIndexName("Tenant-001");
        String index3 = indexNameUtil.getTenantIndexName("tenant-001");

        assertThat(index1).isEqualTo(index2).isEqualTo(index3);
    }

    @Test
    void shouldApplyConfiguredPatternWithTenantPlaceholder() {
        IndexNameUtil indexNameUtil = new IndexNameUtil("acme-logs-{tenantId}-events");

        String indexName = indexNameUtil.getTenantIndexName("tenant-001");

        assertThat(indexName).isEqualTo("acme-logs-tenant-001-events");
    }

    @Test
    void shouldFailFastWhenPatternHasNoTenantPlaceholder() {
        assertThatThrownBy(() -> new IndexNameUtil("logs-static"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must include {tenantId}");
    }

    @Test
    void shouldFailFastWhenPatternContainsInvalidCharacters() {
        assertThatThrownBy(() -> new IndexNameUtil("Logs-{tenantId}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid characters");
    }
}
