package org.example.test;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author Kuznetsovka created 14.07.2022
 */

@Entity(name = "mentors_with_students")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class MentorWithStudent {

  @Id
  public Long id;

  public String name;

  public String surname;

  public LocalDateTime birthday;

  @OneToMany(fetch = FetchType.LAZY)
  public List<Student> students;

  public MentorWithStudent() {
  }

  public MentorWithStudent(Long id, String name, String surname) {
    this.id = id;
    this.name = name;
    this.surname = surname;
  }

  public MentorWithStudent(Long id, String name, String surname, LocalDateTime birthday, List<Student> students) {
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

  public List<Student> getStudents() {
    return students;
  }

  public void setStudents(List<Student> students) {
    this.students = students;
  }

}
