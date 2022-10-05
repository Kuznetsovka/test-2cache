package org.example.test;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author Kuznetsovka created 14.07.2022
 */

@Entity(name = "mentors")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Mentor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String name;

  public String surname;

  public LocalDateTime birthday;
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
          name = "student_mentor",
          joinColumns = @JoinColumn(name = "mentor_id"),
          inverseJoinColumns = @JoinColumn(name = "student_id"))
  public Set<Student> students;

  public Mentor() {
  }

  public Mentor(Long id, String name, String surname, LocalDateTime birthday, Set<Student> students) {
    this.id = id;
    this.name = name;
    this.surname = surname;
    this.birthday = birthday;
    this.students = students;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public LocalDateTime getBirthday() {
    return birthday;
  }

  public void setBirthday(LocalDateTime birthday) {
    this.birthday = birthday;
  }

  public Set<Student> getStudents() {
    return students;
  }

  public void setStudents(Set<Student> students) {
    this.students = students;
  }
}
