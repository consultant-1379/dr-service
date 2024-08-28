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
package com.ericsson.bos.dr.model.mappers;

import com.ericsson.bos.dr.jpa.JPASpecificationVisitor;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.springframework.data.jpa.domain.Specification;

/**
 * Map FIQL query string to a JPA specification.
 * @param <E> Entity class
 */
public class SpecificationMapper<E> implements Mapper<String, Specification<E>> {

    private FilterableAndSortableProperties<E> filterableProperties;

    /**
     * SpecificationMapper
     * @param filterableProperties filterable properties for the entity along with the mapped property name.
     */
    public SpecificationMapper(final FilterableAndSortableProperties<E> filterableProperties) {
        this.filterableProperties = filterableProperties;
    }

    @Override
    public Specification<E> apply(String filterQueryParam) {
        try  {
            final FiqlParser<SearchBean> fiqlParser = new FiqlParser<>(SearchBean.class);
            final SearchCondition<SearchBean> searchCondition = fiqlParser.parse(filterQueryParam);
            final JPASpecificationVisitor<SearchBean, E> visitor = new JPASpecificationVisitor<>(filterableProperties);
            searchCondition.accept(visitor);
            return visitor.getQuery();
        } catch(final SearchParseException e) {
            throw new DRServiceException(ErrorCode.INVALID_FILTER_PARAM, e.getMessage());
        }
    }
}
