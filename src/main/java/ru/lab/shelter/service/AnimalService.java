package ru.lab.shelter.service;

import java.util.HashSet;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lab.shelter.dto.animal.AnimalInput;
import ru.lab.shelter.dto.animal.AnimalView;
import ru.lab.shelter.dto.DtoMapper;
import ru.lab.shelter.exception.ApiException;
import ru.lab.shelter.model.*;
import ru.lab.shelter.repository.*;

@Service
@Transactional(readOnly = true)
public class AnimalService {
  private final AnimalRepository animals;
  private final TagRepository tags;
  private final CatalogService catalog;

  public AnimalService(AnimalRepository animals, TagRepository tags, CatalogService catalog) {
    this.animals = animals;
    this.tags = tags;
    this.catalog = catalog;
  }

  public Page<AnimalView> list(
      Long shelterId, Species species, AnimalStatus status, Long tagId, Pageable page) {
    Specification<Animal> spec = (root, query, cb) -> cb.conjunction();
    if (shelterId != null)
      spec = spec.and((root, q, cb) -> cb.equal(root.get("shelter").get("id"), shelterId));
    if (species != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("species"), species));
    if (status != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
    if (tagId != null)
      spec = spec.and((root, q, cb) -> cb.equal(root.join("tags").get("id"), tagId));
    return animals.findAll(spec, page).map(DtoMapper::animal);
  }

  public AnimalView get(Long id) {
    return DtoMapper.animal(
        animals.findById(id).orElseThrow(() -> ApiException.notFound("Животное", id)));
  }

  @Transactional
  public AnimalView create(AnimalInput input) {
    Animal e = new Animal();
    apply(e, input);
    return DtoMapper.animal(animals.save(e));
  }

  @Transactional
  public AnimalView update(Long id, AnimalInput input) {
    Animal e = lock(id);
    if (e.getStatus() == AnimalStatus.ADOPTED)
      throw ApiException.conflict("Карточку переданного животного нельзя менять");
    apply(e, input);
    return DtoMapper.animal(e);
  }

  private void apply(Animal e, AnimalInput input) {
    var selected = tags.findAllById(input.tagIds());
    if (selected.size() != input.tagIds().size())
      throw ApiException.badRequest("Один или несколько тегов не существуют");
    e.setName(input.name().trim());
    e.setSpecies(input.species());
    e.setBirthDate(input.birthDate());
    e.setDescription(input.description().trim());
    e.setShelter(catalog.requireShelter(input.shelterId()));
    e.setTags(new HashSet<>(selected));
  }

  @Transactional
  public void delete(Long id) {
    Animal e = lock(id);
    if (e.getStatus() == AnimalStatus.ADOPTED)
      throw ApiException.conflict("Нельзя удалить переданное животное");
    animals.delete(e);
  }

  private Animal lock(Long id) {
    return animals.findLockedById(id).orElseThrow(() -> ApiException.notFound("Животное", id));
  }
}
