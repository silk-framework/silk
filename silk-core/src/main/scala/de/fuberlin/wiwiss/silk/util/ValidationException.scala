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

package de.fuberlin.wiwiss.silk.util

import de.fuberlin.wiwiss.silk.util.ValidationException.ValidationError

/**
 * Thrown if the configuration is not valid.
 */
class ValidationException(val errors: Seq[ValidationError], cause: Throwable) extends Exception(errors.mkString("\n")) {
  def this(errors: Seq[ValidationError]) = this(errors, null)

  def this(error: String, cause: Throwable) = this(ValidationError(error) :: Nil, cause)

  def this(error: String, id: Identifier) = this(ValidationError(error, Some(id)) :: Nil, null)

  def this(error: String, id: Identifier, elementType: String) = this(ValidationError(error, Some(id), Some(elementType)) :: Nil, null)

  def this(error: String) = this(ValidationError(error) :: Nil, null)
}

object ValidationException {
  case class ValidationError(message: String, id: Option[Identifier] = None, elementType: Option[String] = None) {
    override def toString = id match {
      case Some(identifier) => "Validation error in " + elementType.getOrElse("element") + " with id '" + identifier + "': " + message
      case None => message
    }
  }
}