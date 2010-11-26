package de.fuberlin.wiwiss.silk.util

import collection.mutable.Queue

class ParallelTraversable[T](traversable : Traversable[T], threadCount : Int = 8) extends Traversable[T]
{
  val queue = new Queue[T]()

  override def foreach[U](f : T => U)
  {
    //Start worker threads in the background
    val threads = for(i <- 0 until threadCount) yield new WorkerThread(f, i)
    threads.foreach(_.start)

    //Add all elements to the queue
    traversable.foreach(e => queue.synchronized { queue.enqueue(e) } )

    //Wait until worker threads are finished
    while(!queue.isEmpty)
    {
      Thread.sleep(100)
    }
    threads.foreach(_.interrupt)
    threads.foreach(_.join)
  }

  private class WorkerThread[U](f : T => U, id : Int) extends Thread
  {
    override def run()
    {
      try
      {
        while(!isInterrupted)
        {
          var e : Option[T] = None

          queue.synchronized { if(!queue.isEmpty) e = Some(queue.dequeue) }

          e match
          {
            case Some(el) =>
            {
              f(el)
            }
            case None =>
            {
              Thread.sleep(100)
            }
          }
        }
      }
      catch
      {
        case ex : InterruptedException =>
      }
    }
  }
}
