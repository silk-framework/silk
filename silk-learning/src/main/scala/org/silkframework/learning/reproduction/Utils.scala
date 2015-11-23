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

package org.silkframework.learning.reproduction

import util.Random
import org.silkframework.learning.individual.Node

private object Utils {

  def crossoverNodes[T <: Node](n1: List[T], n2: List[T]): List[T] = {
    //Interleave both node lists
    val interleaved = (n1 zip n2).flatMap {
      case (a, b) => if (Random.nextBoolean) a :: b :: Nil else b :: a :: Nil
    }

    //Randomly remove about half of the nodes
    interleaved.filter(_ => Random.nextBoolean)
  }
}