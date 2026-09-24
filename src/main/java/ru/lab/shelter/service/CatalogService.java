package ru.lab.shelter.service;

import java.util.Locale;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lab.shelter.dto.DtoMapper;
import ru.lab.shelter.dto.shelter.ShelterInput;
import ru.lab.shelter.dto.shelter.ShelterView;
import ru.lab.shelter.dto.tag.TagInput;
import ru.lab.shelter.dto.tag.TagView;
import ru.lab.shelter.dto.user.UserInput;
import ru.lab.shelter.dto.user.UserUpdate;
import ru.lab.shelter.dto.user.UserView;
import ru.lab.shelter.exception.ApiException;
import ru.lab.shelter.model.*;
import ru.lab.shelter.repository.*;

@Service
@Transactional(readOnly = true)
public class CatalogService {
  private final ShelterRepository shelters;
  private final UserProfileRepository users;
  private final TagRepository tags;

  public CatalogService(
      ShelterRepository shelters, UserProfileRepository users, TagRepository tags) {
    this.shelters = shelters;
    this.users = users;
    this.tags = tags;
  }

  public Shelter requireShelter(Long id) {
    return shelters.findById(id).orElseThrow(() -> ApiException.notFound("Приют", id));
  }

  public UserProfile requireUser(Long id) {
    return users.findById(id).orElseThrow(() -> ApiException.notFound("Пользователь", id));
  }

  public Tag requireTag(Long id) {
    return tags.findById(id).orElseThrow(() -> ApiException.notFound("Тег", id));
  }

  public Page<ShelterView> shelters(Pageable page) {
    return shelters.findAll(page).map(DtoMapper::shelter);
  }

  public ShelterView shelter(Long id) {
    return DtoMapper.shelter(requireShelter(id));
  }

  @Transactional
  public ShelterView createShelter(ShelterInput input) {
    Shelter e = new Shelter();
    applyShelter(e, input);
    return DtoMapper.shelter(shelters.save(e));
  }

  @Transactional
  public ShelterView updateShelter(Long id, ShelterInput input) {
    Shelter e = requireShelter(id);
    applyShelter(e, input);
    return DtoMapper.shelter(e);
  }

  private void applyShelter(Shelter e, ShelterInput input) {
    e.setName(input.name().trim());
    e.setAddress(input.address().trim());
  }

  @Transactional
  public void deleteShelter(Long id) {
    shelters.delete(requireShelter(id));
  }

  public Page<UserView> users(Pageable page) {
    return users.findAll(page).map(DtoMapper::user);
  }

  public UserView user(Long id) {
    return DtoMapper.user(requireUser(id));
  }

  @Transactional
  public UserView createUser(UserInput input) {
    UserProfile e = new UserProfile();
    e.setRole(input.role());
    applyUser(e, input.name(), input.email(), input.shelterId());
    return DtoMapper.user(users.save(e));
  }

  @Transactional
  public UserView updateUser(Long id, UserUpdate input) {
    UserProfile e = requireUser(id);
    applyUser(e, input.name(), input.email(), input.shelterId());
    return DtoMapper.user(e);
  }

  private void applyUser(UserProfile e, String name, String email, Long shelterId) {
    if (e.getRole() == Role.EMPLOYEE && shelterId == null)
      throw ApiException.badRequest("Для сотрудника нужно указать приют");
    e.setName(name.trim());
    e.setEmail(email.trim().toLowerCase(Locale.ROOT));
    e.setShelter(shelterId == null ? null : requireShelter(shelterId));
  }

  @Transactional
  public void deleteUser(Long id) {
    users.delete(requireUser(id));
  }

  public Page<TagView> tags(Pageable page) {
    return tags.findAll(page).map(DtoMapper::tag);
  }

  public TagView tag(Long id) {
    return DtoMapper.tag(requireTag(id));
  }

  @Transactional
  public TagView createTag(TagInput input) {
    Tag e = new Tag();
    e.setName(input.name().trim().toLowerCase(Locale.ROOT));
    return DtoMapper.tag(tags.save(e));
  }

  @Transactional
  public TagView updateTag(Long id, TagInput input) {
    Tag e = requireTag(id);
    e.setName(input.name().trim().toLowerCase(Locale.ROOT));
    return DtoMapper.tag(e);
  }

  @Transactional
  public void deleteTag(Long id) {
    tags.delete(requireTag(id));
  }
}
