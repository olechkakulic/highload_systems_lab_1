package ru.lab.shelter.service;

import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lab.shelter.dto.DtoMapper;
import ru.lab.shelter.dto.application.ApplicationInput;
import ru.lab.shelter.dto.application.ApplicationUpdate;
import ru.lab.shelter.dto.application.ApplicationView;
import ru.lab.shelter.dto.application.ReviewDecision;
import ru.lab.shelter.dto.application.ReviewInput;
import ru.lab.shelter.exception.ApiException;
import ru.lab.shelter.model.*;
import ru.lab.shelter.repository.*;

@Service
@Transactional(readOnly = true)
public class AdoptionService {
  private static final List<ApplicationStatus> ACTIVE =
      List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED);
  private final AdoptionApplicationRepository applications;
  private final AnimalRepository animals;
  private final CatalogService catalog;

  public AdoptionService(
      AdoptionApplicationRepository applications,
      AnimalRepository animals,
      CatalogService catalog) {
    this.applications = applications;
    this.animals = animals;
    this.catalog = catalog;
  }

  public Page<ApplicationView> list(Long animalId, Long applicantId, Pageable page) {
    return applications.search(animalId, applicantId, page).map(DtoMapper::application);
  }

  public ApplicationView get(Long id) {
    return DtoMapper.application(require(id));
  }

  private AdoptionApplication require(Long id) {
    return applications.findById(id).orElseThrow(() -> ApiException.notFound("Заявка", id));
  }

  private Animal lockAnimal(Long id) {
    return animals.findLockedById(id).orElseThrow(() -> ApiException.notFound("Животное", id));
  }

  // All writes for one animal acquire its row lock first, including new applications.
  private AdoptionApplication lockApplication(Long id) {
    Long animalId =
        applications.findAnimalId(id).orElseThrow(() -> ApiException.notFound("Заявка", id));
    lockAnimal(animalId);
    return require(id);
  }

  private void requireAvailable(Animal animal) {
    if (animal.getStatus() != AnimalStatus.AVAILABLE)
      throw ApiException.conflict("Животное уже передано владельцу");
  }

  @Transactional
  public ApplicationView create(ApplicationInput input) {
    Animal animal = lockAnimal(input.animalId());
    requireAvailable(animal);
    UserProfile applicant = catalog.requireUser(input.applicantId());
    if (applicant.getRole() != Role.APPLICANT)
      throw ApiException.conflict("Заявку можно оформить только на профиль заявителя");
    if (applications.existsByAnimalIdAndApplicantIdAndStatusIn(
        animal.getId(), applicant.getId(), ACTIVE)) {
      throw ApiException.conflict("У пользователя уже есть активная заявка на это животное");
    }
    AdoptionApplication e = new AdoptionApplication();
    e.setAnimal(animal);
    e.setApplicant(applicant);
    e.setComment(input.comment().trim());
    return DtoMapper.application(applications.save(e));
  }

  @Transactional
  public ApplicationView update(Long id, ApplicationUpdate input) {
    AdoptionApplication e = lockApplication(id);
    if (e.getStatus() != ApplicationStatus.PENDING)
      throw ApiException.conflict("Изменять комментарий можно только у новой заявки");
    e.setComment(input.comment().trim());
    return DtoMapper.application(e);
  }

  @Transactional
  public ApplicationView review(Long id, ReviewInput input) {
    AdoptionApplication e = lockApplication(id);
    requireAvailable(e.getAnimal());
    if (!ACTIVE.contains(e.getStatus())) throw ApiException.conflict("Заявка уже закрыта");
    if (input.decision() == ReviewDecision.APPROVE) {
      if (e.getStatus() != ApplicationStatus.PENDING)
        throw ApiException.conflict("Заявка уже одобрена");
      if (applications.existsByAnimalIdAndStatus(
          e.getAnimal().getId(), ApplicationStatus.APPROVED)) {
        throw ApiException.conflict("Для животного уже одобрена другая заявка");
      }
      e.setStatus(ApplicationStatus.APPROVED);
    } else {
      e.setStatus(ApplicationStatus.REJECTED);
    }
    return DtoMapper.application(e);
  }

  @Transactional
  public ApplicationView withdraw(Long id) {
    AdoptionApplication e = lockApplication(id);
    if (!ACTIVE.contains(e.getStatus()))
      throw ApiException.conflict("Отозвать можно только активную заявку");
    e.setStatus(ApplicationStatus.WITHDRAWN);
    return DtoMapper.application(e);
  }

  @Transactional
  public ApplicationView complete(Long id) {
    AdoptionApplication e = lockApplication(id);
    requireAvailable(e.getAnimal());
    if (e.getStatus() != ApplicationStatus.APPROVED)
      throw ApiException.conflict("Передача возможна только по одобренной заявке");
    e.getAnimal().setStatus(AnimalStatus.ADOPTED);
    e.setStatus(ApplicationStatus.COMPLETED);
    applications.closeOthers(e.getAnimal().getId(), e.getId(), ApplicationStatus.REJECTED, ACTIVE);
    return DtoMapper.application(e);
  }

  @Transactional
  public void delete(Long id) {
    AdoptionApplication e = lockApplication(id);
    if (ACTIVE.contains(e.getStatus()) || e.getStatus() == ApplicationStatus.COMPLETED) {
      throw ApiException.conflict("Удалять можно только отклонённые или отозванные заявки");
    }
    applications.delete(e);
  }
}
