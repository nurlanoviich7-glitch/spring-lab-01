package kz.iitu.springlab.web.dto;

import java.util.List;

public record BookPageResponse(List<BookResponse> content, int page, int size,
                               long totalElements, long totalPages) { }
