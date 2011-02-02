package de.fuberlin.wiwiss.silk.hadoop

import impl.HadoopInstanceCache
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import org.apache.hadoop.fs.{FileSystem, Path}
import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, InstanceCache, Instance}

object Block
{
  private val logger = Logger.getLogger(Block.getClass.getName)

  private val hadoopConfig = new org.apache.hadoop.conf.Configuration()

  def main(args : Array[String])
  {
    if(args.length < 3)
    {
      println("Usage: Block configFile inputDir ouputDir")
      System.exit(1)
    }
    val configPath = new Path(args(0))
    val inputPath = new Path(args(1))
    val outputPath = new Path(args(2))

    DefaultImplementations.register()

    val config = loadConfig(configPath, outputPath)

    for(linkSpec <- config.linkSpecs)
    {
      block(config, linkSpec, inputPath, outputPath)
    }
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
      Configuration.load(stream)
    }
    finally
    {
      stream.close()
    }
  }

  private def block(config : Configuration, linkSpec : LinkSpecification, inputPath : Path, outputPath : Path)
  {
    blockCache(config, linkSpec, inputPath.suffix("/source/"), outputPath.suffix("/source/"))
    blockCache(config, linkSpec, inputPath.suffix("/target/"), outputPath.suffix("/target/"))
  }

  private def blockCache(config : Configuration, linkSpec : LinkSpecification, inputPath : Path, outputPath : Path)
  {
    val inputFS = FileSystem.get(inputPath.toUri, hadoopConfig)
    val outputFS = FileSystem.get(outputPath.toUri, hadoopConfig)

    val numBlocks = config.blocking.map(_.blocks).getOrElse(1)
    val instanceSpecs = InstanceSpecification.retrieve(linkSpec, config.prefixes)
    val inputCache = new HadoopInstanceCache(instanceSpecs.source, inputFS, inputPath.suffix("/" + linkSpec.id + "/"), 1)
    val outputCache = new HadoopInstanceCache(instanceSpecs.target, outputFS, outputPath.suffix("/" + linkSpec.id + "/"), numBlocks)

    val instances = new Traversable[Instance]
    {
      override def foreach[U](f : (Instance) => U) : scala.Unit =
      {
        for(block <- 0 until inputCache.blockCount;
            partition <- 0 until inputCache.partitionCount(block);
            instance <- inputCache.read(block, partition))
        {
          f(instance)
        }
      }
    }

    //TODO enable blocking
    outputCache.write(instances)
    //outputCache.write(instances, linkSpec.blocking)
  }
}
