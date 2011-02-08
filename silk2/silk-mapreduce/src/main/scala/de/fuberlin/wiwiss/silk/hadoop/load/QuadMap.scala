package de.fuberlin.wiwiss.silk.hadoop.load

import org.apache.hadoop.mapreduce.Mapper
import org.apache.hadoop.io.{Text, NullWritable}

class QuadMap extends Mapper[NullWritable, Quad, Text, Quad]
{
  protected override def map(key : NullWritable, quad : Quad,
                             context : Mapper[NullWritable, Quad, Text, Quad]#Context)
  {

  }
}