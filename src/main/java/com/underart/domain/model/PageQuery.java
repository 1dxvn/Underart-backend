package com.underart.domain.model;

public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        page = Math.max(page, 0);
        size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    public static PageQuery of(Integer page, Integer size) {
        return new PageQuery(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    public static PageQuery first() {
        return new PageQuery(0, DEFAULT_SIZE);
    }
}
