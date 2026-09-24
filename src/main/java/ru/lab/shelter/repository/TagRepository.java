package ru.lab.shelter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.lab.shelter.model.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {}
