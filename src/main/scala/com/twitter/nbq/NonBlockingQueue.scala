package com.twitter.nbq
import NonBlockingQueue._

import java.util.concurrent.atomic.AtomicLong
import scala.annotation.tailrec

class NonBlockingQueue[@specialized T : Manifest] private (capacity: Int) {
  val buffer = new Array[T](capacity)
  // the high order bit in the read and write positions is used to indicate
  // that a thread is currently reading/writing: exactly one thread may be
  // in that position at a time
  // the position of the next producable item
  //val writeRef = new AtomicLong(0)
  // the position of the next unconsumed item
  val headRef = new AtomicLong(0)
  // the position of the next consumable item
  //val readRef = new AtomicLong(0)
  // the position of the next produced item
  val tailRef = new AtomicLong(0)

  private final def abs(long: Long): Long =
    ABSOLUTE_MASK & long
  private final def locked(long: Long): Boolean =
    (SIGN_MASK & long) == SIGN_MASK
  private final def lock(long: Long): Long =
    SIGN_MASK | long

  @tailrec
  final def enqueue(e: T, attempt: Int = 1): Unit = {
    val tail = tailRef.get
    val write = abs(headRef.get)
    if (!locked(tail) && tail - write < capacity && tailRef.compareAndSet(tail, lock(tail))) {
      //println("++: " + write + "/" + tail)
      // we now own the slot at 'tail'
      buffer((tail % capacity).toInt) = e
      // unlock position
      tailRef.set(tail + 1)
      // success
      return
    } else {
      if ((Thread.currentThread.getId - attempt) % 64 == 0) {
        //println("++: busy attempt %d at %d/%d".format(attempt, write, tail))
        Thread.`yield`
      }
      enqueue(e, attempt + 1)
    }
  }

  @tailrec
  final def dequeue(attempt: Int = 1): T = {
    val head = headRef.get
    val read = abs(tailRef.get)
    if (!locked(head) && read - head > 0 && headRef.compareAndSet(head, lock(head))) {
      //println("--: " + head + "/" + read)
      // we now own the slot at 'head'
      val e = buffer((head % capacity).toInt)
      // unlock position
      headRef.set(head + 1)
      // success
      e
    } else {
      if ((Thread.currentThread.getId - attempt) % 64 == 0) {
        //println("--: busy attempt %d at %d/%d".format(attempt, head, read))
        Thread.`yield`
      }
      dequeue(attempt + 1)
    }
  }
}
object NonBlockingQueue {
  /** Hide specialization behind a factory function. */
  def apply[T : Manifest](capacity: Int): NonBlockingQueue[T] =
    new NonBlockingQueue(capacity)

  val SIGN_MASK: Long = 1L << 63
  val ABSOLUTE_MASK: Long = ~(SIGN_MASK)
}
