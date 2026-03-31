package com.carddemo.card;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * JPA {@link Specification} helpers for {@link Card} queries.
 */
public final class CardSpecifications {

    private CardSpecifications() {}

    public static Specification<Card> hasAccountId(Long accountId) {
        return (Root<Card> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("accountId"), accountId);
    }

    public static Specification<Card> cardNumberStartsWith(String prefix) {
        String p = prefix == null ? "" : prefix.trim();
        return (Root<Card> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate like = cb.like(root.get("cardNumber"), p + "%");
            return p.isEmpty() ? cb.conjunction() : like;
        };
    }
}
