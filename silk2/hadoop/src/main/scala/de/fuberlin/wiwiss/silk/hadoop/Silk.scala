package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.fs.Path
import java.io._
import org.apache.hadoop.mapreduce.lib.output.{FileOutputFormat, TextOutputFormat}
import org.apache.hadoop.mapreduce._
import de.fuberlin.wiwiss.silk.util.FileUtils._
import org.apache.hadoop.io.{NullWritable, Text}
import de.fuberlin.wiwiss.silk.config.{ConfigLoader, Configuration}
import de.fuberlin.wiwiss.silk.instance.{InstanceCache, FileInstanceCache}

object Silk
{
    private val partitionCacheDir = new File("./partitionCache/")
    private val outputDir = new File("./output")

    val config = loadConfig()
    val linkSpec = config.linkSpecs.values.head

    val sourceCache : InstanceCache = new FileInstanceCache(new File(partitionCacheDir + "/source/"))
    val targetCache : InstanceCache = new FileInstanceCache(new File(partitionCacheDir + "/target/"))

    def main(args : Array[String])
    {
        val startTime = System.currentTimeMillis()

        runMapReduce()
        
        println("Elapsed time: " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }

    private def loadConfig() : Configuration =
    {
        System.getProperty("configFile") match
        {
            case fileName : String => ConfigLoader.load(new File(fileName))
            case _ => throw new IllegalArgumentException("No configuration file specified. Please set the 'configFile' property")
        }
    }

    private def runMapReduce() : Unit =
    {
        val job = new Job()

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

        outputDir.deleteRecursive()
        FileOutputFormat.setOutputPath(job, new Path(outputDir.getCanonicalPath))

        //Start Job
        job.waitForCompletion(true)
    }
}