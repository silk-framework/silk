package de.fuberlin.wiwiss.silk.hadoop

import impl._
import org.apache.hadoop.mapreduce._
import lib.output.FileOutputFormat
import org.apache.hadoop.io.Text
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import org.apache.hadoop.fs.Path

/**
 * Executes Silk - MapReduce.
 * Prior to running this, the instance cache must be created using the Load class.
 *
 * @see Load
 */
class Match(inputPath : String, outputPath : String, linkSpec : Option[String], hadoopConfig : org.apache.hadoop.conf.Configuration)
{
  private val logger = Logger.getLogger(getClass.getName)

  def apply()
  {
    val job = new Job(hadoopConfig)

    configJob(job)
    setupJob(job)
    runJob(job)
  }

  /**
   * Sets the Hadoop job configuration parameters.
   */
  private def configJob(job : Job)
  {
    job.getConfiguration.set(SilkConfiguration.InputParam, inputPath)
    job.getConfiguration.set(SilkConfiguration.OutputParam, outputPath)
    for(spec <- linkSpec)
    {
      job.getConfiguration.set(SilkConfiguration.LinkSpecParam, spec)
    }
  }

  /**
   * Sets the Hadoop job up.
   */
  private def setupJob(job : Job)
  {
    DefaultImplementations.register()

    val config = SilkConfiguration.get(job.getConfiguration)

    //General settings
    job.setJobName("Silk")
    job.setJarByClass(classOf[SilkInputFormat])

    //Set Input
    job.setInputFormatClass(classOf[SilkInputFormat])

    //Set Mapper
    job.setMapperClass(classOf[SilkMap])

    //Set Reducer
    if(config.linkSpec.filter.limit.isDefined)
    {
      job.setReducerClass(classOf[SilkReduce])
    }
    else
    {
      job.setNumReduceTasks(0)
    }

    //Set Output
    FileOutputFormat.setOutputPath(job, config.outputPath)

    job.setOutputFormatClass(classOf[SilkOutputFormat])
    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[InstanceSimilarity])
  }

  /**
   * Runs the Hadoop job
   */
  private def runJob(job : Job)
  {
    val startTime = System.currentTimeMillis()
    logger.info("Running MapReduce job")

    job.waitForCompletion(true)

    logger.info("Total time: " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
  }
}
