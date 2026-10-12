package kz.jqd.arqa.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** One page of results; {@code page} is zero-based. */
public record PagedResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <S, T> PagedResponse<T> from(Page<S> page, Function<S, T> itemMapper) {
        return new PagedResponse<>(page.getContent().stream().map(itemMapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
