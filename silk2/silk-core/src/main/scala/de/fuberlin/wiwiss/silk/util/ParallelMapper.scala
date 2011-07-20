package de.fuberlin.wiwiss.silk.util

import collection.mutable.{Queue, Buffer, SynchronizedBuffer, ArrayBuffer}

class ParallelMapper[T](traversable: Traversable[T], threadCount: Int = 8) {
  val queue = new Queue[T]()

  def map[U](f: T => U): Traversable[U] = {
    val buffer = new ArrayBuffer[U]() with SynchronizedBuffer[U]

    //Start worker threads in the background
    val threads = for (i <- 0 until threadCount) yield new WorkerThread(f, buffer, i)
    threads.foreach(_.start())

    //Add all elements to the queue
    traversable.foreach(e => queue.synchronized { queue.enqueue(e) })

    //Wait until worker threads are finished
    while (!queue.isEmpty) {
      Thread.sleep(100)
    }
    threads.foreach(_.interrupt())
    threads.foreach(_.join())

    buffer.toTraversable
  }

  private class WorkerThread[U](map: T => U, buffer: Buffer[U], id: Int) extends Thread {
    override def run() {
      try {
        while (!isInterrupted) {
          var e: Option[T] = None

          queue.synchronized {
            if (!queue.isEmpty) e = Some(queue.dequeue)
          }

          e match {
            case Some(el) => {
              val m = map(el)
              buffer.append(m)
            }
            case None => {
              Thread.sleep(100)
            }
          }
        }
      }
      catch {
        case ex: InterruptedException =>
        case ex: Exception => ex.printStackTrace()
      }
    }
  }

}
