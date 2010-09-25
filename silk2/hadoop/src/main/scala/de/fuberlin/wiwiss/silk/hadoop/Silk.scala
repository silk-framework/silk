package de.fuberlin.wiwiss.silk.hadoop

import impl._
import org.apache.hadoop.mapreduce._
import lib.output.FileOutputFormat
import org.apache.hadoop.io.Text
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import java.util.logging.Logger
import org.apache.hadoop.fs.{FileSystem, Path}
import de.fuberlin.wiwiss.silk.config.ConfigLoader

object SilkConfiguration
{
    @volatile var config : SilkConfiguration = null

    def get(hadoopConfig : org.apache.hadoop.conf.Configuration) =
    {
        //This method is not synchronized, because multiple instantiation of SilkConfiguration would not be a problem
        if(config == null)
        {
            config = new SilkConfiguration(hadoopConfig)
        }
        config
    }
}

class SilkConfiguration private(hadoopConfig : org.apache.hadoop.conf.Configuration)
{
    def instanceCachePath = new Path(hadoopConfig.get("silk.instancecache.path"))

    //TODO use default hadoop path instead?
    def outputPath = new Path(hadoopConfig.get("silk.output.path"))

    private lazy val cacheFS = FileSystem.get(instanceCachePath.toUri, hadoopConfig)

    lazy val config =
    {
        DefaultImplementations.register()
        ConfigLoader.load(cacheFS.open(instanceCachePath.suffix("/config.xml")))
    }

    lazy val linkSpec = config.linkSpecs.values.head

    lazy val sourceCache =
    {
        val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
        new HadoopInstanceCache(cacheFS, instanceCachePath.suffix("/source/"), numBlocks)
    }

    lazy val targetCache =
    {
        val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
        new HadoopInstanceCache(cacheFS, instanceCachePath.suffix("/target/"), numBlocks)
    }
}

object Silk
{
    private val logger = Logger.getLogger(Silk.getClass.getName)

    def main(args : Array[String])
    {
        val startTime = System.currentTimeMillis()
        logger.info("Silk started")

        val job = new Job()
        job.setJarByClass(classOf[SilkInputFormat])

        job.getConfiguration.set("silk.instancecache.path", args(0))
        job.getConfiguration.set("silk.output.path", args(1))

        val config = SilkConfiguration.get(job.getConfiguration)

        //Input
        job.setInputFormatClass(classOf[SilkInputFormat])

        //Map
        job.setMapperClass(classOf[SilkMap])

        //Reduce
        if(config.linkSpec.filter.limit.isDefined)
        {
            job.setReducerClass(classOf[SilkReduce])
        }
        else
        {
            job.setNumReduceTasks(0)
        }

        //Output
        FileOutputFormat.setOutputPath(job, new Path(args(1)))

        job.setOutputFormatClass(classOf[SilkOutputFormat])
        job.setOutputKeyClass(classOf[Text])
        job.setOutputValueClass(classOf[InstanceSimilarity])

        //Start Job
        job.waitForCompletion(true)

        logger.info("Total time: " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }
}
