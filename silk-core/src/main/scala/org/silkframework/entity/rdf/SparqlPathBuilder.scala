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

package org.silkframework.entity.rdf

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths._
import org.silkframework.entity.rdf.SparqlEntitySchema.specialPaths
import org.silkframework.runtime.validation.ValidationException

/**
 * Builds SPARQL patterns from paths.
 */
object SparqlPathBuilder {

  /**
    * Builds a SPARQL pattern for a single path.
    *
    * @param path The path
    * @param subject The subject e.g. ?s or <uri>
    * @param value The value
    */
  def path(path: Path, subject: String = "?s", value: String = "?v", tempVarPrefix: String = "?t", filterVarPrefix: String = "?f"): String = {
    val vars = new Vars(subject, value, tempVarPrefix, filterVarPrefix)
    buildOperators(vars.subject, path.operators, vars).replace(vars.curTempVar, value)
  }

  /**
    * Builds a SPARQL pattern from a sequence of paths.
    *
    * @param paths        The paths
    * @param subject      The subject e.g. ?s or <uri>
    * @param valuesPrefix The value of every path will be bound to a variable of the form: valuesPrefix{path.id}
    * @param useOptional  If the path query should use optionals around the path pattern.
    */
  def apply(paths: Seq[UntypedPath],
            subject: String = "?s",
            valuesPrefix: String = "?v",
            tempVarPrefix: String = "?t",
            filterVarPrefix: String = "?f",
            useOptional: Boolean): String = {
    val vars = new Vars(subject, valuesPrefix, tempVarPrefix, filterVarPrefix)
    paths.zipWithIndex.map {
      case (path, index) => buildPath(path, index, vars, useOptional)
    }.mkString
  }

  /**
   * Builds a SPARQL pattern from a single Path.
   */
  private def buildPath(path: UntypedPath, index: Int, vars: Vars, useOptional: Boolean): String = {
    val pathPattern = buildOperators(vars.subject, path.operators, vars).replace(vars.curTempVar, vars.newValueVar(path, index))
    if(useOptional) {
      s"OPTIONAL {\n$pathPattern}\n"
    } else {
      pathPattern
    }
  }

  /**
   * Builds a SPARQL pattern from a list of operators.
   */
  private def buildOperators(subject: String, operators: List[PathOperator], vars: Vars): String = {
    if (operators.isEmpty) return ""
    validateOperators(operators)

    implicit val prefixes = Prefixes.empty

    val operatorSparql = operators.head match {
      case ForwardOperator(property) =>
        if(forwardSpecialPaths.contains(property.uri)) {
          ""
        } else {
          subject + " <" + property + "> " + vars.newTempVar + " .\n"
        }
      case BackwardOperator(property) => vars.newTempVar + " <" + property + "> " + subject + " .\n"
      case LanguageFilter(op, lang) => "FILTER(lang(" + subject + ") " + op + " '" + lang + "') . \n"
      case PropertyFilter(property, op, value) =>
        val message = s"Property filter ${operators.head.serialize} is not valid, because '$property' is not a valid URI."
        if(!property.isValidUri) {
          if(property.uri == "@lang") {
            val example = LanguageFilter(op, value.stripPrefix("\"").stripSuffix("\"")).serialize
            throw new ValidationException(s"$message If a language filter was intended, use single quotes: $example")
          } else {
            throw new ValidationException(message)
          }
        }
        subject + " <" + property.uri + "> " + vars.newFilterVar + " .\n" +
          "FILTER(" + vars.curFilterVar + " " + op + " " + value + ") . \n"
    }

    if (operators.tail.nonEmpty) {
      operatorSparql + buildOperators(vars.curTempVar, operators.tail, vars)
    } else {
      operatorSparql
    }
  }

  private val forwardSpecialPaths = Set(specialPaths.LANG, specialPaths.TEXT)

  private def validateOperators(operators: List[PathOperator]): Unit = {
    val lastIndex = operators.size - 1
    operators.zipWithIndex.foreach { case (po, idx) =>
      po match {
        case ForwardOperator(p) if forwardSpecialPaths.contains(p.uri) && idx < lastIndex =>
          throw new ValidationException(s"Special path '${p.uri}' is only allowed at the end of a path expression!")
        case BackwardOperator(p) if forwardSpecialPaths.contains(p.uri) =>
          throw new ValidationException(s"Special path '${p.uri}' not allowed as backward path!")
        case _ =>
      }
    }
  }

  /**
   * Holds all variables used during the construction of a SPARQL pattern.
   */
  private class Vars(val subject: String = "?s", val valuesPrefix: String = "?v", val tempVarPrefix: String = "?t", val filterVarPrefix: String = "?f") {

    private var tempVarIndex = 0

    private var filterVarIndex = 0

    def newTempVar: String = {
      tempVarIndex += 1; tempVarPrefix + tempVarIndex
    }

    def curTempVar: String = tempVarPrefix + tempVarIndex

    def newFilterVar: String = {
      filterVarIndex += 1; filterVarPrefix + filterVarIndex
    }

    def curFilterVar: String = filterVarPrefix + filterVarIndex

    def newValueVar(path: UntypedPath, index: Int): String = valuesPrefix + index
  }

}
