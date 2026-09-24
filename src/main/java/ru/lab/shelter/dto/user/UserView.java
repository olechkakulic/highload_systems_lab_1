package ru.lab.shelter.dto.user;

import ru.lab.shelter.model.Role;

public record UserView(Long id, String name, String email, Role role, Long shelterId) {}
