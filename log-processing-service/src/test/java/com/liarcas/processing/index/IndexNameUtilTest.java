package com.liarcas.processing.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IndexNameUtilTest {

    @Test
    void shouldGenerateTenantIndexNameWithValidTenantId() {
        String indexName = IndexNameUtil.getTenantIndexName("tenant-001");
        assertThat(indexName).isEqualTo("logs-tenant-001");
    }

    @Test
    void shouldGenerateTenantIndexNameAndSanitizeTenantId() {
        String indexName = IndexNameUtil.getTenantIndexName("TENANT_001");
        assertThat(indexName).isEqualTo("logs-tenant_001");
    }

    @Test
    void shouldSanitizeSpecialCharactersToHyphens() {
        String indexName = IndexNameUtil.getTenantIndexName("tenant@001#test");
        assertThat(indexName).isEqualTo("logs-tenant-001-test");
    }

    @Test
    void shouldHandleMultipleTenants() {
        String index1 = IndexNameUtil.getTenantIndexName("tenant-001");
        String index2 = IndexNameUtil.getTenantIndexName("tenant-002");
        String index3 = IndexNameUtil.getTenantIndexName("org-acme");

        assertThat(index1).isEqualTo("logs-tenant-001");
        assertThat(index2).isEqualTo("logs-tenant-002");
        assertThat(index3).isEqualTo("logs-org-acme");
    }

    @Test
    void shouldThrowExceptionForNullTenantId() {
        assertThatThrownBy(() -> IndexNameUtil.getTenantIndexName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId cannot be null or blank");
    }

    @Test
    void shouldThrowExceptionForBlankTenantId() {
        assertThatThrownBy(() -> IndexNameUtil.getTenantIndexName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId cannot be null or blank");
    }

    @Test
    void shouldRemoveLeadingSpecialCharacters() {
        // Index names cannot start with . or -
        String indexName = IndexNameUtil.getTenantIndexName("---tenant-001");
        assertThat(indexName).isEqualTo("logs-tenant-001");
    }

    @Test
    void shouldPreserveDotsUnderscoresAndHyphens() {
        String indexName = IndexNameUtil.getTenantIndexName("tenant.org_name-001");
        assertThat(indexName).isEqualTo("logs-tenant.org_name-001");
    }

    @Test
    void shouldBeConsistentForSameTenantId() {
        String index1 = IndexNameUtil.getTenantIndexName("tenant-001");
        String index2 = IndexNameUtil.getTenantIndexName("tenant-001");
        assertThat(index1).isEqualTo(index2);
    }

    @Test
    void shouldAlwaysLowercaseIndexName() {
        String index1 = IndexNameUtil.getTenantIndexName("TENANT-001");
        String index2 = IndexNameUtil.getTenantIndexName("Tenant-001");
        String index3 = IndexNameUtil.getTenantIndexName("tenant-001");

        assertThat(index1).isEqualTo(index2).isEqualTo(index3);
    }
}
