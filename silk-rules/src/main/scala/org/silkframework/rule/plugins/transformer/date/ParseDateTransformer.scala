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

package org.silkframework.rule.plugins.transformer.date

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.LocaleOptionParameter
import org.silkframework.runtime.plugin.types.autoComlpetionProviders.LocaleParameterAutoCompletionProvider
import org.silkframework.runtime.validation.ValidationException

import java.text.{DateFormat, ParseException, SimpleDateFormat}
import java.util.Locale;

/**
 * Parses a date, returning an xsd:date.
 */
@Plugin(
  id = "parseDate",
  categories = Array("Date"),
  label = "Parse date pattern",
  description = "Parses a date based on a specified pattern, returning an xsd:date."
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("format", "dd.MM.yyyy"),
    input1 = Array("03.04.2015"),
    output = Array("2015-04-03")
  ),
  new TransformExample(
    parameters = Array("format", "dd.MM.yyyy"),
    input1 = Array("3.4.2015"),
    output = Array("2015-04-03")
  ),
  new TransformExample(
    parameters = Array("format", "yyyyMMdd"),
    input1 = Array("20150403"),
    output = Array("2015-04-03")
  ),
  new TransformExample(
    parameters = Array("format", "MMM yyyy", "locale", "en"),
    input1 = Array("May 2024"),
    output = Array("2024-05-01")
  ),
  new TransformExample(
    parameters = Array("format", "MMM yyyy", "locale", "de"),
    input1 = Array("Mai 2024"),
    output = Array("2024-05-01")
  ),
  new TransformExample(
    parameters = Array("format", "MMM yyyy", "locale", "de"),
    input1 = Array("May 2024"),
    throwsException = classOf[org.silkframework.runtime.validation.ValidationException]
  ),
  new TransformExample(
    parameters = Array("format", "yyyyMMdd", "lenient", "false"),
    input1 = Array("20150000"),
    throwsException = classOf[org.silkframework.runtime.validation.ValidationException]
  )
))
case class ParseDateTransformer(
  @Param("The date pattern used to parse the input values")
  format: String = "dd-MM-yyyy",
  @Param("If set to true, the parser tries to use heuristics to parse dates with invalid fields (such as a day of zero).")
  lenient: Boolean = false,
  @Param(value = "Optional locale for the date format. If not set the system's locale will be used.",
    autoCompletionProvider = classOf[LocaleParameterAutoCompletionProvider])
  locale: LocaleOptionParameter = LocaleOptionParameter(None)) extends Transformer with Serializable {

  def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatten.flatMap(parse)
  }

  def parse(value: String): Seq[String] = {
    try {
      // Parse date
      val dateFormat = locale.value match {
        case Some(l) => new SimpleDateFormat(format, new Locale(l))
        case None => new SimpleDateFormat(format)
      }
      dateFormat.setLenient(lenient)
      val date = dateFormat.parse(value)

      // Format as XSD date
      val xsdFormat = new SimpleDateFormat("yyyy-MM-dd")
      val xsdDate = xsdFormat.format(date.getTime)

      Seq(xsdDate)
    }
    catch {
      case ex: ParseException =>
        throw new ValidationException(s"Misformatted date. Expected format: $format", ex)
    }
  }
}
