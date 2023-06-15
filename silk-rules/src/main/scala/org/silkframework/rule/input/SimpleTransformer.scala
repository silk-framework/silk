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

package org.silkframework.rule.input

import scala.collection.immutable.ArraySeq

/**
 * Simple transformer which transforms all values of all inputs.
 */
abstract class SimpleTransformer extends Transformer {

  override final def apply(values: Seq[Seq[String]]): Seq[String] = {
    var numberOfValues = 0
    for(inputValues <- values) {
      numberOfValues += inputValues.size
    }

    val output = new Array[String](numberOfValues)
    var count = 0
    for {
      inputValues <- values
      value <- inputValues
    } {
      output(count) = evaluate(value)
      count += 1
    }
    ArraySeq.unsafeWrapArray(output)
  }

  def evaluate(value: String): String
}
