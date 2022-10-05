package org.example.test;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kuznetsovka created 11.07.2022
 */
public class Main {

  private static Session session = initSessionFactory();

  private static Statistics statictics = session.getSessionFactory().getStatistics();


  //@BeforeAll // Заполняем одини раз, потом запускаем тесты.
  public static void fillDB() {
    statictics.setStatisticsEnabled(true);
    session.getTransaction().begin();
    Set<Student> myUsers = new HashSet<>();
    Mentor mentor1 = new Mentor();
    Mentor mentor2 = new Mentor();
    mentor1.setName("Ментор1");
    mentor1.setSurname("Петя");

    Student student1 = new Student();
    Student student2 = new Student();
    Student student3 = new Student();
    naming(student1, "Студент1", "Кирилл");
    naming(student2, "Студент2", "Сергей");
    naming(student3, "Партнер", "Петр");

    session.persist(student1);
    session.persist(student2);
    session.persist(student3);

    myUsers.add(student1);
    myUsers.add(student2);

    mentor1.setStudents(myUsers);
    mentor2.setStudents(Collections.singleton(student3));
    session.persist(mentor1);
    session.persist(mentor2);

    session.flush();
    session.getTransaction().commit();
  }

  @Test
  public void testCacheFirstLevelHQLQuery() {
    statictics.setStatisticsEnabled(true);
    Mentor user = new Mentor();
    Student student1 = session.createQuery("select e from students e", Student.class)
        .getResultStream()
        .findFirst()
        .orElse(null);
    student1.getName();
    user.setStudents(Collections.singleton(student1));

    Student student2 = session.createQuery("select e from students e", Student.class)
        .getResultStream()
        .findFirst()
        .orElse(null);

    student2.getName();
    user.setStudents(Collections.singleton(student2));
    assertEquals(2, statictics.getQueryExecutionCount());
    statictics.clear();
    // При этом в логе запросов запрос всего 2.
  }

  @Test
  public void testCacheFirstLevelFindMethodQuery() throws SQLException {
    Mentor user = new Mentor();
    statictics.setStatisticsEnabled(true);
    Student students1 = session.find(Student.class, 1L);
    System.out.println(students1.getName());
    user.setStudents(Collections.singleton(students1));

    Student student2 = session.find(Student.class, 1L);

    user.setStudents(Collections.singleton(student2));
    assertEquals(0, statictics.getQueryExecutionCount());
    statictics.clear();
  }

  @Test
  public void testCacheSecondLevelHQLQuery() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = session.createQuery("select e from mentors e", Mentor.class)
        .getResultStream()
        .findFirst()
        .orElse(null);
    mentor1.getName();

    Mentor mentor2 = session.createQuery("select e from mentors e", Mentor.class)
        .getResultStream()
        .findFirst()
        .orElse(null);

    mentor2.getName();

    assertEquals(2, statictics.getQueryExecutionCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    statictics.clear();
    // 2 запроса. Кэш не отрабатывает
  }

  @Test
  public void testCacheSecondLevelFindMethodQuery() throws SQLException {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = session.find(Mentor.class, 1L);
    mentor1.getName();

    Mentor mentor2 = session.find(Mentor.class, 1L);
    mentor2.getName();

    assertEquals(1, statictics.getSecondLevelCachePutCount());
    // 1 запрос.
    statictics.clear();
  }

  private static Student naming(Student user, String name, String surname) {
    user.setName(name);
    user.setSurname(surname);
    return user;
  }

  private static Session initSessionFactory() {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
    Session session = emf.createEntityManager().unwrap(org.hibernate.Session.class);
    return session;
  }
}
