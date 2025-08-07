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

package org.silkframework.rule.plugins.transformer.validation

import javax.xml.datatype.DatatypeFactory
import org.silkframework.rule.input.Transformer
import ValidateDateAfter._
import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.validation.ValidationException

@Plugin(
  id = "validateDateAfter",
  categories = Array("Validation", "Date"),
  label = "Validate date after",
  description = "Validates if the first input date is after the second input date. Outputs the first input if the validation is successful."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("2015-04-02"),
    input2 = Array("2015-04-03"),
    throwsException = classOf[org.silkframework.runtime.validation.ValidationException]
  ),
  new TransformExample(
    input1 = Array("2015-04-04"),
    input2 = Array("2015-04-03"),
    output = Array("2015-04-04")
  ),
  new TransformExample(
    parameters = Array("allowEqual", "true"),
    input1 = Array("2015-04-03"),
    input2 = Array("2015-04-03"),
    output = Array("2015-04-03")
  ),
  new TransformExample(
    parameters = Array("allowEqual", "false"),
    input1 = Array("2015-04-03"),
    input2 = Array("2015-04-03"),
    throwsException = classOf[org.silkframework.runtime.validation.ValidationException]
  )
))
case class ValidateDateAfter(
  @Param(value = "Allow both dates to be equal.")
  allowEqual: Boolean = false) extends Transformer {

  def apply(values: Seq[Seq[String]]): Seq[String] = {
    require(values.length == 2, "ValidateDateOrder accepts exactly two inputs")

    val firstDate = datatypeFactory.newXMLGregorianCalendar(values.head.head)
    val secondDate = datatypeFactory.newXMLGregorianCalendar(values(1).head)

    if(firstDate.compare(secondDate) == 0) {
      if(allowEqual)
        values.head
      else
        throw new ValidationException(s"Date $firstDate is not after data $secondDate, but equal")
    } else if(firstDate.compare(secondDate) > 0) {
      values.head
    } else {
      throw new ValidationException(s"Date $firstDate is not after data $secondDate.")
    }
  }
}

object ValidateDateAfter {
  private val datatypeFactory = DatatypeFactory.newInstance()
}
