package ru.lab.shelter.unit;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.*;
import ru.lab.shelter.controller.Pagination;
import ru.lab.shelter.exception.ApiException;

class PaginationTest {
  @ParameterizedTest
  @CsvSource({"-1,20", "0,0", "0,51", "2147483647,50"})
  void refusesInvalidOrExcessivePages(int page, int size) {
    assertThatThrownBy(() -> Pagination.request(page, size)).isInstanceOf(ApiException.class);
  }

  @Test
  void pagesExposeTotalInHeaderAndStableSort() {
    var page = Pagination.request(1, 2);
    var response = Pagination.page(new PageImpl<>(List.of("c", "d"), page, 5));
    assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("5");
    assertThat(response.getBody().hasNext()).isTrue();
    assertThat(page.getSort().getOrderFor("id").isAscending()).isTrue();
  }

  @Test
  void sliceDoesNotNeedTotalCount() {
    var result = Pagination.slice(new SliceImpl<>(List.of("a"), Pagination.request(0, 1), true));
    assertThat(result.items()).containsExactly("a");
    assertThat(result.hasNext()).isTrue();
  }
}
