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

package org.silkframework.plugins.transformer.combine

import org.scalatest.{FlatSpec, Matchers}




class ConcatTransformerTest extends FlatSpec with Matchers {

  val transformer = new ConcatTransformer()

  "ConcatTransformer" should "return 'abcdef'" in {
    transformer.apply(Seq(Seq("abc"), Seq("def"))) should equal(Seq("abcdef"))
  }

  val transformer1 = new ConcatTransformer()

  "ConcatTransformer" should "return 'def123'" in {
    transformer1.apply(Seq(Seq("def"), Seq("123"))) should equal(Seq("def123"))
  }
}
