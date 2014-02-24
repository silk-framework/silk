/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.hadoop

import impl._
import org.apache.hadoop.mapreduce._
import lib.output.FileOutputFormat
import org.apache.hadoop.io.Text
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.plugins.Plugins
import org.apache.hadoop.fs.Path

/**
 * Executes Silk - MapReduce.
 * Prior to running this, the entity cache must be created using the Load class.
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
    Plugins.register()

    val config = SilkConfiguration.get(job.getConfiguration)

    //General settings
    job.setJobName("Silk")
    job.setJarByClass(classOf[SilkInputFormat])

    //Set Input
    job.setInputFormatClass(classOf[SilkInputFormat])

    //Set Mapper
    job.setMapperClass(classOf[SilkMap])

    //Set Reducer
    if(config.linkSpec.rule.filter.limit.isDefined)
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
    job.setOutputValueClass(classOf[EntityConfidence])
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
