package de.fuberlin.wiwiss.silk.server.model

import java.io.{FileNotFoundException, File}
import de.fuberlin.wiwiss.silk.util.StringUtils._

/**
 * The Silk Server configuration.
 * The following properties are supported:
 *   - `configDir`: The directory where the Silk configuration files can be found
 *   - `writeUnknownEntities`: Specifies whether unmatched entities should be added to the entity cache
 *   - `returnUnknownEntities`: Specifies whether the server response should contain unknown entities too
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

        ServerConfig(configDir, writeUnknownEntities, returnUnknownEntities)
    }
}

case class ServerConfig(configDir : File ,
                        writeUnknownEntities : Boolean,
                        returnUnknownEntities : Boolean)
