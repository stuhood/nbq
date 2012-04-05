package com.twitter.nbq

import java.util.concurrent.atomic.AtomicLong
import scala.annotation.tailrec

object NonBlockingQueue {
  /** Hide specialization behind a factory function. */
  def apply[T : Manifest](capacity: Int): NonBlockingQueue[T] =
    new NonBlockingQueue(capacity)
}
class NonBlockingQueue[@specialized T : Manifest] private (capacity: Int) {
  val buffer = new Array[T](capacity)
  // the invariant is that head <= read <= tail:
  // the position of the next unconsumed item
  val headRef = new AtomicLong(0)
  // the position of the next consumable item
  val readRef = new AtomicLong(0)
  // the position of the next produced item
  val tailRef = new AtomicLong(0)

  @tailrec
  final def enqueue(e: T): Unit = {
    val tail = tailRef.get
    val head = headRef.get
    if (tail - head < capacity && tailRef.compareAndSet(tail, tail + 1)) {
      println("++: " + head + "/" + tail)
      // we now own the slot at 'tail'
      buffer((tail % capacity).toInt) = e
      // loop forever to increment readRef to indicate the position is readable:
      // this prevents interleaving of the tail/read increments
      while (!readRef.compareAndSet(tail, tail + 1)) {
        println("++: contended at %d".format(tail))
      }
      return // success
    } else {
      println("++: busy at %d/%d in %s"
        .format(head, tail, Thread.currentThread.getName))
      enqueue(e)
    }
  }

  @tailrec
  final def dequeue(): T = {
    val head = headRef.get
    val read = readRef.get
    if (read - head > 0 && headRef.compareAndSet(head, head + 1)) {
      println("--: " + head + "/" + read)
      // we now own the slot at 'head'
      buffer((head % capacity).toInt)
    } else {
      println("--: busy at %d/%d in %s"
        .format(head, read, Thread.currentThread.getName))
      dequeue()
    }
  }
}
