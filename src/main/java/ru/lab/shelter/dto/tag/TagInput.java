package ru.lab.shelter.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagInput(@NotBlank @Size(max = 60) String name) {}
