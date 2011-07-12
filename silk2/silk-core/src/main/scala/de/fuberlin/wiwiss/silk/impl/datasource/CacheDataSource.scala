package de.fuberlin.wiwiss.silk.impl.datasource

import de.fuberlin.wiwiss.silk.datasource.DataSource
import java.io.File
import de.fuberlin.wiwiss.silk.instance.{FileInstanceCache, InstanceSpecification, Instance}
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "cache", label = "Cache")
class CacheDataSource(dir : String) extends DataSource
{
  private val file = new File(dir)

  def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String] = Seq.empty) : Traversable[Instance] =
  {
    val instanceCache = new FileInstanceCache(instanceSpec, file)

    new Traversable[Instance]
    {
      override def foreach[U](f : Instance => U)
      {
        for(block <- 0 until instanceCache.blockCount;
            partition <- 0 until instanceCache.partitionCount(block);
            instance <- instanceCache.read(block, partition).instances)
        {
          f(instance)
        }
      }
    }
  }
}