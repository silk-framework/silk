package de.fuberlin.wiwiss.silk.hadoop

import impl.HadoopInstanceCache
import de.fuberlin.wiwiss.silk.config.{Configuration, ConfigLoader}
import de.fuberlin.wiwiss.silk.Loader
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import org.apache.hadoop.fs.{FileSystem, Path}
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification

/**
 *
 */
object Load
{
    private val logger = Logger.getLogger(Load.getClass.getName)

    private val hadoopConfig = new org.apache.hadoop.conf.Configuration()

    def main(args : Array[String])
    {
        DefaultImplementations.register()

        //TODO check if arguments are provided
        val configPath = new Path(args(0))
        val instanceCachePath = new Path(args(1))

        val config = loadConfig(configPath, instanceCachePath)

        val linkSpec = config.linkSpecs.values.head

        //for((name, linkSpec) <- config.linkSpecs)
        //{
            write(config, linkSpec, instanceCachePath)
        //}

    }

    private def loadConfig(filePath : Path, instanceCachePath : Path) : Configuration =
    {
        //Create two FileSystem objects, because the config file and the instance cache might be located in different file systems
        val configFS = FileSystem.get(filePath.toUri, hadoopConfig)
        val cacheFS = FileSystem.get(instanceCachePath.toUri, hadoopConfig)

        //Copy the config file into the instance cache directory
        val inputStream = configFS.open(filePath)
        val outputStream = cacheFS.create(instanceCachePath.suffix("/config.xml"))
        try
        {
            val buffer = new Array[Byte](4096)
            var c = inputStream.read(buffer)
            while(c != -1)
            {
                outputStream.write(buffer, 0, c)
                c = inputStream.read(buffer)
            }
        }
        finally
        {
            outputStream.close()
            inputStream.close()
        }

        //Load the configuration
        val stream = configFS.open(filePath)
        try
        {
            ConfigLoader.load(stream)
        }
        finally
        {
            stream.close()
        }
    }

    private def write(config : Configuration, linkSpec : LinkSpecification, instanceCachePath : Path)
    {
        val cacheFS = FileSystem.get(instanceCachePath.toUri, hadoopConfig)

        val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
        val sourceCache = new HadoopInstanceCache(cacheFS, instanceCachePath.suffix("/source/"), numBlocks)
        val targetCache = new HadoopInstanceCache(cacheFS, instanceCachePath.suffix("/target/"), numBlocks)

        val loader = new Loader(config, linkSpec)
        loader.writeCaches(sourceCache, targetCache)
    }
}
