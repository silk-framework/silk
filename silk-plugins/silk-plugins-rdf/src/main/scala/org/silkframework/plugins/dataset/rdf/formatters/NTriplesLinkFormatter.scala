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

import java.net.URI

import org.silkframework.entity.Link
import org.silkframework.util.StringUtils.DoubleLiteral

import scala.util.Try

case class NTriplesLinkFormatter() extends LinkFormatter with EntityFormatter {

  override def format(link: Link, predicateUri: String) = {
    "<" + link.source + ">  <" + predicateUri + ">  <" + link.target + "> .\n"
  }

  override def formatLiteralStatement(subject: String, predicate: String, value: String) = {
    value match {
      // Check if value is an URI
      case v if value.startsWith("http") && Try(URI.create(value)).isSuccess =>
        "<" + subject + "> <" + predicate + "> <" + v + "> .\n"
      // Check if value is a number
      case DoubleLiteral(d) =>
        "<" + subject + "> <" + predicate + "> \"" + d + "\"^^<http://www.w3.org/2001/XMLSchema#double> .\n"
      // Write string values
      case _ =>
        val escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        "<" + subject + "> <" + predicate + "> \"" + escapedValue + "\" .\n"
    }
  }
}