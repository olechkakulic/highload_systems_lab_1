package ru.lab.shelter.repository;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.lab.shelter.model.*;

public interface ShiftParticipationRepository extends JpaRepository<ShiftParticipation, Long> {
  long countByShiftIdAndStatus(Long shiftId, ParticipationStatus status);

  Optional<ShiftParticipation> findByShiftIdAndUserId(Long shiftId, Long userId);

  Page<ShiftParticipation> findByShiftId(Long shiftId, Pageable pageable);

  @Modifying(flushAutomatically = true)
  @Query(
      "update ShiftParticipation p set p.status = :cancelled where p.shift.id = :shiftId and"
          + " p.status = :registered")
  int cancelAll(
      @Param("shiftId") Long shiftId,
      @Param("cancelled") ParticipationStatus cancelled,
      @Param("registered") ParticipationStatus registered);
}
