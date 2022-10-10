package org.example.test;

import org.hibernate.Session;
import org.hibernate.Transaction;
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
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * @author Kuznetsovka created 11.07.2022
 */
public class Main {

  // https://russianblogs.com/article/74471007299/
  public static final Session session = initSessionFactory();

  private static final Statistics statictics = session.getSessionFactory().getStatistics();

  private static final Cache cache = session.getSessionFactory().getCache();

  private static final Transaction tr = session.getTransaction();

  @BeforeAll
  public static void fillDB() {
    tr.begin();
    Set<Student> students = new HashSet<>();

    Mentor mentor1 = new Mentor(1L, "Ментор1", "Петя");

    Mentor mentor2 = new Mentor(2L, "Ментор2","Cо студентами");

    MentorReadOnly mentor3 = new MentorReadOnly(1L, "Ментор3","Артем",
        LocalDateTime.of(2000,1,1, 0,0));

    MentorTransactional mentor4 = new MentorTransactional(1L, "Ментор старый","Вася",
        LocalDateTime.of(2000,1,1, 0,0));

    MentorNonstrict mentor5 = new MentorNonstrict(1L, "Ментор старый","Олег",
        LocalDateTime.of(2000,1,1, 0,0));

    Student student1 = new Student(1L, "Студент1", "Кирилл");
    Student student2 = new Student(2L, "Студент2", "Сергей");
    Student student3 = new Student(3L, "Студент3", "Иван");



    session.merge(student1);
    session.merge(student2);
    session.merge(student3);

    students.add(student1);
    students.add(student2);

    mentor1.setStudents(students);
    mentor2.setStudents(Collections.singleton(student3));
    mentor5.setStudents(Collections.singleton(student1));
    session.merge(mentor1);
    session.merge(mentor2);
    session.merge(mentor3);
    session.merge(mentor4);
    session.merge(mentor5);
    session.flush();
    tr.commit();

    clearCache(true);
  }

  /**
   * Проверка кэша 1-го уровня
   * Результат: 2 запроса.
   * Вывод: Кэш не работате
   */
  @Test
  public void testCacheFirstLevelHQLQuery() {
    statictics.setStatisticsEnabled(true);

    Student student1 = session.createQuery("select e from students e", Student.class)
        .getResultStream()
        .findFirst()
        .orElse(new Student());

    System.out.println(student1.getName());

    Student student2 = session.createQuery("select e from students e", Student.class)
        .getResultStream()
        .findFirst()
        .orElse(new Student());

    System.out.println(student2.getName());

    assertEquals(2, statictics.getQueryExecutionCount());

    
    clearCache(true);
    // При этом в логе запросов запрос всего 2.
  }

  /**
   * Проверка кэша 1-го уровня
   * Результат: 2 запроса.
   * Вывод: Кэш не работате
   */
  @Test
  public void testCacheFirstLevelHQLQuerySingleResult() {
    statictics.setStatisticsEnabled(true);

    Student student1 = session.createQuery("select e from students e where e.id=:id", Student.class)
        .setParameter("id",1L)
        .getSingleResult();

    System.out.println(student1.getName());

    Student student2 = session.createQuery("select e from students e where e.id=:id", Student.class)
        .setParameter("id",1L)
        .getSingleResult();

    System.out.println(student2.getName());

    assertEquals(2, statictics.getQueryExecutionCount());

    
    clearCache(true);
    // При этом в логе запросов запрос всего 2.
  }

  /**
   * Проверка кэша 1-го уровня
   * Результат: 2 запроса.
   * Вывод: Кэш не работате
   */
  @Test
  public void testCacheFirstLevelNamedQuery() {
    statictics.setStatisticsEnabled(true);
    Mentor user = new Mentor();
    Student student1 = session.createNamedQuery("Student.getBySurname", Student.class)
        .setParameter("surname", "Сергей")
        .getSingleResult();

    System.out.println(student1.getName());

    Student student2 = session.createNamedQuery("Student.getBySurname", Student.class)
        .setParameter("surname", "Сергей")
        .getSingleResult();

    System.out.println(student2.getName());

    assertEquals(2, statictics.getQueryExecutionCount());

    
    clearCache(true);
    // 2 запроса, кэш не срабатывает
  }

  /**
   * Проверка кэша 1-го уровня
   * Результат: 1 запроса.
   * Вывод: кэш 1-го уровня работает
   */
  @Test
  public void testCacheFirstLevelFindMethodQuery() {
    statictics.setStatisticsEnabled(true);
    Mentor user = new Mentor();

    Student students1 = session.find(Student.class, 1L);
    System.out.println(students1.getName());
    user.setStudents(Collections.singleton(students1));

    Student student2 = session.find(Student.class, 1L);
    System.out.println(student2.getName());
    user.setStudents(Collections.singleton(student2));

    assertEquals(1, statictics.getPrepareStatementCount());

    
    clearCache(true);
    // 1 запрос
  }

