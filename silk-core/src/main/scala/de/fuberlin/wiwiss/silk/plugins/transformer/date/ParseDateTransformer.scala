/*
 * Copyright 2013 dmdp9553.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.plugins.transformer.date

import java.text.{ParseException, SimpleDateFormat}
import javax.xml.datatype.DatatypeFactory

import de.fuberlin.wiwiss.silk.rule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin;

/**
 * Parses a date, returning an xsd:date.
 */
@Plugin(
  id = "parseDate",
  categories = Array("Date"),
  label = "Parse date",
  description = "Parses a date, returning an xsd:date"
)
case class ParseDateTransformer(format: String = "dd-MM-yyyy") extends Transformer {

  private val datatypeFactory = DatatypeFactory.newInstance()

  def apply(values: Seq[Set[String]]): Set[String] = {
    values.flatten.toSet.flatMap(parse)
  }

  def parse(value: String): Set[String] = {
    try {
      // Parse date
      val dateFormat = new SimpleDateFormat(format)
      val date = dateFormat.parse(value)

      // Format as XSD date
      val xsdFormat = new SimpleDateFormat("yyyy-MM-dd")
      val xsdDate = xsdFormat.format(date.getTime)

      Set(xsdDate)
    }
    catch {
      case ex: ParseException => Set.empty
    }
  }
}
