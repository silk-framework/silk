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

import de.fuberlin.wiwiss.silk.runtime.task.ValueTask.ValueHolder
import de.fuberlin.wiwiss.silk.util.Observable

/**
 * A task where the intermediate results of the computation can be retrieved.
 */
abstract class ValueTask[T](initialValue: T) extends Task[T]  {
  /** Holds the current value of this task */
  val value = new ValueHolder(initialValue)

  def executeSubValueTask(subTask: ValueTask[T], finalProgress: Double = 1.0): T = {
    val listener = (v: T) => value.update(v)
    subTask.value.onUpdate(listener)
    executeSubTask(subTask, finalProgress)
  }
}

object ValueTask {
  /**
   * Holds a value.
   */
  class ValueHolder[T](initialValue: T) extends Observable[T] {
      @volatile private var currentValue = initialValue

      def get: T = currentValue

      def update(v: T) {
        currentValue = v
        publish(v)
      }
  }
}