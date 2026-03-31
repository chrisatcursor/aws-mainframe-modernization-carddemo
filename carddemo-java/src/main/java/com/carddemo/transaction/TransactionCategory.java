package com.carddemo.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * JPA entity mapped from COBOL copybook CVTRA04Y (TRAN-CAT-RECORD, RECLN 60).
 * Backed by VSAM file TRANCATG.
 * Composite key: (typeCode, categoryCode).
 */
@Entity
@Table(name = "transaction_categories")
public class TransactionCategory {

    @EmbeddedId
    private TransactionCategoryKey id;

    @Column(name = "description", length = 50)
    private String description;

    /** DB2 TRC_TYPE_CATEGORY (CHAR 4)); aligned with Phase 5 / DCLTRCAT. */
    @Column(name = "category_code", length = 4, nullable = false)
    private String categoryCode;

    protected TransactionCategory() {}

    /** Detached instance for tests and seed helpers. */
    public static TransactionCategory of(String typeCode, int categoryCode, String description) {
        TransactionCategory c = new TransactionCategory();
        c.setId(new TransactionCategoryKey(typeCode, categoryCode));
        c.setDescription(description);
        c.setCategoryCode(String.format("%04d", categoryCode));
        return c;
    }

    public TransactionCategoryKey getId() { return id; }
    public void setId(TransactionCategoryKey id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    @Embeddable
    public static class TransactionCategoryKey implements Serializable {

        @Column(name = "type_cd", length = 2)
        private String typeCode;

        @Column(name = "cat_cd")
        private Integer categoryCode;

        protected TransactionCategoryKey() {}

        public TransactionCategoryKey(String typeCode, Integer categoryCode) {
            this.typeCode = typeCode;
            this.categoryCode = categoryCode;
        }

        public String getTypeCode() { return typeCode; }
        public Integer getCategoryCode() { return categoryCode; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TransactionCategoryKey that)) return false;
            return Objects.equals(typeCode, that.typeCode)
                    && Objects.equals(categoryCode, that.categoryCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeCode, categoryCode);
        }
    }
}
