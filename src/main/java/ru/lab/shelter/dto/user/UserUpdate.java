package ru.lab.shelter.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UserUpdate(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @Positive Long shelterId) {}
