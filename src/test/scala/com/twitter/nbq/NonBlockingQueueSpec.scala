package com.twitter.nbq

import java.util.concurrent.{Callable, Executors, atomic}

import org.specs.Specification
import NonBlockingQueueSpec._

class NonBlockingQueueSpec extends Specification {
  "NonBlockingQueue" should {
    "enqueue and dequeue" in {
      val nbq = NonBlockingQueue[Int](2)
      for (i <- 0 until 10) {
        nbq.enqueue(i)
        nbq.enqueue(i + 1)
        nbq.dequeue() mustEqual i
        nbq.dequeue() mustEqual i + 1
      }
    }

    "single sender, single receiver" in {
      val nbq = NonBlockingQueue[Int](2)
      val exchange = 10
      val sendr = fork {
        for (i <- 0 until exchange)
          nbq.enqueue(i)
      }
      val recvr = fork {
        for (i <- 0 until exchange)
          nbq.dequeue() mustEqual i
      }
      // block for completion
      sendr.get; recvr.get
    }

    "many senders, many receivers" in {
      System.err.println("cap\tsendrs\trecvrs\tnbq\tabq\tlbq")
      val exchanged = 10000000
      val qcons = Seq[Int => Queue[String]](Queue.nbq _, Queue.abq _, Queue.lbq _)
      for {
        sendrs <- 2 to 64 by 4
        recvrs <- 2 to 64 by 4
        capacity <- 16 to 256 by 32
      } {
        System.err.print(
          "%d\t%d\t%d\t".format(capacity, sendrs, recvrs)
        )
        val times = qcons.map { q =>
          sendrecv(q(capacity), exchanged, sendrs, recvrs)
        }
        System.err.println("%d\t%d\t%d".format(times: _*))
      }
    }
  }
}
object NonBlockingQueueSpec {
  val executor = Executors.newCachedThreadPool()
  def fork[T](body: => T): java.util.concurrent.Future[T] =
    executor.submit(
      new Callable[T] {
        def call(): T = body
      }
    )

  def sendrecv(
    queue: Queue[String],
    roughExchange: Int,
    sendrCount: Int,
    recvrCount: Int
  ): Long = {
    val exchangePerSendr = roughExchange / sendrCount
    val exchange = exchangePerSendr * sendrCount
    val remaining = new atomic.AtomicInteger(exchange)
    val start = System.currentTimeMillis
    val sendrs = (0 until sendrCount).map { _ =>
      fork {
        for (i <- 0 until exchangePerSendr)
          queue.enqueue(i.toString)
      }
    }
    val recvrs = (0 until recvrCount).map { _ =>
      fork {
        while (remaining.getAndDecrement > 0)
          queue.dequeue()
      }
    }
    // block for completion
    for (task <- sendrs ++ recvrs)
      task.get
    System.currentTimeMillis - start
  }

  object Queue {
    def nbq[T : Manifest](capacity: Int) =
      new Queue[T] {
        val q = NonBlockingQueue[T](capacity)
        def enqueue(e: T) = q.enqueue(e)
        def dequeue(): T = q.dequeue()
        override def toString = q.getClass.getSimpleName
      }

    def abq[T](capacity: Int) =
      bq(new java.util.concurrent.ArrayBlockingQueue[T](capacity, false))
    def lbq[T](capacity: Int) =
      bq(new java.util.concurrent.LinkedBlockingQueue[T](capacity))
    private def bq[T](q: java.util.concurrent.BlockingQueue[T]) =
      new Queue[T] {
        def enqueue(e: T) = q.put(e)
        def dequeue(): T = q.take()
        override def toString = q.getClass.getSimpleName
      }
  }
  trait Queue[@specialized T] {
    def enqueue(e: T): Unit
    def dequeue(): T
  }
}
