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

package de.fuberlin.wiwiss.silk.server.model

import java.io.{FileNotFoundException, File}
import de.fuberlin.wiwiss.silk.util.StringUtils._

/**
 * The Silk Server configuration.
 * The following properties are supported:
 *   - `configDir`: The directory where the Silk configuration files can be found
 *   - `writeUnknownEntities`: Specifies whether unmatched entities should be added to the entity cache
 *   - `returnUnknownEntities`: Specifies whether the server response should contain unknown entities too
 *   - `matchOnlyInProvidedGraph`: Specifies whether to look for links within all data (pulled from the datasource as
 *   well as data provided to service method in previous calls) or only within the data provided to the service
 *   method in the current call.
 */
object ServerConfig {
    /**
     * Loads the server configuration.
     */
    def load() = {
        val configDir = new File(System.getProperty("configDir", "./config"))
        if(!configDir.exists) throw new FileNotFoundException("Config directory " + configDir + " not found. Please specify a valid configuration directory using the system property 'configDir'")

        val writeUnknownEntities = System.getProperty("writeUnknownEntities", "false") match {
            case BooleanLiteral(b) => b
            case _ => throw new IllegalArgumentException("'writeUnknownEntities' must be a boolean")
        }

        val returnUnknownEntities = System.getProperty("returnUnknownEntities", "false") match {
            case BooleanLiteral(b) => b
            case _ => throw new IllegalArgumentException("'returnUnknownEntities' must be a boolean")
        }

        val matchOnlyInProvidedGraph = System.getProperty("matchOnlyInProvidedGraph", "false") match {
            case BooleanLiteral(b) => b
            case _ => throw new IllegalArgumentException("'matchOnlyInProvidedGraph' must be a boolean")
        }

        ServerConfig(configDir, writeUnknownEntities, returnUnknownEntities, matchOnlyInProvidedGraph)
    }
}

case class ServerConfig(configDir : File ,
                        writeUnknownEntities : Boolean,
                        returnUnknownEntities : Boolean,
                        matchOnlyInProvidedGraph: Boolean)
