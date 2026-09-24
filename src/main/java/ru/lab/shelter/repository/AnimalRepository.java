package ru.lab.shelter.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.lab.shelter.model.Animal;

public interface AnimalRepository
    extends JpaRepository<Animal, Long>, JpaSpecificationExecutor<Animal> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Animal a where a.id = :id")
  Optional<Animal> findLockedById(@Param("id") Long id);
}
