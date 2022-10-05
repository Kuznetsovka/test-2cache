package org.example.test;


import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author Kuznetsovka created 14.07.2022
 */
@Entity(name = "students")
public class Student {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String name;

  public String surname;

  public LocalDateTime birthday;
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
          name = "course_student",
          joinColumns = @JoinColumn(name = "student_id"),
          inverseJoinColumns = @JoinColumn(name = "course_id"))
  Set<Course> courses;

  public Student() {
  }

  public Student(Long id, String name, String surname, LocalDateTime birthday, Set<Course> courses) {
    this.id = id;
    this.name = name;
    this.surname = surname;
    this.birthday = birthday;
    this.courses = courses;
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

  public Set<Course> getCourses() {
    return courses;
  }

  public void setCourses(Set<Course> courses) {
    this.courses = courses;
  }
}
