package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.fs.Path
import java.io._
import org.apache.hadoop.mapreduce.lib.output.{FileOutputFormat, TextOutputFormat}
import org.apache.hadoop.mapreduce._
import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.util.FileUtils._
import org.apache.hadoop.io.{NullWritable, Text}
import de.fuberlin.wiwiss.silk.datasource.{PartitionCache, FilePartitionCache}
import de.fuberlin.wiwiss.silk.config.{ConfigLoader, Configuration}

object Silk
{
    private val partitionCacheDir = new File("./partitionCache/")
    private val outputDir = new File("./output")

    val config = loadConfig()
    val linkSpec = config.linkSpecs.values.head

    val sourcePartitionCache : PartitionCache = new FilePartitionCache(new File(partitionCacheDir + "/" + linkSpec.sourceDatasetSpecification.dataSource.id + "/"))
    val targetPartitionCache : PartitionCache = new FilePartitionCache(new File(partitionCacheDir + "/" + linkSpec.targetDatasetSpecification.dataSource.id + "/"))

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