package ru.lab.shelter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.lab.shelter.model.Shelter;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {}
