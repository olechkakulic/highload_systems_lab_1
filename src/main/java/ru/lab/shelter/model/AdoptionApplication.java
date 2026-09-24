package ru.lab.shelter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "adoption_applications")
public class AdoptionApplication extends BaseEntity {
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "animal_id", nullable = false)
  private Animal animal;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "applicant_id", nullable = false)
  private UserProfile applicant;

  @NotBlank
  @Size(max = 2000)
  @Column(nullable = false, length = 2000)
  private String comment;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ApplicationStatus status = ApplicationStatus.PENDING;

  @NotNull
  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public Animal getAnimal() {
    return animal;
  }

  public void setAnimal(Animal animal) {
    this.animal = animal;
  }

  public UserProfile getApplicant() {
    return applicant;
  }

  public void setApplicant(UserProfile applicant) {
    this.applicant = applicant;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public ApplicationStatus getStatus() {
    return status;
  }

  public void setStatus(ApplicationStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
