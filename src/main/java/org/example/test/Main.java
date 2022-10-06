package org.example.test;

import org.hibernate.Session;
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

  private static EntityManagerFactory emf = null;



  @BeforeAll
  public static void createCache(){
    // Для Encache 2
    // Добавить кеш с определенной конфигурацией
    //import net.sf.ehcache.Cache;
    //import net.sf.ehcache.CacheManager;
    //import net.sf.ehcache.config.CacheConfiguration;
    //import net.sf.ehcache.config.PersistenceConfiguration;
    //import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
//    CachingProvider provider = Caching.getCachingProvider();
//    CacheManager cacheManager = provider.getCacheManager();
//    MutableConfiguration<Long, String> configuration =
//        new MutableConfiguration<Long, String>()
//            .set
//            .setTypes(Long.class, String.class)
//            .setStoreByValue(false)
//            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_MINUTE));
//    Cache<Long, String> cache = cacheManager.createCache("jCache", configuration);
//
//
//    CacheManager manager1 = CacheManager.newInstance("ehcache.xml");
//    CacheManager manager = CacheManager.create();
//    Cache testCache = new Cache(
//        new CacheConfiguration("basicCache", 10)
//            .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
//            .eternal(false)
//            .timeToLiveSeconds(1)
//            .timeToIdleSeconds(1)
//            .diskExpiryThreadIntervalSeconds(0)
//            .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP)));
//    manager.addCache(testCache);
//    cache = testCache;
  }

  //@BeforeAll
  public static void fillDB() {
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

    clearCache();
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

    statictics.clear();
    clearCache();
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

    statictics.clear();
    clearCache();
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

    statictics.clear();
    clearCache();
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

    statictics.clear();
    clearCache();
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

    statictics.clear();
    clearCache();

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

    statictics.clear();
    clearCache();

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

    statictics.clear();
    clearCache();
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
    statictics.clear();
    clearCache();
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
    clearCache();           // Без очистки кэша объект не удалится

    System.out.println("Количество 'объектов' во 2-м кэше:" + statictics.getSecondLevelCachePutCount());

    Mentor mentor2 = session.find(Mentor.class, 1L);
    System.out.println(mentor2.getName());


    assertEquals(2, statictics.getPrepareStatementCount());
    assertEquals(2, statictics.getSecondLevelCachePutCount());
    // 1 запрос
    statictics.clear();
    clearCache();
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

  }

  private void sleep(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void naming(Student user, String name, String surname) {
    user.setName(name);
    user.setSurname(surname);
  }

  private static Session initSessionFactory() {
    emf = Persistence.createEntityManagerFactory("default");
    return emf.createEntityManager().unwrap(org.hibernate.Session.class);
  }

  private static void clearCache() {
    if (cache != null) {
      session.clear();
      cache.evictAll();
    }
  }
}