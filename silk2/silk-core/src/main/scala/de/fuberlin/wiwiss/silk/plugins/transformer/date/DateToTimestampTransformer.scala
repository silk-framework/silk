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

import javax.xml.datatype.DatatypeFactory

import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin;

/**
 * Convert an xsd:date to a Unix timestamp.
 *
 * @author Robert Isele
 */
@Plugin(
  id = "datetoTimestamp",
  categories = Array("Date"),
  label = "Date to timestamp",
  description = "Convert an xsd:date to a Unix timestamp"
)
class DateToTimestampTransformer extends SimpleTransformer {

  private val datatypeFactory = DatatypeFactory.newInstance()

  override def evaluate(value: String) = {
    val millis = datatypeFactory.newXMLGregorianCalendar(value).toGregorianCalendar.getTimeInMillis
    millis.toString
  }
}
