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

package org.silkframework.plugins.dataset.rdf.formatters

import org.silkframework.dataset.rdf.LinkFormatter
import org.silkframework.entity.{Link, ValueType}
import org.silkframework.plugins.dataset.rdf.RdfFormatUtil

case class NTriplesLinkFormatter() extends LinkFormatter with EntityFormatter {

  override def formatLink(link: Link, predicateUri: String, inversePredicateUri: Option[String]): String = {
    val statement = "<" + link.source + "> <" + predicateUri + "> <" + link.target + "> .\n"
    inversePredicateUri match {
      case Some(inversePredicateUri) =>
        val inverseStatement = "<" + link.target + "> <" + inversePredicateUri + "> <" + link.source + "> .\n"
        statement + inverseStatement
      case None =>
        statement
    }
  }

  override def formatLiteralStatement(subject: String, predicate: String, value: String, valueType: ValueType): String = {
    RdfFormatUtil.tripleValuesToNTriplesSyntax(subject, predicate, value, valueType)
  }
}