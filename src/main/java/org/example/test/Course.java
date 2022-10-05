package org.example.test;

        import javax.persistence.*;
        import java.util.Set;

/**
 * @author Kuznetsovka created 14.07.2022
 */
@Entity(name = "courses")
public class Course {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToMany
  @JoinTable(
          name = "estimate_course",
          joinColumns = @JoinColumn(name = "course_id"),
          inverseJoinColumns = @JoinColumn(name = "estimate_id"))
  Set<Estimation> estimations;

  public Course() {
  }

  public Course(Set<Estimation> estimations) {
    this.estimations = estimations;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Set<Estimation> getEstimations() {
    return estimations;
  }

  public void setEstimations(Set<Estimation> estimations) {
    this.estimations = estimations;
  }
}
