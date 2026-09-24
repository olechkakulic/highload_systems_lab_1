package ru.lab.shelter.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.lab.shelter.model.*;

public interface VolunteerShiftRepository extends JpaRepository<VolunteerShift, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from VolunteerShift s where s.id = :id")
  Optional<VolunteerShift> findLockedById(@Param("id") Long id);

  @Query(
      "select s from VolunteerShift s where (:shelterId is null or s.shelter.id = :shelterId) and"
          + " (:status is null or s.status = :status)")
  Slice<VolunteerShift> search(
      @Param("shelterId") Long shelterId, @Param("status") ShiftStatus status, Pageable pageable);
}
