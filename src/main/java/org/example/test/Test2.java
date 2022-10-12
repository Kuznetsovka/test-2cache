package org.example.test;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Kuznetsovka created 12.10.2022
 */
public class Test2 {

  // https://russianblogs.com/article/74471007299/
  public static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
  public static final EntityManager em = emf.createEntityManager();
  private static final Session session = em.unwrap(org.hibernate.Session.class);
  private static final SessionFactory sf = session.getSessionFactory();
  private static final Statistics statictics = sf.getStatistics();
  private static final Cache cache = emf.getCache();


  @BeforeAll
  public static void fillDB() {
    List<Student> students = new ArrayList<>();
    em.getTransaction().begin();
    Mentor mentor1 = new Mentor(1L, "Ментор1", "Петя");
    Mentor mentor2 = new Mentor(2L, "Ментор2", "Cо студентами");

    MentorReadOnly mentor3 = new MentorReadOnly(1L, "Ментор3", "Артем",
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
    clearCache(true);
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

    clearCache(true);

    EntityManager em1 = emf.createEntityManager();
    EntityManager em2 = emf.createEntityManager();

    student1 = em1.createQuery("select e from students e where e.id=:id", Student.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(student1.getName());

    student2 = em2.createQuery("select e from students e where e.id=:id", Student.class)
        .setParameter("id", 1L)
        .getSingleResult();

    System.out.println(student2.getName());
    assertTrue(em1.contains(student1));
    assertFalse(em2.contains(student1));
    assertNotEquals(student1, student2);

    assertFalse(cache.contains(Student.class, 1L));

    assertEquals(2, statictics.getQueryExecutionCount());
    assertEquals(0, statictics.getSecondLevelCachePutCount());

    clearCache(true);

  }

  /**
   * Проверка кэша 1-го уровня и 2-го уровня
   * Объект: Cacheable
   * Результат: 2 запроса.
   * Вывод: Работает кэш 1-го уровня, хотя запрос и был отправлен.
   * При получении из разных EM не работает не один из кэшей.
   * ??? Откуда берется объект ???
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

    clearCache(true);

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

    assertNotEquals(mentor1, mentor2);

    assertTrue(cache.contains(Mentor.class, 1L));

    assertEquals(2, statictics.getQueryExecutionCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    clearCache(true);

  }

  /**
   * Проверка кэша 3-го уровня
   * Результат: 2 запроса.
   * Вывод: Запросы кладутся в кэш 3-го уровня, хотя идут 2 запроса.
   * При запросе из разных EM, идет 1 запрос. Объект получен из 3-го кэша, так как если закоментировать setHint, то будет 2 запроса.
   * Примечание 1 часть не влияет на 2, можно закоментировать
   */
  @Test
  public void testCacheThirdLevelHQLQuery() {
    statictics.setStatisticsEnabled(true);
    List<Mentor> mentor1 = em.createQuery("select e from mentors e", Mentor.class)
        .setHint("org.hibernate.cacheable", true)
        .getResultList();
    System.out.println(mentor1.get(0).getName());

    List<Mentor> mentor2 = em.createQuery("select e from mentors e", Mentor.class)
        .setHint("org.hibernate.cacheable", true)
        .getResultList();
    System.out.println(mentor2.get(0).getName());

    assertTrue(cache.contains(Mentor.class,1L));
    assertEquals(mentor1, mentor2);

    assertEquals(2, statictics.getQueryCacheMissCount());
    assertEquals(0, statictics.getQueryCacheHitCount());
    assertEquals(2, statictics.getQueryCachePutCount());
    assertEquals(2, statictics.getQueryExecutionCount());
    assertEquals(mentor1.size(), statictics.getSecondLevelCachePutCount());

    clearCache(true);

    EntityManager em1 = emf.createEntityManager();
    EntityManager em2 = emf.createEntityManager();

    List<Mentor> mentor3 = em1.createQuery("select e from mentors e", Mentor.class)
        .setHint("org.hibernate.cacheable", true)
        .getResultList();
    System.out.println(mentor3.get(0).getName());

    List<Mentor> mentor4 = em2.createQuery("select e from mentors e", Mentor.class)
        .setHint("org.hibernate.cacheable", true)
        .getResultList();
    System.out.println(mentor3.get(0).getName());

    assertTrue(cache.contains(Mentor.class,1L));
    assertNotEquals(mentor3, mentor4);
    assertEquals(1, statictics.getQueryCacheMissCount());
    assertEquals(1, statictics.getQueryCacheHitCount());
    assertEquals(1, statictics.getQueryCachePutCount());
    assertEquals(1, statictics.getQueryExecutionCount());
    assertEquals(2, statictics.getSecondLevelCacheHitCount());
    assertEquals(mentor3.size(), statictics.getSecondLevelCachePutCount());

    clearCache(true);

    // 2 запроса. Кэш не отрабатывает
  }

  /**
   * Проверка кэша 3-го уровня
   * Результат: 1 запроса.
   * Вывод: В запросы вида getSingleResult() работает 3-ий кэш
   */
  @Test
  public void testCacheThirdLevelHQLQuerySingleResult() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = em.createQuery("select e from mentors e where e.id=:id", Mentor.class)
        .setHint("org.hibernate.cacheable", true)
        .setParameter("id", 1L)
        .getSingleResult();
    System.out.println(mentor1.getName());

    Mentor mentor2 = em.createQuery("select e from mentors e where e.id=:id", Mentor.class)
        .setHint("org.hibernate.cacheable", true)
        .setParameter("id", 1L)
        .getSingleResult();
    System.out.println(mentor2.getName());

    assertEquals(1, statictics.getQueryExecutionCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());


    clearCache(true);

    // 1 запроса. Только в этом случае отрабатывает Кэш 3-го уровня
  }

  /**
   * Проверка кэша 2-го уровня
   * Результат: 1 запроса.
   * Вывод: В запросах NamedQuery работает 3-ий кэш, но не работает 2-й.
   * ВНИМАНИЕ: Не забывать добавлять @QueryHint(name = "org.hibernate.cacheable", value = "true")
   */
  @Test
  public void testCacheThirdLevelNamedQuery() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = em.createNamedQuery("Mentor.getBySurname", Mentor.class)
        .setParameter("surname", "Петя")
        .getSingleResult();
    System.out.println(mentor1.getName());

    Mentor mentor2 = em.createNamedQuery("Mentor.getBySurname", Mentor.class)
        .setParameter("surname", "Петя")
        .getSingleResult();

    System.out.println(mentor2.getName());
    assertEquals(1, statictics.getQueryExecutionCount());


    clearCache(true);
    // При добавлении 3-го уровня Кэша, Кэш запросов, запрос будет 1, Обязательно указывать Hint в запросе
  }

