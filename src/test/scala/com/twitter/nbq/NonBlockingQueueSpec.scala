package com.twitter.nbq

import java.util.concurrent.{Callable, Executors}

import org.specs.Specification
import NonBlockingQueueSpec._

class NonBlockingQueueSpec extends Specification {
  "NonBlockingQueue" should {
    "enqueue and dequeue" in {
      val nbq = NonBlockingQueue[Int](2)
      for (i <- 0 until 10) {
        nbq.enqueue(i)
        nbq.enqueue(i + 1)
        nbq.dequeue mustEqual i
        nbq.dequeue mustEqual i + 1
      }
    }

    "multithreaded enqueue and dequeue" in {
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
}
