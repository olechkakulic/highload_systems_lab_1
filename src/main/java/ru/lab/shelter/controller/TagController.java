package ru.lab.shelter.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.lab.shelter.dto.common.ListResult;
import ru.lab.shelter.dto.tag.TagInput;
import ru.lab.shelter.dto.tag.TagView;
import ru.lab.shelter.service.CatalogService;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "tags")
public class TagController {
  private final CatalogService service;

  public TagController(CatalogService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<ListResult<TagView>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return Pagination.page(service.tags(Pagination.request(page, size)));
  }

  @GetMapping("/{id}")
  public TagView get(@PathVariable Long id) {
    return service.tag(id);
  }

  @PostMapping
  public ResponseEntity<TagView> create(@Valid @RequestBody TagInput input) {
    var result = service.createTag(input);
    return ResponseEntity.created(URI.create("/api/tags/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public TagView update(@PathVariable Long id, @Valid @RequestBody TagInput input) {
    return service.updateTag(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.deleteTag(id);
  }
}
