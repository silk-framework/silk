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

package org.silkframework.plugins.spatial.transformer

import org.scalatest.Matchers
import org.scalatest.FlatSpec



/**
 * Tests the Buffer Transformer.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */


class BufferTransformerTest extends FlatSpec with Matchers {

  val transformer = new BufferTransformer()

  //Buffer of a square with distance 0.
  "BufferTransformer test 1" should "return '\"POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0))\"" in {
    transformer.evaluate("POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0, 1 1, 0 0))") should equal("POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0))")
  }
  
}
