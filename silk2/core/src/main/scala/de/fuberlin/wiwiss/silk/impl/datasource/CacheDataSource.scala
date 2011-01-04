package de.fuberlin.wiwiss.silk.impl.datasource

import de.fuberlin.wiwiss.silk.datasource.DataSource
import java.io.File
import de.fuberlin.wiwiss.silk.instance.{FileInstanceCache, InstanceSpecification, Instance}

class CacheDataSource(val params : Map[String, String]) extends DataSource
{
  val dir = new File(readRequiredParam("dir"))

  def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String] = Seq.empty) : Traversable[Instance] =
  {
    val instanceCache = new FileInstanceCache(instanceSpec, dir)

    new Traversable[Instance]
    {
      override def foreach[U](f : Instance => U)
      {
        for(block <- 0 until instanceCache.blockCount;
            partition <- 0 until instanceCache.partitionCount(block);
            instance <- instanceCache.read(block, partition))
        {
          f(instance)
        }
      }
    }
  }
}