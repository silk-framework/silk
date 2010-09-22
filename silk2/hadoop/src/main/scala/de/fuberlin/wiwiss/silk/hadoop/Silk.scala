package de.fuberlin.wiwiss.silk.hadoop

import impl._
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.io.Text
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import java.util.logging.Logger
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop
import de.fuberlin.wiwiss.silk.config.{Configuration, ConfigLoader}
import java.io.File

object Silk
{
    private val logger = Logger.getLogger(Silk.getClass.getName)

    private val fs = FileSystem.get(new hadoop.conf.Configuration())

    private val instanceCachePath = new Path("instanceCache/")
    private val outputPath = new Path("output/")

    DefaultImplementations.register()

    val config = ConfigLoader.load(fs.open(instanceCachePath.suffix("/config.xml")))
    val linkSpec = config.linkSpecs.values.head

    val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
    val sourceCache = new HadoopInstanceCache(fs, instanceCachePath.suffix("/source/"), numBlocks)
    val targetCache = new HadoopInstanceCache(fs, instanceCachePath.suffix("/target/"), numBlocks)

    def main(args : Array[String])
    {
        val startTime = System.currentTimeMillis()
        logger.info("Silk started")

        val job = new Job()
        job.setJarByClass(classOf[SilkInputFormat])

        //Input
        job.setInputFormatClass(classOf[SilkInputFormat])

        //Map
        job.setMapperClass(classOf[SilkMap])

        //Reduce
        if(linkSpec.filter.limit.isDefined)
        {
            job.setReducerClass(classOf[SilkReduce])
        }
        else
        {
            job.setNumReduceTasks(0)
        }

        //Output
        job.setOutputFormatClass(classOf[SilkOutputFormat])
        job.setOutputKeyClass(classOf[Text])
        job.setOutputValueClass(classOf[InstanceSimilarity])

        fs.delete(outputPath, true)
        FileOutputFormat.setOutputPath(job, outputPath)

        //Start Job
        job.waitForCompletion(true)

        logger.info("Total time: " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }
}
