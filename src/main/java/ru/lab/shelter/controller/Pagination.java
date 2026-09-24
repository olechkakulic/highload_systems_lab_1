package ru.lab.shelter.controller;

import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import ru.lab.shelter.dto.common.ListResult;
import ru.lab.shelter.exception.ApiException;

public final class Pagination {
  private Pagination() {}

  public static Pageable request(int page, int size) {
    if (page < 0 || size < 1 || size > 50 || (long) page * size > Integer.MAX_VALUE) {
      throw ApiException.badRequest(
          "page должен быть >= 0; size — от 1 до 50; смещение — не больше 2147483647");
    }
    return PageRequest.of(page, size, Sort.by("id").ascending());
  }

  public static <T> ResponseEntity<ListResult<T>> page(Page<T> data) {
    return ResponseEntity.ok()
        .header("X-Total-Count", Long.toString(data.getTotalElements()))
        .body(slice(data));
  }

  public static <T> ListResult<T> slice(Slice<T> data) {
    return new ListResult<>(data.getContent(), data.getNumber(), data.getSize(), data.hasNext());
  }
}
