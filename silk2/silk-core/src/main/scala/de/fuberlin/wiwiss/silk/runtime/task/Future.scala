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

package de.fuberlin.wiwiss.silk.runtime.task

import scala.concurrent.ExecutionContext

/**
 * Represents the result of an asynchronous computation.
 */
trait Future[+T] extends (() => T) {
  /**
   * Blocks until the computation to complete, and then retrieves its result.
   */
  override def apply(): T

  /**
   * Returns true if the result is available.
   */
  def isSet: Boolean
}

object Future {
  implicit def fromJavaFuture[T](future: java.util.concurrent.Future[T]) = {
    new Future[T] {
      def apply() = future.get

      def isSet = future.isDone
    }
  }

  implicit def toScalaFuture[T](future: Future[T])(implicit execctx: ExecutionContext) = scala.concurrent.Future { future() }
}