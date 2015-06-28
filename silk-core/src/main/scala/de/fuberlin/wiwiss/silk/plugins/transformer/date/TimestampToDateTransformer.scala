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

import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import scala.math.BigInt;

/**
 * Convert Unix timestamp to xsd:date.
 *
 * @author Julien Plu
 */
@Plugin(
  id = "timeToDate",
  categories = Array("Date"),
  label = "Timestamp to date",
  description = "convert Unix timestamp to xsd:date"
)
case class TimestampToDateTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = {
    val cal = new GregorianCalendar()
    cal.setTimeInMillis((BigInt.apply(value) * 1000).longValue())
    val format = new SimpleDateFormat("yyyy-MM-dd")
    val date = format.format(cal.getTime)
    
    date.toString
  }
}
