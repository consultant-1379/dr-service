/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.bos.dr.jpa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import com.ericsson.bos.dr.jpa.specifications.FIQLExpression;
import com.ericsson.bos.dr.model.mappers.FilterableAndSortableProperties;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.OrSearchCondition;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionVisitor;
import org.springframework.data.jpa.domain.Specification;

/**
 * <code>SearchConditionVisitor</code> implementation to translate the FIQL search conditions to a
 * JPA <code>Specification</code>.
 * <ul>
 *  <li>Supports FIQL equals expression only.</li>
 *  <li>Supports string property value types only in JPA specification.</li>
 *  <li>Supports nested property names e.g jobSpecification.featurePackName</li>
 * </ul>
 * @param <T> Search condition type
 * @param <E> Entity type
 */
public class JPASpecificationVisitor<T, E> implements SearchConditionVisitor<T, Specification<E>> {

    private final Deque<List<Specification<E>>> specificationsStack = new ArrayDeque<>();
    private final FilterableAndSortableProperties<E> filterableProperties;

    /**
     * Constructor takes <code>FilterableAndSortableProperties</code>.
     *
     * @param filterableProperties
     *         FilterableAndSortableProperties
     */
    public JPASpecificationVisitor(final FilterableAndSortableProperties<E> filterableProperties) {
        this.filterableProperties = filterableProperties;
    }

    @Override
    public void visit(SearchCondition<T> searchCondition) {
        if (specificationsStack.isEmpty()) {
            specificationsStack.push(new ArrayList<>());
        }
        final PrimitiveStatement statement = searchCondition.getStatement();
        if (statement != null) { //single filter condition
            validateProperty(statement.getProperty());
            validateCondition(statement.getCondition());
            specificationsStack.peek().add(getSpecification(statement));
        } else { // composite filter condition
            specificationsStack.push(new ArrayList<>());
            for (SearchCondition<T> condition: searchCondition.getSearchConditions()) {
                condition.accept(this);
            }
            final List<Specification<E>> specifications = specificationsStack.pop();
            final Optional<Specification<E>> newSpecification;
            if (searchCondition instanceof OrSearchCondition) {
                newSpecification = specifications.stream().reduce(Specification::or);
            } else {
                newSpecification = specifications.stream().reduce(Specification::and);
            }
            newSpecification.ifPresent(s -> specificationsStack.peek().add(s));
        }
    }

    @Override
    public Specification<E> getQuery() {
        return specificationsStack.peek().iterator().next();
    }

    private Specification<E> getSpecification(final PrimitiveStatement statement) {
        final String property = statement.getProperty();
        return filterableProperties.getFilterSpecification(property)
                .get(new FIQLExpression(filterableProperties.getMappedName(property), statement.getValue()));
    }

    private void validateProperty(String property) {
        if (!filterableProperties.isFilterable(property)) {
            throw new DRServiceException(ErrorCode.INVALID_FILTER_PARAM,
                    String.format("Filtering not supported for property %s", property));
        }
    }

    private void validateCondition(ConditionType conditionType) {
        if (!ConditionType.EQUALS.equals(conditionType)) {
            throw new DRServiceException(ErrorCode.INVALID_FILTER_PARAM, "Filter supports '==' condition only");
        }
    }
}