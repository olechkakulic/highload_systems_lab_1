package ru.lab.shelter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.lab.shelter.model.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {}
