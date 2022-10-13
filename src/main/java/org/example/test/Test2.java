package org.example.test;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Kuznetsovka created 12.10.2022
 */
public class Test2 {

  // https://russianblogs.com/article/74471007299/
  public static volatile  EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
  public static final EntityManager em = emf.createEntityManager();
  private static volatile Session session = em.unwrap(org.hibernate.Session.class);
  private static volatile SessionFactory sf = session.getSessionFactory();
  public static volatile Statistics statictics = sf.getStatistics();
  public static volatile Cache cache = emf.getCache();


  @BeforeAll
  public static void fillDB() {
    List<Student> students = new ArrayList<>();
    em.getTransaction().begin();
    Mentor mentor1 = new Mentor(1L, "Ментор старый", "Петя");
    Mentor mentor2 = new Mentor(2L, "Ментор старый", "Cо студентами");

    MentorReadOnly mentor3 = new MentorReadOnly(1L, "Ментор старый", "Артем",
        LocalDateTime.of(2000, 1, 1, 0, 0));

    MentorTransactional mentor4 = new MentorTransactional(1L, "Ментор старый", "Вася",
        LocalDateTime.of(2000, 1, 1, 0, 0));

    MentorNonstrict mentor5 = new MentorNonstrict(1L, "Ментор старый", "Олег",
        LocalDateTime.of(2000, 1, 1, 0, 0));

    MentorWithStudent mentor6 = new MentorWithStudent(1L, "Ментор1", "Петя");

    Student student1 = new Student(1L, "Студент1", "Кирилл");
    Student student2 = new Student(2L, "Студент2", "Сергей");
    Student student3 = new Student(3L, "Студент3", "Иван");

    em.persist(student1);
    em.persist(student2);
    em.persist(student3);

    students.add(student1);
    students.add(student2);

    mentor1.setStudents(students);
    mentor2.setStudents(Collections.singletonList(student3));
    mentor6.setStudents(students);

    em.persist(mentor1);
    em.persist(mentor2);
    em.persist(mentor3);
    em.persist(mentor4);
    em.persist(mentor5);
    em.persist(mentor6);
    em.flush();
    em.getTransaction().commit();
    clearCache(true, false);
  }

