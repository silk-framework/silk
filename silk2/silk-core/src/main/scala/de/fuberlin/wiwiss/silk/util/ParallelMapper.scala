/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.util

import collection.mutable.{Queue, Buffer, SynchronizedBuffer, ArrayBuffer}

class ParallelMapper[T](traversable: Traversable[T], threadCount: Int = 8) {
  val queue = new Queue[T]()

  def map[U](f: T => U): Traversable[U] = {
    val buffer = new ArrayBuffer[U]() with SynchronizedBuffer[U]
    val threads = for (i <- 0 until threadCount) yield new WorkerThread(f, buffer, i)

    try {
      //Start worker threads in the background
      threads.foreach(_.start())

      //Add all elements to the queue
      traversable.foreach(e => queue.synchronized { queue.enqueue(e) })

      //Wait until worker threads are finished
      while (!queue.isEmpty) Thread.sleep(100)
    } finally {
      threads.foreach(_.interrupt())
      threads.foreach(_.join())
    }

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
