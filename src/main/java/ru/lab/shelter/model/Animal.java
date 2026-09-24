package ru.lab.shelter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "animals")
public class Animal extends BaseEntity {
  @NotBlank
  @Size(max = 120)
  @Column(nullable = false, length = 120)
  private String name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Species species;

  @NotNull
  @PastOrPresent
  @Column(nullable = false)
  private LocalDate birthDate;

  @NotBlank
  @Size(max = 2000)
  @Column(nullable = false, length = 2000)
  private String description;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AnimalStatus status = AnimalStatus.AVAILABLE;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shelter_id", nullable = false)
  private Shelter shelter;

  @Size(max = 50)
  @ManyToMany
  @JoinTable(
      name = "animal_tags",
      joinColumns = @JoinColumn(name = "animal_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Species getSpecies() {
    return species;
  }

  public void setSpecies(Species species) {
    this.species = species;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(LocalDate birthDate) {
    this.birthDate = birthDate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public AnimalStatus getStatus() {
    return status;
  }

  public void setStatus(AnimalStatus status) {
    this.status = status;
  }

  public Shelter getShelter() {
    return shelter;
  }

  public void setShelter(Shelter shelter) {
    this.shelter = shelter;
  }

  public Set<Tag> getTags() {
    return tags;
  }

  public void setTags(Set<Tag> tags) {
    this.tags = tags;
  }
}
