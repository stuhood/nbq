package com.twitter.nbq

import java.util.concurrent.atomic.AtomicLong

object NonBlockingQueue {
  /** Hide specialization behind a factory function. */
  def apply[T : Manifest](capacity: Int): NonBlockingQueue[T] =
    new NonBlockingQueue(capacity)
}
class NonBlockingQueue[@specialized T : Manifest] private (capacity: Int) {
  // the invariant is that head <= read <= tail:
  // the position of the next unconsumed item
  val headRef = new AtomicLong(0)
  // the position of the next consumable item
  val readRef = new AtomicLong(0)
  // the position of the next produced item
  val tailRef = new AtomicLong(0)
  val buffer = new Array[T](capacity)

  def enqueue(e: T): Unit = {
    var tail = tailRef.get
    while (tail - headRef.get <= capacity) {
      if (tailRef.compareAndSet(tail, tail + 1)) {
        println("enqueue: " + tail + ", " + (tail % capacity))
        // we now own the slot at 'tail'
        buffer((tail % capacity).toInt) = e
        // loop forever to increment readRef to indicate the position is readable:
        // this prevents interleaving of the tail/read increments
        do {} while (!readRef.compareAndSet(tail, tail + 1))
        // success
        return;
      }
      tail = tailRef.get
    }
    throw new Exception("Queue is full: TODO: implement adaptive blocking.")
  }

  def dequeue(): T = {
    var head = headRef.get
    // we may not read past the readRef
    while (readRef.get - head > 0) {
      if (headRef.compareAndSet(head, head + 1)) {
        println("dequeue: " + head + ", " + (head % capacity))
        // we now own the slot at 'head'
        return buffer((head % capacity).toInt);
      }
      head = headRef.get
    }
    throw new Exception("Queue is empty: TODO: implement adaptive blocking.")
  }
}