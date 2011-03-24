package de.fuberlin.wiwiss.silk.instance

import collection.mutable.ArrayBuffer
import java.util.logging.Logger

/**
 * An instance cache, which caches the instance in memory and allows adding new instances at runtime.
 */
class MemoryInstanceCache(instanceSpec : InstanceSpecification, val blockCount : Int = 1, maxPartitionSize : Int = 1000) extends InstanceCache
{
  private val logger = Logger.getLogger(getClass.getName)

  private var blocks = IndexedSeq.fill(blockCount)(new Block)

  private var allInstances = Set[String]()

  private var instanceCounter = 0

  @volatile private var writing = false

  /**
   * Writes to this cache.
   */
  override def write(instances : Traversable[Instance], blockingFunction : Option[Instance => Set[Int]] = None)
  {
    val startTime = System.currentTimeMillis()
    writing = true

    try
    {
      for(instance <- instances)
      {
        add(instance, blockingFunction)
      }

      val time = ((System.currentTimeMillis - startTime) / 1000.0)
      logger.info("Finished writing " + instanceCounter + " instances with type '" + instanceSpec.restrictions + "' in " + time + " seconds")
    }
    finally
    {
      writing = false
    }
  }

  override def isWriting = writing

  /**
   * Adds a single instance to the cache.
   */
  private def add(instance : Instance, blockingFunction : Option[Instance => Set[Int]])
  {
    if(!allInstances.contains(instance.uri))
    {
      for(block <- blockingFunction.map(f => f(instance)).getOrElse(Set(0)))
      {
        blocks(block).add(instance)
      }
      allInstances += instance.uri
      instanceCounter += 1
    }
  }

  override def clear()
  {
    instanceCounter = 0
    blocks = IndexedSeq.fill(blockCount)(new Block)
    allInstances = Set[String]()
  }

  override def close()
  {
  }

  def instanceCount = instanceCounter

  /**
   * Reads a partition of a block.
   */
  override def read(block : Int, partition : Int) = blocks(block)(partition).toArray

  /**
   * The number of partitions in a specific block.
   */
  override def partitionCount(block : Int) = blocks(block).size

  private class Block
  {
    private val partitions = ArrayBuffer(ArrayBuffer[Instance]())

    def apply(index : Int) = partitions(index)

    def add(instance : Instance)
    {
      if(partitions.last.size < maxPartitionSize)
      {
        partitions.last.append(instance)
      }
      else
      {
        partitions.append(ArrayBuffer(instance))
      }
    }

    def size = partitions.size
  }
}
