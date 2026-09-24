package ru.lab.shelter.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.lab.shelter.model.Role;

public record UserInput(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @NotNull Role role,
    @Positive Long shelterId) {}