  /**
   * Проверка кэша 3-го уровня
   * Результат: 1 запроса.
   * Вывод: В запросы вида getResultStream() ни один из кэшей не работает
   */
  @Test
  public void testCacheThirdLevelHQLQuery() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = session.createQuery("select e from mentors e", Mentor.class)
        .setHint( "org.hibernate.cacheable",true) // даже добавив это будет 2 запроса
        .getResultStream()
        .findFirst()
        .orElse(new Mentor());
    System.out.println(mentor1.getName());

    Mentor mentor2 = session.createQuery("select e from mentors e", Mentor.class)
        .setHint( "org.hibernate.cacheable",true) // даже добавив это будет 2 запроса
        .getResultStream()
        .findFirst()
        .orElse(new Mentor());
    System.out.println(mentor2.getName());

    assertEquals(2, statictics.getQueryExecutionCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    
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
    Mentor mentor1 = session.createQuery("select e from mentors e where e.id=:id", Mentor.class)
        .setHint( "org.hibernate.cacheable",true)
        .setParameter("id",1L)
        .getSingleResult();
    System.out.println(mentor1.getName());

    Mentor mentor2 = session.createQuery("select e from mentors e where e.id=:id", Mentor.class)
        .setHint( "org.hibernate.cacheable",true)
        .setParameter("id",1L)
        .getSingleResult();
    System.out.println(mentor2.getName());

    assertEquals(1, statictics.getQueryExecutionCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    
    clearCache(true);

    // 1 запроса. Только в этом случае отрабатывает Кэш 3-го уровня
    // Не срабатывыет при create-drop и инициализации таблицы
  }

  /**
   * Проверка кэша 2-го уровня
   * Результат: 1 запроса.
   * Вывод: В запросах NamedQuery работает 3-ий кэш, но не работает 2-й.
   * ВНИМАНИЕ: Не рабывать добавлять @QueryHint(name = "org.hibernate.cacheable", value = "true")
   */
  @Test
  public void testCacheThirdLevelNamedQuery() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = session.createNamedQuery("Mentor.getBySurname", Mentor.class)
        .setParameter("surname", "Петя")
        .getSingleResult();
    System.out.println(mentor1.getName());

    Mentor mentor2 = session.createNamedQuery("Mentor.getBySurname", Mentor.class)
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
    Mentor mentor1 = session.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());

    Mentor mentor2 = session.find(Mentor.class, 1L);
    System.out.println(mentor2.getName());

    assertEquals(1, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    // 1 запрос.
    
    clearCache(true);
  }

