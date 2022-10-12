package org.example.test;

import javax.persistence.EntityManager;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static org.example.test.Test2.*;

class MyThread implements Runnable {

  enum MethodName{
    CHANGE,
    NONE,
    DELETE
  }
  private final CountDownLatch cdl;
  private final String name;
  private final MethodName isChange;
  private final int time;
  private final CyclicBarrier barrier;
  private final Object lock = new Object();
  private final Class<Mentor> mentor;
  private final EntityManager em = emf.createEntityManager();

  public MyThread(Class<Mentor> mentor, CyclicBarrier barrier,CountDownLatch cdl, String name, MethodName isChange, int time) {
    this.name = name;
    this.isChange = isChange;
    this.time = time;
    this.barrier = barrier;
    this.cdl = cdl;
    this.mentor = mentor;
    new Thread(this);
  }

  public void run() {
    sleep(time);
    System.out.println(name + " Начало транзакции");
    System.out.println(cache.toString());
    em.getTransaction().begin();

    MentorNameable mentor1 = em.find(mentor, 1L);
    System.out.println(name + " Старое имя объекта: " + mentor1.getName());

    method(mentor1);

    System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
    System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());

    MentorNameable mentor2 = em.find(mentor, 1L);
    System.out.println(name + " Новое имя объекта: " + mentor2.getName());
    System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
    System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());
    System.out.println(name + " Количество сущностей взятых из кэша: " + statictics.getSecondLevelCacheHitCount());
    try {
      barrier.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (BrokenBarrierException e) {
      throw new RuntimeException(e);
    }
    em.getTransaction().commit();
    System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
    System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());
    System.out.println(name + " Количество сущностей взятых из кэша: " + statictics.getSecondLevelCacheHitCount());
    System.out.println(name + " Конец транзакции");
    try {
      barrier.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (BrokenBarrierException e) {
      throw new RuntimeException(e);
    }
    cdl.countDown();
  }

  private void method(MentorNameable mentor) {
    switch (isChange){
      case CHANGE:
        mentor.setName("Ментор новый");
        em.flush();
        break;
      case NONE:
        break;
    }
  }
}