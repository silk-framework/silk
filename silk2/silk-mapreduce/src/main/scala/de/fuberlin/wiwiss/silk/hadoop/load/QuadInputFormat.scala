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

package de.fuberlin.wiwiss.silk.hadoop.load

import org.apache.hadoop.mapreduce._
import lib.input.TextInputFormat
import org.apache.hadoop.io.{Text, LongWritable, NullWritable}

class QuadInputFormat extends InputFormat[NullWritable, Quad]
{
  val textInputFormat = new TextInputFormat()

  override def getSplits(jobContext : JobContext) : java.util.List[InputSplit] =
  {
    textInputFormat.getSplits(jobContext)
  }

  override def createRecordReader(inputSplit : InputSplit, context : TaskAttemptContext) : RecordReader[NullWritable, Quad] =
  {
    val lineRecordReader = textInputFormat.createRecordReader(inputSplit, context)
    new QuadRecordReader(lineRecordReader)
  }

  private class QuadRecordReader(lineRecordReader : RecordReader[LongWritable, Text]) extends RecordReader[NullWritable, Quad]
  {
    override def getProgress = lineRecordReader.getProgress

    override def initialize(inputSplit : InputSplit, context : TaskAttemptContext) : Unit = lineRecordReader.initialize(inputSplit, context)

    override def close : Unit = lineRecordReader.close()

    override def nextKeyValue : Boolean =  lineRecordReader.nextKeyValue()

    override def getCurrentKey = NullWritable.get

    override def getCurrentValue = Quad.parse(lineRecordReader.getCurrentValue.toString)
  }
}