  /**
   * Проверка кэша 1-го уровня
   * Результат: 2 запроса.
   * Вывод: Работает кэш 1-го уровня, хотя запрос и был отправлен.
   * При получении из разных EM не работает не один из кэшей.
   */
  @Test
  public void testCacheFirstLevel_NotCacheable() {
    statictics.setStatisticsEnabled(true);
    Student student1 = em.createQuery("select e from students e where e.id=:id", Student.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(student1.getName());

    Student student2 = em.createQuery("select e from students e where e.id=:id", Student.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(student2.getName());
    assertTrue(em.contains(student1));
    assertEquals(student1, student2);
    assertEquals(2, statictics.getQueryExecutionCount());
    assertEquals(0, statictics.getSecondLevelCachePutCount());

    clearCache(true, false);

    EntityManager em1 = emf.createEntityManager();
    EntityManager em2 = emf.createEntityManager();

    Student student3 = em1.createQuery("select e from students e where e.id=:id", Student.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(student3.getName());

    Student student4 = em2.createQuery("select e from students e where e.id=:id", Student.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(student4.getName());
    assertTrue(em1.contains(student3));
    assertFalse(em2.contains(student3));
    assertNotEquals(student3, student4);

    assertFalse(cache.contains(Student.class, 1L));

    assertEquals(2, statictics.getQueryExecutionCount());
    assertEquals(0, statictics.getSecondLevelCachePutCount());

    clearCache(true, true);

  }

  /**
   * Проверка кэша 1-го уровня и 2-го уровня
   * Объект: Cacheable
   * Результат: 2 запроса.
   * Вывод: Работает кэш 1-го уровня, хотя запрос и был отправлен.
   * При получении из разных EM не работает не один из кэшей.
   */
  @Test
  public void testCacheFirstLevel_Cacheable() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = em.createQuery("select e from mentors e where e.id=:id", Mentor.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(mentor1.getName());

    Mentor mentor2 = em.createQuery("select e from mentors e where e.id=:id", Mentor.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(mentor2.getName());
    assertTrue(em.contains(mentor1));
    assertEquals(mentor1, mentor2);
    assertEquals(2, statictics.getQueryExecutionCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    assertEquals(0, statictics.getSecondLevelCacheHitCount());

    clearCache(true, false);

    EntityManager em1 = emf.createEntityManager();
    EntityManager em2 = emf.createEntityManager();

    mentor1 = em1.createQuery("select e from mentors e where e.id=:id", Mentor.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(mentor1.getName());
//    sleep(6000); //Если подождать 6 секунд, объект mentor1 пропадет из кэша
//    assertFalse(cache.contains(Mentor.class, 1L));
    assertTrue(cache.contains(Mentor.class, 1L));

    mentor2 = em2.createQuery("select e from mentors e where e.id=:id", Mentor.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(mentor2.getName());

    assertNotEquals(mentor1, mentor2); //Объекты взяты из базы, хоть и лежит в кэше 2-го уровня.

    assertTrue(cache.contains(Mentor.class, 1L));

    assertEquals(2, statictics.getQueryExecutionCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    assertEquals(0, statictics.getSecondLevelCacheHitCount()); // Объекты взяты из базы, хоть и лежит в кэше 2-го уровня.
    clearCache(true, true);

  }

  /**
   * Проверка кэша 2-го уровня
   * Результат: 1 запроса.
   * Вывод: Работает 2-ой кэш
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery2() {
    EntityManager em1 = em.getEntityManagerFactory().createEntityManager();
    EntityManager em2 = em.getEntityManagerFactory().createEntityManager();
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = em1.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());

    Mentor mentor2 = em2.find(Mentor.class, 1L);
    System.out.println(mentor2.getName());

    assertNotEquals(em1, em2);
    assertTrue(cache.contains(Mentor.class, 1L));
    assertNotEquals(mentor1, mentor2);
    assertEquals(1, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    assertEquals(1, statictics.getSecondLevelCacheHitCount());
    System.out.println("Получение общего количество кэшируемых сущностей/коллекций, успешно извлеченных из кэша: " + statictics.getSecondLevelCacheHitCount());

    em1.clear();
    em2.clear();
    clearCache(true, false);

    Mentor mentor3 = em1.createQuery("select e from mentors e where e.id=:id", Mentor.class)
        .setParameter("id", 1L)
        .getSingleResult();
    System.out.println(mentor3.getName());

    Mentor mentor4 = em2.find(Mentor.class, 1L);
    System.out.println(mentor4.getName());

    assertNotEquals(mentor3, mentor4);
    assertTrue(cache.contains(Mentor.class, 1L));
    assertEquals(1, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    assertEquals(1, statictics.getSecondLevelCacheHitCount());
    System.out.println("Получение общего количество кэшируемых сущностей/коллекций, успешно извлеченных из кэша: " + statictics.getSecondLevelCacheHitCount());
    clearCache(true, true);
  }

  /**
   * Проверка кэша транзакционного изменения
   * Стратегия: READ_ONLY
   * Вывод объект со стратегией ReadOnly не может быть изменен, но может быть удален
   */
  @Test
  public void testCacheSecond_CHANGE_READ_ONLY() {
    statictics.setStatisticsEnabled(true);
    try {
      em.getTransaction().begin();
      MentorReadOnly mentor1 = em.find(MentorReadOnly.class, 1L);
      System.out.println(mentor1.getName());

      assertEquals("Ментор старый", mentor1.getName());
      mentor1.setName("Ментор новый");
      em.flush();
      assertEquals("Ментор новый", mentor1.getName());

      em.getTransaction().commit();
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      System.out.println("Объект со стратегией ReadOnly не может быть изменен");
      em.getTransaction().rollback();
    }
    em.getTransaction().begin();
    MentorReadOnly mentor1 = em.find(MentorReadOnly.class, 1L);
    System.out.println(mentor1.getName());

    assertEquals("Ментор старый", mentor1.getName());
    em.remove(mentor1);
    em.flush();
    em.getTransaction().commit();
    clearCache(true, true);
  }


  /**
   * Проверка кэша в кросстранзакционности
   * Стратегия: READ_ONLY
   * Результат: 1 запрос, объект берется из кэша 2го уровня
   * Вывод: Даже используя разные EM в транзакциях кэш 2-го уровня так-же работает
   */
  @Test
  public void READ_ONLY_TwoTransactional() {
    statictics.setStatisticsEnabled(true);
    CyclicBarrier barrier = new CyclicBarrier(2);
    CountDownLatch cdl = new CountDownLatch(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    MyThread t1 = new MyThread.Builder()
        .mentorReadOnly(MentorReadOnly.class).barrier(barrier).cdl(cdl)
        .name("1-Thread")
        .methodName(MyThread.MethodName.DELETE)
        //.typeBarrier(MyThread.TypeBarrier.TIMEOUT_5)
        .time(0)
        .build();
    MyThread t2 = new MyThread.Builder()
        .mentorReadOnly(MentorReadOnly.class).barrier(barrier).cdl(cdl)
        .name("2-Thread")
        .methodName(MyThread.MethodName.DELETE)
        //.typeBarrier(MyThread.TypeBarrier.TIMEOUT_10)
        .time(1000)
        .build();
    System.out.println("Запуск потоков");
    executor.execute(t1);
    executor.execute(t2);

    try {
      cdl.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    executor.shutdown();
    System.out.println("Завершение потоков");
    clearCache(true, true);
  }

  /**
   * Проверка кэша в кросстранзакционности
   * Стратегия: NONSTRICT
   * Результат: Блокировка сессии при двойной замене. Блокировка на уровке БД.
   * Результат: 2 запроса, объект берется из базы
   * Вывод: Даже используя разные EM в транзакциях кэш 2-го уровня так-же работает
   */
  @Test
  public void NONSTRICT_TwoTransactional() {
    statictics.setStatisticsEnabled(true);
    CyclicBarrier barrier = new CyclicBarrier(2);
    CountDownLatch cdl = new CountDownLatch(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    MyThread t1 = new MyThread.Builder()
        .mentorNonstrict(MentorNonstrict.class).barrier(barrier).cdl(cdl)
        .name("1-Thread")
        .methodName(MyThread.MethodName.FIND)
        .time(0)
        .build();
    MyThread t2 = new MyThread.Builder()
        .mentorNonstrict(MentorNonstrict.class).barrier(barrier).cdl(cdl)
        .name("2-Thread")
        .methodName(MyThread.MethodName.FIND)
        .time(1000)
        .build();
    System.out.println("Запуск потоков");
    executor.execute(t1);
    executor.execute(t2);

    try {
      cdl.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    executor.shutdown();
    System.out.println("Завершение потоков");
    clearCache(true, true);
  }

  /**
   * Проверка кэша в кросстранзакционности
   * стратегия: WRITE_READ
   * Результат: Блокировка сессии при двойной замене. Блокировка на уровке БД.
   * Результат: 2 запроса, объект берется из базы
   * Вывод: Даже используя разные EM в транзакциях кэш 2-го уровня так-же работает
   */
  @Test
  public void WRITE_READ_TwoTransactional() {
    statictics.setStatisticsEnabled(true);
    CyclicBarrier barrier = new CyclicBarrier(2);
    CountDownLatch cdl = new CountDownLatch(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    MyThread t1 = new MyThread.Builder()
        .mentor(Mentor.class).barrier(barrier).cdl(cdl)
        .name("1-Thread")
        .methodName(MyThread.MethodName.FIND)
        .time(0)
        .build();
    MyThread t2 = new MyThread.Builder()
        .mentor(Mentor.class).barrier(barrier).cdl(cdl)
        .name("2-Thread")
        .methodName(MyThread.MethodName.FIND)
        .time(1000)
        .build();
    System.out.println("Запуск потоков");
    executor.execute(t1);
    executor.execute(t2);

    try {
      cdl.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    executor.shutdown();
    System.out.println("Завершение потоков");
    clearCache(true, true);
  }

  /**
   * Проверка кэша в кросстранзакционности
   * Стратегия: TRANSACTIONAL
   * Результат: 1 запрос, объект берется из кэша 2го уровня 2-ой поток берет из кэша, если добавлено свойство  <property name="current_session_context_class" value = "thread"/>
   * Вывод: Даже используя разные EM в транзакциях кэш 2-го уровня так-же работает
   */
  @Test
  public void TRANSACTIONAL_TwoTransactional() {
    statictics.setStatisticsEnabled(true);
    CyclicBarrier barrier = new CyclicBarrier(2);
    CountDownLatch cdl = new CountDownLatch(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    MyThread t1 = new MyThread.Builder()
        .mentorTransactional(MentorTransactional.class).barrier(barrier).cdl(cdl)
        .name("1-Thread")
        .methodName(MyThread.MethodName.CHANGE)
        .time(0)
        .build();
    MyThread t2 = new MyThread.Builder()
        .mentorTransactional(MentorTransactional.class).barrier(barrier).cdl(cdl)
        .name("2-Thread")
        .methodName(MyThread.MethodName.CHANGE)
        .time(1000)
        .build();
    System.out.println("Запуск потоков");
    executor.execute(t1);
    executor.execute(t2);

    try {
      cdl.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    executor.shutdown();
    System.out.println("Завершение потоков");
    clearCache(true, true);
  }

  /**
   * Проверка кэша 2-го уровня в транзакции при удалении записи
   * Стратегия: WRITE_READ
   * Результат: 2 запроса. До окончания транзакции сущность уже меняет состояние на null.
   * Вывод: Имеем 2 запроса, но данные возвращаются из cache? т.к. в момент запроса в базе объект еще не удален.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_Delete() {
    statictics.setStatisticsEnabled(true);
    em.getTransaction().begin();
    MentorReadOnly mentor1 = em.find(MentorReadOnly.class, 1L);
    System.out.println(mentor1.getName());
    assertEquals(0, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(0, statictics.getEntityDeleteCount());
    em.remove(mentor1);
    em.flush();
    assertEquals(2, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(1, statictics.getEntityDeleteCount());
    MentorReadOnly mentor2 = em.find(MentorReadOnly.class, 1L);
    System.out.println("Сущность = " + mentor2);
    assertNull(mentor2);
    assertEquals(4, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    em.getTransaction().commit();
    clearCache(true, true);
  }

  /**
   * Проверка кэша 2-го уровня в связной коллекцией
   * Результат: 2 запрос, при первом обращении кэшируемый объект попадает в КЭШ 2-го уровня.
   * Вывод: При использовании некэшируемой коллекции, коллекции в Кэше не обнаружено.
   */
  @Test
  public void testCacheSecondLevelForCollection() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = em.find(Mentor.class, 1L);
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    Integer count = mentor1.getStudents().size(); // первое обращение к коллекции студентов
    assertEquals(2, statictics.getSecondLevelCachePutCount());

    for (Student student : mentor1.getStudents()) {
      System.out.println("Имя студента: " + student.getName());
    }
    assertEquals(2, statictics.getSecondLevelCachePutCount());
    assertEquals(2, statictics.getPrepareStatementCount());

    clearCache(true, false);

    MentorWithStudent mentor2 = em.find(MentorWithStudent.class, 1L);
    Integer count2 = mentor2.getStudents().size(); // первое обращение к коллекции студентов

    for (Student student : mentor2.getStudents()) {
      System.out.println("Имя студента: " + student.getName());
    }
    //assertEquals(1, statictics.getSecondLevelCachePutCount());

    clearCache(true, true);
  }

  public static void sleep(int milliseconds) {
    try {
      if(milliseconds > 0)
        Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void clearCache(boolean isClearStatistic, boolean isCloseSession) {
    if (cache != null) {
      session.clear();
      emf.getCache().evictAll();
      cache.evictAll();
      sf.getCache().evictQueryRegions();
    }
    if (isClearStatistic)
      statictics.clear();
    if (isCloseSession) {
      session.close();
    }
    // em.evict(Mentor); Удаление из кэша 1-го уровня.
    // emFactory.evict(Mentor.class, mentorId); Удаление из кэша определенного объекта
    // emFactory.evict(Mentor.class); Удаление из кэша все объекты указанного класса
    // emFactory.evictCollection("Mentor.students", mentorId); удалить определенную коллекцию
    // emFactory.evictCollection("Mentor.students"); удалить все коллекции ментора
    // emFactory.evictQueries() очистка запросов из кэша.
    // Возможно более точечное обновление запросов: Query.setCacheMode(CacheMode.REFRESH) + setCacheRegion()
  }
}