  /**
   * Проверка кэша 2-го уровня
   * Результат: 1 запроса.
   * Вывод: Работает 2-ой кэш
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = em.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());

    Mentor mentor2 = em.find(Mentor.class, 1L);
    System.out.println(mentor2.getName());

    assertEquals(1, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    // 1 запрос.

    clearCache(true);
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
    assertEquals(1, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    // 1 запрос.

    clearCache(true);
  }

  /**
   * Проверка очистки кэша
   * Результат: 2 запроса
   * Вывод: Ручная очистка работает
   * ВНИМАНИЕ: требуется запускать как em.clear(), так и cache.evict()
   */
  @Test
  public void testCacheSecondLevelFindMethodQueryHandleEvict() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = em.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());

    System.out.println("Количество 'объектов' во 2-м кэше:" + statictics.getSecondLevelCachePutCount());
    session.evict(mentor1); // Удаление объекта из кеша сессии
    clearCache(false);           // Без очистки кэша объект не удалится

    System.out.println("Количество 'объектов' во 2-м кэше:" + statictics.getSecondLevelCachePutCount());

    Mentor mentor2 = em.find(Mentor.class, 1L);
    System.out.println(mentor2.getName());


    assertEquals(2, statictics.getPrepareStatementCount());
    assertEquals(2, statictics.getSecondLevelCachePutCount());
    // 1 запрос

    clearCache(true);
  }

  /**
   * Проверка очистки кэша по таймауту
   * Результат: Настроить общий кэш в ehcache.xml с таймаутом безрезультатно
   * Вывод: Есть возможность настроить динамический кэш https://www.ehcache.org/documentation/3.0/107.html#supplement-jsr-107-configurations
   */
  @Test
  public void testCacheSecondLevelFindMethodQueryHandleTimeout() {
    CachingProvider provider = Caching.getCachingProvider();
    CacheManager cacheManager = provider.getCacheManager();
    MutableConfiguration<Long, String> configuration =
        new MutableConfiguration<Long, String>()
            .setTypes(Long.class, String.class)
            .setStatisticsEnabled(true)
            .setStoreByValue(false)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 4)));
    javax.cache.Cache<Long, String> cache2 = cacheManager.createCache("jCache", configuration);
    cache2.put(1L, "one");
    assertEquals("one", cache2.get(1L));
    sleep(5000);
    assertNull(cache2.get(1L));
    cache2.put(2L, "two");
    assertEquals("two", cache2.get(2L));

    sleep(2000);

    assertEquals("two", cache2.get(2L));
    /*
     * em.setCacheMode();
     * CacheMode.NORMAL: будет читать элементы из кэша второго уровня и записывать их в него.
     * CacheMode.GET: будет читать элементы из кеша второго уровня.
     *   Не производить запись в кеш второго уровня, кроме как при обновлении данных
     * */
  }


  /**
   * Проверка кэша транзакционного изменения
   * Начало
   * Стратегия: READ_ONLY
   */
  @Test
  public void testCacheSecondTwoTransactional_start() {
    statictics.setStatisticsEnabled(true);
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    Mentor mentor1 = em.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());

    assertEquals("Ментор1", mentor1.getName());

    mentor1.setName("Ментор2");

    em.merge(mentor1);
    em.flush();
    assertEquals("Ментор2", mentor1.getName());

    do {
      sleep(Integer.MAX_VALUE);
    } while (false);

    em.getTransaction().commit();

    // 1 запрос.
