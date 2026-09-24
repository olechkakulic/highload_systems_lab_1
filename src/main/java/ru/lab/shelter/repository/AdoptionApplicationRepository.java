package ru.lab.shelter.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.lab.shelter.model.*;

public interface AdoptionApplicationRepository extends JpaRepository<AdoptionApplication, Long> {
  @Query("select a.animal.id from AdoptionApplication a where a.id = :id")
  Optional<Long> findAnimalId(@Param("id") Long id);

  boolean existsByAnimalIdAndApplicantIdAndStatusIn(
      Long animalId, Long applicantId, Collection<ApplicationStatus> statuses);

  boolean existsByAnimalIdAndStatus(Long animalId, ApplicationStatus status);

  @Query(
      "select a from AdoptionApplication a where (:animalId is null or a.animal.id = :animalId) and"
          + " (:applicantId is null or a.applicant.id = :applicantId)")
  Page<AdoptionApplication> search(
      @Param("animalId") Long animalId, @Param("applicantId") Long applicantId, Pageable pageable);

  @Modifying(flushAutomatically = true)
  @Query(
      "update AdoptionApplication a set a.status = :closed where a.animal.id = :animalId and a.id"
          + " <> :selectedId and a.status in :active")
  int closeOthers(
      @Param("animalId") Long animalId,
      @Param("selectedId") Long selectedId,
      @Param("closed") ApplicationStatus closed,
      @Param("active") Collection<ApplicationStatus> active);
}