  /**
   * Проверка очистки кэша
   * Результат: 2 запроса
   * Вывод: Ручная очистка работает
   * ВНИМАНИЕ: требуется запускать как session.clear(), так и cache.evict()
   */
  @Test
  public void testCacheSecondLevelFindMethodQueryHandleEvict() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = session.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());

    System.out.println("Количество 'объектов' во 2-м кэше:" + statictics.getSecondLevelCachePutCount());
    session.evict(mentor1); // Удаление объекта из кеша сессии
    clearCache(false);           // Без очистки кэша объект не удалится

    System.out.println("Количество 'объектов' во 2-м кэше:" + statictics.getSecondLevelCachePutCount());

    Mentor mentor2 = session.find(Mentor.class, 1L);
    System.out.println(mentor2.getName());


    assertEquals(2, statictics.getPrepareStatementCount());
    assertEquals(2, statictics.getSecondLevelCachePutCount());
    // 1 запрос
    
    clearCache(true);
  }

  /**
   * Проверка очистки кэша по таймауту
   * Результат: Настроить общий кэш в ehcache.xml с таймаутом безрезультатно
   * Вывод: Есть возможность настроить динамический кэш
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
    assertEquals("one",cache2.get(1L));
    sleep(5000);
    assertNull(cache2.get(1L));
    cache2.put(2L, "two");
    assertEquals("two",cache2.get(2L));

    sleep(2000);

    assertEquals("two",cache2.get(2L));
    /*
    * session.setCacheMode();
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
    tr.begin();
    Mentor mentor1 = session.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());

    assertEquals("Ментор1", mentor1.getName());

    mentor1.setName("Ментор2");

    session.persist(mentor1);

    assertEquals("Ментор2", mentor1.getName());

    do {
      sleep(Integer.MAX_VALUE);
    } while(false);

    tr.commit();

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
    tr.begin();
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = session.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());
    assertEquals("Ментор1", mentor1.getName());
    tr.commit();
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
    tr.begin();
    Mentor mentor1 = session.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());

    Mentor mentor2 = session.find(Mentor.class, 1L);
    System.out.println(mentor2.getName());

    assertEquals(1, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    tr.commit();
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
    statictics.setStatisticsEnabled(true);
    tr.begin();

    System.out.println("**********  Начало 1 транзакции ********** ");
    Mentor mentor1 = session.find(Mentor.class, 1L);
    System.out.println("Имя ментора в базе: " + mentor1.getName());

    mentor1.setName("Ментор новый");
    session.persist(mentor1);
    System.out.println("Новое имя ментора: " + mentor1.getName());

    Mentor mentor2 = session.find(Mentor.class, 1L);

    System.out.println("Имя ментора полученное из КЭШа в рамках транзакции: " + mentor2.getName());

    assertEquals("Ментор новый", mentor2.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    tr.commit();
    // 1 запрос.
    
    clearCache(true);
    // Для сброса в изначальное состояние
    tr.begin();
    mentor1.setName("Ментор1");
    session.saveOrUpdate(mentor1);
    tr.commit();
  }

  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Стратегия: READ_ONLY
   * Результат: 1 запрос, второй запрос возвращает измененную сущность.
   *            Не позволяет изменять читаемый объект в одной транзакции
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_WithChange_READ_ONLY_Transactional() {
    statictics.setStatisticsEnabled(true);
    tr.begin();
    MentorReadOnly mentor1 = session.find(MentorReadOnly.class, 1L);
    System.out.println(mentor1.getName());

    mentor1.setName("Ментор2");
    session.persist(mentor1);

    MentorReadOnly mentor2 = session.find(MentorReadOnly.class, 1L);
    System.out.println(mentor2.getName());

    assertEquals("Ментор2", mentor2.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    tr.commit();
    // 1 запрос.
    
    clearCache(true);
  }


  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Начало
   * Стратегия: TRANSACTIONAL
   * Результат: 1 запрос, второй запрос возвращает измененную сущность. Коммит исполняется
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита.
   * Примечание: Встроенные поставщики кэша не поддерживают блокировку.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_WithChange_TRANSACTIONAL_Transactional_start() {
    statictics.setStatisticsEnabled(true);
    tr.begin();
    System.out.println("**********  Начало 1 транзакции ********** ");
    MentorTransactional mentor1 = session.find(MentorTransactional.class, 1L);
    System.out.println("Имя ментора в базе: " + mentor1.getName());

    mentor1.setName("Ментор новый");
    session.persist(mentor1);
    System.out.println("Новое имя ментора: " + mentor1.getName());

    MentorTransactional mentor2 = session.find(MentorTransactional.class, 1L);

    System.out.println("Имя ментора полученное из КЭШа в рамках транзакции: " + mentor2.getName());

    assertEquals("Ментор новый", mentor2.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    tr.commit();
    System.out.println("**********  Конец 1 транзакции ********** ");

    // Для сброса в изначальное состояние
    tr.begin();
    mentor1.setName("Ментор старый");
    session.saveOrUpdate(mentor1);
    tr.commit();
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
  public void testCacheSecondLevelFindMethodQuery_WithChange_TRANSACTIONAL_Transactional_finish() {
    statictics.setStatisticsEnabled(true);
    tr.begin();
    System.out.println("**********  Начало 2 транзакции ********** ");
    MentorTransactional mentor1 = session.find(MentorTransactional.class, 1L);
    System.out.println("Имя ментора в базе:" + mentor1.getName());
    assertEquals("Ментор новый", mentor1.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    tr.commit();
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
    tr.begin();
    System.out.println("**********  Начало 1 транзакции ********** ");
    MentorNonstrict mentor1 = session.find(MentorNonstrict.class, 1L);
    System.out.println("Имя ментора в базе: " + mentor1.getName());

    mentor1.setName("Ментор новый");
    session.persist(mentor1);
    System.out.println("Новое имя ментора: " + mentor1.getName());

    MentorNonstrict mentor2 = session.find(MentorNonstrict.class, 1L);

    System.out.println("Имя ментора полученное из базы: " + mentor2.getName());

    assertEquals("Ментор новый", mentor2.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    tr.commit();
    System.out.println("**********  Конец 1 транзакции ********** ");

    // Для сброса в изначальное состояние
    tr.begin();
    mentor1.setName("Ментор старый");
    session.saveOrUpdate(mentor1);
    tr.commit();
    clearCache(true);
  }

  /**
   * Проверка кэша 2-го уровня в транзакции с изменением
   * Конец
   * Стратегия: NONSTRICT
   * Результат: 1 запрос, второй запрос возвращает измененную сущность. Коммит исполняется
   * Вывод: В рамках одной транзакции кэш отрабатывает сразу, до коммита.
   */
  @Test
  public void testCacheSecondLevelFindMethodQuery_WithChange_NONSTRICT_Transactional_finish() {
    statictics.setStatisticsEnabled(true);
    tr.begin();
    System.out.println("**********  Начало 2 транзакции ********** ");
    MentorNonstrict mentor1 = session.find(MentorNonstrict.class, 1L);
    System.out.println("Имя ментора в базе:" + mentor1.getName());

    mentor1.setName("Ментор новый2");
    session.persist(mentor1);

    System.out.println("Новое имя ментора: " + mentor1.getName());

    MentorNonstrict mentor2 = session.find(MentorNonstrict.class, 1L);

    assertEquals("Ментор новый2", mentor1.getName());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    tr.commit();
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
    tr.begin();
    Mentor mentor1 = session.find(Mentor.class, 1L);
    System.out.println(mentor1.getName());
    assertEquals(0, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(0, statictics.getEntityDeleteCount());
    session.remove(mentor1);
    session.flush();
    assertEquals(2, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(1, statictics.getEntityDeleteCount());
    Mentor mentor2 = session.find(Mentor.class, 1L);
    System.out.println("Сущность = " + mentor2);
    assertNull(mentor2);
    assertEquals(4, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    tr.commit();
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
    tr.begin();
    MentorNonstrict mentor1 = session.find(MentorNonstrict.class, 1L);
    System.out.println(mentor1.getName());
    assertEquals(0, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(0, statictics.getEntityDeleteCount());
    session.remove(mentor1);
    session.flush();
    assertEquals(1, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(1, statictics.getEntityDeleteCount());
    MentorNonstrict mentor2 = session.find(MentorNonstrict.class, 1L);
    System.out.println("Сущность = " + mentor2);
    assertNull(mentor2);
    assertEquals(3, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    tr.commit();
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
    tr.begin();
    MentorNonstrict mentor1 = session.find(MentorNonstrict.class, 1L);
    System.out.println(mentor1.getName());
    assertEquals(0, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(0, statictics.getEntityDeleteCount());
    session.remove(mentor1);
    session.flush();
    assertEquals(2, statictics.getUpdateTimestampsCachePutCount());
    assertEquals(1, statictics.getEntityDeleteCount());
    MentorNonstrict mentor2 = session.find(MentorNonstrict.class, 1L);
    System.out.println("Сущность = " + mentor2);
    assertNull(mentor2);
    assertEquals(4, statictics.getPrepareStatementCount());
    assertEquals(1, statictics.getSecondLevelCachePutCount());

    clearCache(true);
  }

  /**
   * Проверка кэша 2-го уровня в связной коллекцией
   * Результат: 2 запрос, при первом обращении кэшируемый объект попадает в КЭШ 2-го уровня.
   * Вывод: При использовании некэшируемой коллекции, доп. запросов так-же нет, но и 2-й КЭШ он не попадает.
   * Следовательно объект попадает в первый КЭШ.
   */
  @Test
  public void testCacheSecondLevelForCollection() {
    statictics.setStatisticsEnabled(true);
    Mentor mentor1 = session.find(Mentor.class, 1L);
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    Integer count = mentor1.getStudents().size(); // первое обращение к коллекции студентов
    assertEquals(2, statictics.getSecondLevelCachePutCount());

    for (Student student : mentor1.getStudents()) {
      System.out.println("Имя студента: " + student.getName());
    }
    assertEquals(2, statictics.getSecondLevelCachePutCount());
    assertEquals(2, statictics.getPrepareStatementCount());

    clearCache(true);

    MentorNonstrict mentor2 = session.find(MentorNonstrict.class, 1L);

    int count2 = mentor2.getStudents().size(); // первое обращение к коллекции студентов

    assertEquals(1, statictics.getSecondLevelCachePutCount());

    for (Student student : mentor1.getStudents()) {
      System.out.println("Имя студента: " + student.getName());
    }
    assertEquals(1, statictics.getSecondLevelCachePutCount());
    assertEquals(2, statictics.getPrepareStatementCount());

    clearCache(true);
  }

  private void sleep(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static Session initSessionFactory() {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
    return emf.createEntityManager().unwrap(org.hibernate.Session.class);
  }

  private static void clearCache(boolean isClearStatistic) {
    if (cache != null) {
      session.clear();
      cache.evictAll();
    }
    if(isClearStatistic)
      statictics.clear();
    // session.evict(Mentor); Удаление из кэша 1-го уровня.
    // sessionFactory.evict(Mentor.class, mentorId); Удаление из кэша определенного объекта
    // sessionFactory.evict(Mentor.class); Удаление из кэша вссе объекты указанного класса
    // sessionFactory.evictCollection("Mentor.students", mentorId); удалить определенную коллекцию
    // sessionFactory.evictCollection("Mentor.students"); удалить все коллекции ментора
    // sessionFactory.evictQueries() очистка запросов из кэша.
    // Возможно более точесная обновление запросов: Query.setCacheMode(CacheMode.REFRESH) + setCacheRegion()
  }
}
