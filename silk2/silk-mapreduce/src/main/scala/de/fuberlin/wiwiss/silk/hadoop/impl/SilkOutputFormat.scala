package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import java.io.DataOutputStream
import org.apache.hadoop.mapreduce.{TaskAttemptContext, RecordWriter}
import org.apache.hadoop.io.Text
import de.fuberlin.wiwiss.silk.hadoop.SilkConfiguration

class SilkOutputFormat extends FileOutputFormat[Text, EntityConfidence]
{
  override def getRecordWriter(job : TaskAttemptContext) : RecordWriter[Text, EntityConfidence] =
  {
    val config = job.getConfiguration
    val file = getDefaultWorkFile(job, ".nt")
    val fs = file.getFileSystem(config)
    val out = fs.create(file, false)

    new LinkWriter(out, SilkConfiguration.get(job.getConfiguration))
  }

  private class LinkWriter(out : DataOutputStream, config : SilkConfiguration) extends RecordWriter[Text, EntityConfidence]
  {
    override def write(sourceUri : Text, entitySimilarity : EntityConfidence) : Unit =
    {
      val line = "<" + sourceUri + "> <" + config.linkSpec.linkType + "> <" + entitySimilarity.targetUri + "> .\n"
      out.write(line.getBytes("UTF-8"))
    }

    override def close(context : TaskAttemptContext) : Unit =
    {
      out.close()
    }
  }
}
