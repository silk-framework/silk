package de.fuberlin.wiwiss.silk.server.model

import java.io.{FileNotFoundException, File}
import de.fuberlin.wiwiss.silk.util.StringUtils._

/**
 * The Silk Server configuration.
 * The following properties are supported:
 *   - `configDir`: The directory where the Silk configuration files can be found
 *   - `writeUnknownInstances`: Specifies whether unmatched instances should be added to the instance cache
 *   - `returnUnknownInstances`: Specifies whether the server response should contain unknown instances too
 */
object ServerConfig
{
    /**
     * Loads the server configuration.
     */
    def load() =
    {
        val configDir = new File(System.getProperty("configDir", "./config"))
        if(!configDir.exists) throw new FileNotFoundException("Config directory " + configDir + " not found. Please specify a valid configuration directory using the system property 'configDir'")

        val writeUnknownInstances = System.getProperty("writeUnknownInstances", "false") match
        {
            case BooleanLiteral(b) => b
            case _ => throw new IllegalArgumentException("'writeUnknownInstances' must be a boolean")
        }

        val returnUnknownInstances = System.getProperty("returnUnknownInstances", "false") match
        {
            case BooleanLiteral(b) => b
            case _ => throw new IllegalArgumentException("'returnUnknownInstances' must be a boolean")
        }

        ServerConfig(configDir, writeUnknownInstances, returnUnknownInstances)
    }
}

case class ServerConfig(configDir : File ,
                        writeUnknownInstances : Boolean,
                        returnUnknownInstances : Boolean)
