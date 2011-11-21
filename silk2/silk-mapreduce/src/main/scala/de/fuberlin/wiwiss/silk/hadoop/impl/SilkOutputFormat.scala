/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
