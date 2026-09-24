package ru.lab.shelter.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.lab.shelter.dto.common.ListResult;
import ru.lab.shelter.dto.user.UserInput;
import ru.lab.shelter.dto.user.UserUpdate;
import ru.lab.shelter.dto.user.UserView;
import ru.lab.shelter.service.CatalogService;

@RestController
@RequestMapping("/api/users")
@Tag(name = "users")
public class UserController {
  private final CatalogService service;

  public UserController(CatalogService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<ListResult<UserView>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return Pagination.page(service.users(Pagination.request(page, size)));
  }

  @GetMapping("/{id}")
  public UserView get(@PathVariable Long id) {
    return service.user(id);
  }

  @PostMapping
  public ResponseEntity<UserView> create(@Valid @RequestBody UserInput input) {
    var result = service.createUser(input);
    return ResponseEntity.created(URI.create("/api/users/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public UserView update(@PathVariable Long id, @Valid @RequestBody UserUpdate input) {
    return service.updateUser(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.deleteUser(id);
  }
}
