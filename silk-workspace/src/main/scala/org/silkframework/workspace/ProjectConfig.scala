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

package org.silkframework.workspace

import java.util.logging.Logger

import com.typesafe.config.ConfigException
import org.silkframework.config.{SilkVocab, Config, Prefixes}
import org.silkframework.util.Identifier

/**
  * The project specific config.
  *
  * @param id
  * @param prefixes Prefixes that can be used in qualified names instead of providing full URIs.
  * @param projectResourceUriOpt The URI of the project. Usually this is formed by the id and the config parameter
  *                           project.resourceUriPrefix. This URI usually does not change anymore, once it is set.
  */
case class ProjectConfig(id: Identifier = Identifier.random,
                         prefixes: Prefixes = Prefixes.default,
                         projectResourceUriOpt: Option[String] = None) {
  private val log: Logger = Logger.getLogger(getClass.getName)

  def generateDefaultUri: String = {
    try {
      Config().getString("project.resourceUriPrefix") + id.toString
    } catch {
      case e: ConfigException.Missing =>
        log.warning("Application parameter project.resourceUriPrefix is not set. Using default prefix.")
        "urn:silkframework:project:" + id.toString
    }
  }

  /** Returns the project resource URI if set or generates the default URI for this project. */
  def resourceUriOrElseDefaultUri: String = {
    projectResourceUriOpt.getOrElse(generateDefaultUri)
  }
}