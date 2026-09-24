package ru.lab.shelter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "tags")
public class Tag extends BaseEntity {
  @NotBlank
  @Size(max = 60)
  @Column(nullable = false, unique = true, length = 60)
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
