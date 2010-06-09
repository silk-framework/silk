package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import java.io.DataOutputStream
import org.apache.hadoop.mapreduce.{TaskAttemptContext, RecordWriter}
import org.apache.hadoop.io.{Text, NullWritable}

class SilkOutputFormat extends FileOutputFormat[Text, InstanceSimilarity]
{
    override def getRecordWriter(job : TaskAttemptContext) : RecordWriter[Text, InstanceSimilarity] =
    {
        val conf = job.getConfiguration
        val file = getDefaultWorkFile(job, ".nt")
        val fs = file.getFileSystem(conf)
        val out = fs.create(file, false)

        new LinkWriter(out)
    }

    private class LinkWriter(out : DataOutputStream) extends RecordWriter[Text, InstanceSimilarity]
    {
        private val linkPredicate = Silk.config.resolvePrefix(Silk.linkSpec.linkType)

        override def write(sourceUri : Text, instanceSimilarity : InstanceSimilarity) : Unit =
        {
            val line = sourceUri + " " + linkPredicate + " " + instanceSimilarity.targetUri + ".\n"
            out.write(line.getBytes("UTF-8"))
        }

        override def close(context : TaskAttemptContext) : Unit =
        {
            out.close()
        }
    }
}