//
//    clearCache(true);
  }

  /**
   * Проверка кэша транзакционного изменения
   * Конец
   * Результат: возврат объекта до изменения
   * Вывод: В кэш не записывается значение до конца транзакции
   */
  @Test
  public void testCacheSecondTwoTransactional_finish() {
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = em.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());
    assertEquals("Ментор1", mentor1.getName());
    em.getTransaction().commit();
    assertEquals(1, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    // 1 запрос.

    clearCache(true);
  }

  /**
   * Проверка кэша 2-го уровня в транзакции
   * Результат: 1 запроса.
   * Вывод: Работает 2-ой кэш в рамках одной транзакции
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_Transactional() {
    statictics.setStatisticsEnabled(true);
    EntityManager em1 = em.getEntityManagerFactory().createEntityManager();
    EntityManager em2 = em.getEntityManagerFactory().createEntityManager();
    em1.getTransaction().begin();
    Mentor mentor1 = em1.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());

    Mentor mentor2 = em2.find(Mentor.class, 1L);
    System.out.println(mentor2.getName());

    assertEquals(1, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    em1.getTransaction().commit();
    // 1 запрос.

    clearCache(true);
  }

  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Стратегия: READ_AND_WRITE
   * Результат: 1 запрос, второй запрос возвращает измененную сущность. Коммит исполняется
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита.
   * Примечание: Встроенные поставщики кэша не поддерживают блокировку.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_WithChange_READ_AND_WRITE_Transactional() {
    EntityManager em = emf.createEntityManager();
    statictics.setStatisticsEnabled(true);
    em.getTransaction().begin();

    System.out.println("**********  Начало 1 транзакции ********** ");
    Mentor mentor1 = em.find(Mentor.class, 1L);
    System.out.println("Имя ментора в базе: " + mentor1.getName());

    mentor1.setName("Ментор новый");
    em.merge(mentor1);
    em.flush();
    System.out.println("Новое имя ментора: " + mentor1.getName());

    Mentor mentor2 = em.find(Mentor.class, 1L);

    System.out.println("Имя ментора полученное из КЭШа в рамках транзакции: " + mentor2.getName());

    assertEquals("Ментор новый", mentor2.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    em.getTransaction().commit();
    // 1 запрос.

    clearCache(true);
    // Для сброса в изначальное состояние
    em.getTransaction().begin();
    mentor1.setName("Ментор1");
    em.merge(mentor1);
    em.flush();
    em.getTransaction().commit();
  }

  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Стратегия: READ_ONLY
   * Результат: 1 запрос, второй запрос возвращает измененную сущность.
   * Не позволяет изменять читаемый объект в одной транзакции
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_WithChange_READ_ONLY_Transactional() {
    statictics.setStatisticsEnabled(true);
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    MentorReadOnly mentor1 = em.find(MentorReadOnly.class, 1L);
    System.out.println(mentor1.getName());

    mentor1.setName("Ментор2");
    em.persist(mentor1);
    em.flush();
    MentorReadOnly mentor2 = em.find(MentorReadOnly.class, 1L);
    System.out.println(mentor2.getName());

    assertEquals("Ментор2", mentor2.getName());
    assertEquals(2, statictics.getSecondLevelCachePutCount());
    em.getTransaction().commit();
    // 1 запрос.

    clearCache(true);
  }

  /**
   * Не забыть выключить BeforeAll
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Начало
   * Стратегия: TRANSACTIONAL
   * Результат: 1 запрос, второй запрос возвращает измененную сущность. Коммит исполняется
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита.
   * Примечание: Встроенные поставщики кэша не поддерживают блокировку.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_EM_WithChange_TRANSACTIONAL_Transactional_start() {
    statictics.setStatisticsEnabled(true);
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    System.out.println("**********  Начало 1 транзакции ********** ");
    MentorTransactional mentor1 = em.find(MentorTransactional.class, 1L);
    System.out.println("Имя ментора в базе: " + mentor1.getName());
    mentor1.setName("Ментор новый");
    em.merge(mentor1);
    em.flush();
    System.out.println("Новое имя ментора: " + mentor1.getName());

    MentorTransactional mentor2 = em.find(MentorTransactional.class, 1L);

    System.out.println("Имя ментора полученное из КЭШа в рамках транзакции: " + mentor2.getName());

    assertEquals("Ментор новый", mentor2.getName());
    assertEquals(2, statictics.getSecondLevelCachePutCount());

    em.getTransaction().commit();
    System.out.println("**********  Конец 1 транзакции ********** ");

    // Для сброса в изначальное состояние
    em.getTransaction().begin();
    mentor1.setName("Ментор старый");
    em.flush();
    em.getTransaction().commit();
    clearCache(true);
  }

  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Конец
   * Стратегия: TRANSACTIONAL
   * Результат: 1 запрос, второй запрос возвращает измененную сущность. Коммит исполняется
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита.
   * Примечание: Встроенные поставщики кэша не поддерживают блокировку. Поэтому разницы с WRITE_AND_READ нет.
   * В версии Hibernate 5.0 трназакционный уровень стал доступен только в JTA-окружении.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_EM_WithChange_TRANSACTIONAL_Transactional_finish() {
    statictics.setStatisticsEnabled(true);
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    System.out.println("**********  Начало 2 транзакции ********** ");
    MentorTransactional mentor1 = em.find(MentorTransactional.class, 1L);
    System.out.println("Имя ментора в базе:" + mentor1.getName());
    assertEquals("Ментор новый", mentor1.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    em.getTransaction().commit();
    System.out.println("**********  Конец 2 транзакции ********** ");
    clearCache(true);
    // 1 запрос.
  }

  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Начало
   * Стратегия: NONSTRICT
   * Результат: 1 запрос, второй запрос возвращает измененную сущность. Коммит исполняется
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_WithChange_NONSTRICT_Transactional() {
    statictics.setStatisticsEnabled(true);
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    System.out.println("**********  Начало 1 транзакции ********** ");
    MentorNonstrict mentor1 = em.find(MentorNonstrict.class, 1L);
    System.out.println("Имя ментора в базе: " + mentor1.getName());

    mentor1.setName("Ментор новый");
    em.merge(mentor1);
    em.flush();
    System.out.println("Новое имя ментора: " + mentor1.getName());

    MentorNonstrict mentor2 = em.find(MentorNonstrict.class, 1L);

    System.out.println("Имя ментора полученное из базы: " + mentor2.getName());

    assertEquals("Ментор новый", mentor2.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    em.getTransaction().commit();
    System.out.println("**********  Конец 1 транзакции ********** ");

    // Для сброса в изначальное состояние
    em.getTransaction().begin();
    mentor1.setName("Ментор старый");
    em.flush();
    em.getTransaction().commit();
    clearCache(true);
  }

  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Конец
   * Стратегия: NONSTRICT
   * Результат: 1 запрос: вернет Ментор новый2, 2 из кэша Ментор новый2
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_WithChange_NONSTRICT_Transactional_finish() {
    statictics.setStatisticsEnabled(true);
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    System.out.println("**********  Начало 2 транзакции ********** ");
    MentorNonstrict mentor1 = em.find(MentorNonstrict.class, 1L);
    System.out.println("Имя ментора в базе:" + mentor1.getName());

    mentor1.setName("Ментор новый2");
    em.merge(mentor1);
    em.flush();

    System.out.println("Новое имя ментора: " + mentor1.getName());
    assertEquals("Ментор новый2", mentor1.getName());

    MentorNonstrict mentor2 = em.find(MentorNonstrict.class, 1L);

    assertEquals("Ментор новый2", mentor2.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    em.getTransaction().commit();
    System.out.println("**********  Конец 2 транзакции ********** ");
    clearCache(true);
    // 1 запрос.
  }

  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Начало
   * Стратегия: WRITE_READ
   * Результат: 1 запрос, второй запрос возвращает измененную сущность. Коммит исполняется
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_WithChange_WRITE_READ_Transactional_start() {
    statictics.setStatisticsEnabled(true);
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    System.out.println("**********  Начало 1 транзакции ********** ");
    Mentor mentor1 = em.find(Mentor.class, 1L);
    System.out.println("Имя ментора в базе: " + mentor1.getName());

    mentor1.setName("Ментор новый");
    em.merge(mentor1);
    em.flush();
    System.out.println("Новое имя ментора: " + mentor1.getName());

    Mentor mentor2 = em.find(Mentor.class, 1L);

    System.out.println("Имя ментора полученное из базы: " + mentor2.getName());

    assertEquals("Ментор новый", mentor2.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    em.getTransaction().commit();
    System.out.println("**********  Конец 1 транзакции ********** ");

    // Для сброса в изначальное состояние
    em.getTransaction().begin();
    mentor1.setName("Ментор старый");
    em.merge(mentor1);
    em.getTransaction().commit();
    clearCache(true);
  }

  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Конец
   * Стратегия: WRITE_READ
   * Результат: 1 запрос: вернет Ментор новый, 2 из кэша Ментор новый2
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_WithChange_WRITE_READ_Transactional_finish() {
    statictics.setStatisticsEnabled(true);
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    System.out.println("**********  Начало 2 транзакции ********** ");
    Mentor mentor1 = em.find(Mentor.class, 1L);
    System.out.println("Имя ментора в базе:" + mentor1.getName());

    mentor1.setName("Ментор новый2");
    em.merge(mentor1);
    em.flush();

    assertEquals("Ментор новый2", mentor1.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    em.getTransaction().commit();
    System.out.println("**********  Конец 2 транзакции ********** ");
    clearCache(true);
    // 1 запрос.
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
    Mentor mentor1 = em.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());
    assertEquals(0, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(0, statictics.getEntityDeleteCount());
    em.remove(mentor1);
    em.flush();
    assertEquals(2, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(1, statictics.getEntityDeleteCount());
    Mentor mentor2 = em.find(Mentor.class, 1L);
    System.out.println("Сущность = " + mentor2);
    assertNull(mentor2);
    assertEquals(4, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    em.getTransaction().commit();
    clearCache(true);
  }


  /**
   * Проверка кэша 2-го уровня в транзакции при удалении записи
   * Стратегия: NONSTRICT_READ_WRITE
   * Результат: 2 запроса. До окончания транзакции сущность уже меняет состояние на null.
   * Вывод: Имеем 2 запроса, но данные возвращаются из cache т.к. в момент запроса в базе объект еще не удален.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_Delete_2() {
    statictics.setStatisticsEnabled(true);
    em.getTransaction().begin();
    MentorNonstrict mentor1 = em.find(MentorNonstrict.class, 1L);
    System.out.println(mentor1.getName());
    assertEquals(0, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(0, statictics.getEntityDeleteCount());

    assertTrue(cache.contains(MentorNonstrict.class, 1L));
    em.remove(mentor1);
    assertTrue(cache.contains(MentorNonstrict.class, 1L));
    em.flush();
    assertFalse(cache.contains(MentorNonstrict.class, 1L));
    assertEquals(1, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(1, statictics.getEntityDeleteCount());
    MentorNonstrict mentor2 = em.find(MentorNonstrict.class, 1L);

    System.out.println("Сущность = " + mentor2);
    assertNull(mentor2);
    assertEquals(3, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    em.getTransaction().commit();
    clearCache(true);
  }

  /**
   * Проверка кэша 2-го уровня в транзакции при удалении записи
   * Стратегия: READ_ONLY
   * Результат: 2 запроса. До окончания транзакции сущность уже меняет состояние на null.
   * Вывод: Имеем 2 запроса, но данные возвращаются из cache т.к. в момент запроса в базе объект еще не удален.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_Delete_3() {
    statictics.setStatisticsEnabled(true);
    em.getTransaction().begin();
    MentorNonstrict mentor1 = em.find(MentorNonstrict.class, 1L);
    System.out.println(mentor1.getName());
    assertEquals(0, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(0, statictics.getEntityDeleteCount());
    em.remove(mentor1);
    em.flush();
    assertEquals(2, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(1, statictics.getEntityDeleteCount());
    MentorNonstrict mentor2 = em.find(MentorNonstrict.class, 1L);

    System.out.println("Сущность = " + mentor2);
    assertTrue(cache.contains(MentorNonstrict.class, mentor2));
    assertNull(mentor2);
    assertEquals(4, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    clearCache(true);
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

    clearCache(true);

    MentorWithStudent mentor2 = em.find(MentorWithStudent.class, 1L);
    Integer count2 = mentor2.getStudents().size(); // первое обращение к коллекции студентов

    for (Student student : mentor2.getStudents()) {
      System.out.println("Имя студента: " + student.getName());
    }
    //assertEquals(1, statictics.getSecondLevelCachePutCount());

    clearCache(true);
  }

  private void sleep(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void clearCache(boolean isClearStatistic) {
    if (cache != null) {
      session.clear();
      cache.evictAll();
      sf.getCache().evictQueryRegions();
    }
    if (isClearStatistic)
      statictics.clear();
    // em.evict(Mentor); Удаление из кэша 1-го уровня.
    // emFactory.evict(Mentor.class, mentorId); Удаление из кэша определенного объекта
    // emFactory.evict(Mentor.class); Удаление из кэша все объекты указанного класса
    // emFactory.evictCollection("Mentor.students", mentorId); удалить определенную коллекцию
    // emFactory.evictCollection("Mentor.students"); удалить все коллекции ментора
    // emFactory.evictQueries() очистка запросов из кэша.
    // Возможно более точечное обновление запросов: Query.setCacheMode(CacheMode.REFRESH) + setCacheRegion()
  }
}
