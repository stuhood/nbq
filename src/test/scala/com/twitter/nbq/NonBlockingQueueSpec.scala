package com.twitter.nbq

import org.specs.Specification

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
  }
}
