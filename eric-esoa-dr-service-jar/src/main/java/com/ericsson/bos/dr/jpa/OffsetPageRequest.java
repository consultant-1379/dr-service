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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * <code>Pageable</code> implementation to support offset based paging.
 */
public class OffsetPageRequest implements Pageable {

    public static final int DEFAULT_OFFSET = 0;

    private final int limit;
    private final int offset;
    private final Sort sort;

    /**
     * OffsetPageRequest.
     * @param offset offset
     * @param limit limit
     * @param sort sort
     */
    public OffsetPageRequest(int offset, int limit, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be less than zero!");
        } else if (limit < 1) {
            throw new IllegalArgumentException("Limit must not be less than one!");
        }
        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return offset/limit;
    }

    @Override
    public int getPageSize() {
        return this.limit;
    }

    @Override
    public long getOffset() {
        return this.offset;
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, getPageSize(), getSort());
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest(pageNumber, limit, sort);
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(((int) getOffset()) + getPageSize(), getPageSize(), getSort());
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    private OffsetPageRequest previous() {
        return hasPrevious() ?
                new OffsetPageRequest(((int) getOffset()) - getPageSize(), getPageSize(), getSort()) : this;
    }

    @Override
    public boolean hasPrevious() {
        return offset > limit;
    }
}