package de.fuberlin.wiwiss.silk.hadoop

import impl._
import org.apache.hadoop.mapreduce._
import lib.output.FileOutputFormat
import org.apache.hadoop.io.Text
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

/**
 * Executes Silk - MapReduce.
 * Prior to running this, the instance cache must be created using the Load class.
 *
 * @see Load
 */
object Silk
{
    private val logger = Logger.getLogger(Silk.getClass.getName)

    def main(args : Array[String])
    {
        val job = new Job()

        processArguments(job, args)
        setupJob(job)
        runJob(job)
    }

    /**
     * Processes the command line arguments and sets the corresponding job configuration parameters.
     */
    private def processArguments(job : Job, args : Array[String])
    {
        if(args.length < 2)
        {
            println("Usage: Load inputDir ouputDir [linkSpec]")
            System.exit(1)
        }

        job.getConfiguration.set(SilkConfiguration.InputParam, args(0))
        job.getConfiguration.set(SilkConfiguration.OutputParam, args(1))
        if(args.length >= 3)
        {
            job.getConfiguration.set(SilkConfiguration.LinkSpecParam, args(2))
        }
    }

    /**
     * Sets the Hadoop job up.
     */
    private def setupJob(job : Job)
    {
        DefaultImplementations.register()

        val config = SilkConfiguration.get(job.getConfiguration)

        //Set job jar file
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
        logger.info("Running MapReduc job")

        job.waitForCompletion(true)

        logger.info("Total time: " + ((System.currentTimeMillis - startTime) / 1000.0) + " seconds")
    }
}
