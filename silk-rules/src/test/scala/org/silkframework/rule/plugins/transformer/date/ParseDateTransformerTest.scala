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

package org.silkframework.rule.plugins.transformer.date

import org.silkframework.entity.{DateValueType, GeneralDateValueType}
import org.silkframework.rule.test.TransformerTest

class ParseDateTransformerTest extends TransformerTest[ParseDateTransformer] {

  it should "format dates as xsd:date, so that it validates using the corresponding date type" in {
    val transformer = ParseDateTransformer("dd.MM.yyyy")
    val formattedDate = transformer.apply(Seq(Seq("01.02.2012"))).head
    GeneralDateValueType.validate(formattedDate) shouldBe true
  }

}
