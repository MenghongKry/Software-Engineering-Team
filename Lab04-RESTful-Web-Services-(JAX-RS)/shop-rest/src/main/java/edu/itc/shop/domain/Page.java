package edu.itc.shop.domain;

import java.util.List;

/** One page of a filtered list: the items plus what is needed for paging links. */
public record Page<T>(List<T> items, int page, int size, int total) {

    public int lastPage() {
        return Math.max(0, (total - 1) / size);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page < lastPage();
    }
}
