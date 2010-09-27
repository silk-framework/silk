package de.fuberlin.wiwiss.silk.hadoop

import impl._
import org.apache.hadoop.mapreduce._
import lib.output.FileOutputFormat
import org.apache.hadoop.io.Text
import java.util.logging.Logger
import org.apache.hadoop.fs.Path
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

object Silk
{
    private val logger = Logger.getLogger(Silk.getClass.getName)

    def main(args : Array[String])
    {
        DefaultImplementations.register()

        val startTime = System.currentTimeMillis()
        logger.info("Silk started")

        val job = new Job()
        job.setJarByClass(classOf[SilkInputFormat])

        if(args.length < 2)
        {
            println("Usage: Load configFile ouputDir [linkSpec]")
            System.exit(1)
        }
        job.getConfiguration.set("silk.instancecache.path", args(0))
        job.getConfiguration.set("silk.output.path", args(1))
        if(args.length >= 3)
        {
            job.getConfiguration.set("silk.linkspec", args(2))
        }

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
