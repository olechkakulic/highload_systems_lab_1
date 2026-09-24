package ru.lab.shelter.dto.shelter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShelterInput(
    @NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 300) String address) {}
