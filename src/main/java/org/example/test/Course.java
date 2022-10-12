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

  public Course() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

}
