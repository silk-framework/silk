package de.fuberlin.wiwiss.silk.hadoop

import impl.HadoopInstanceCache
import java.io.File
import de.fuberlin.wiwiss.silk.config.{Configuration, ConfigLoader}
import de.fuberlin.wiwiss.silk.Loader
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import org.apache.hadoop.fs.{FileSystem, Path}
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification

object Load
{
    private val logger = Logger.getLogger(Load.getClass.getName)

    private val instanceCachePath = new Path("instanceCache/")
    private val outputPath = new Path("output/")

    private val fs = FileSystem.get(new org.apache.hadoop.conf.Configuration())

    def main(args : Array[String])
    {
        DefaultImplementations.register()

        val config = loadConfig()

        val linkSpec = config.linkSpecs.values.head

        //for((name, linkSpec) <- config.linkSpecs)
        //{
            write(config, linkSpec)
        //}

    }

    private def loadConfig() : Configuration =
    {
        val configFile = System.getProperty("configFile") match
        {
            case fileName : String => new File(fileName)
            case _ =>
            {
                logger.info("No configuration file specified. Please set the 'configFile' property. Using 'config.xml' per default")
                new File("./config.xml")
            }
        }

        if(!configFile.exists)
        {
            throw new IllegalArgumentException("Config file " + configFile + " not found.")
        }

        fs.copyFromLocalFile(new Path(configFile.getCanonicalPath), instanceCachePath.suffix("/config.xml"))

        ConfigLoader.load(configFile)
    }

    private def write(config : Configuration, linkSpec : LinkSpecification)
    {
        val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
        val sourceCache = new HadoopInstanceCache(fs, instanceCachePath.suffix("/source/"), numBlocks)
        val targetCache = new HadoopInstanceCache(fs, instanceCachePath.suffix("/target/"), numBlocks)

        val loader = new Loader(config, linkSpec)
        loader.writeCaches(sourceCache, targetCache)
    }
}
