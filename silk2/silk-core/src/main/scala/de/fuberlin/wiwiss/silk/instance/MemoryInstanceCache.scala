package de.fuberlin.wiwiss.silk.instance

import collection.mutable.ArrayBuffer
import java.util.logging.Logger

/**
 * An instance cache, which caches the instance in memory and allows adding new instances at runtime.
 */
class MemoryInstanceCache(instanceSpec: InstanceSpecification, val blockCount: Int = 1, maxPartitionSize: Int = 1000) extends InstanceCache {
  private val logger = Logger.getLogger(getClass.getName)

  private var blocks = IndexedSeq.tabulate(blockCount)(new Block(_))

  private var allInstances = Set[String]()

  private var instanceCounter = 0

  @volatile private var writing = false

  /**
   * Writes to this cache.
   */
  override def write(instances: Traversable[Instance], indexFunction: Option[Instance => Set[Int]] = None) {
    val startTime = System.currentTimeMillis()
    writing = true

    try {
      for (instance <- instances) {
        add(instance, indexFunction)
      }

      val time = ((System.currentTimeMillis - startTime) / 1000.0)
      logger.info("Finished writing " + instanceCounter + " instances with type '" + instanceSpec.restrictions + "' in " + time + " seconds")
    }
    finally {
      writing = false
    }
  }

  override def isWriting = writing

  /**
   * Adds a single instance to the cache.
   */
  private def add(instance: Instance, indexFunction: Option[Instance => Set[Int]]) {
    if (!allInstances.contains(instance.uri)) {
      val index = indexFunction.map(f => f(instance)).getOrElse(Set(0))

      for (block <- index.map(_ % blockCount)) {
        blocks(block).add(instance, Index.build(index))
      }
      allInstances += instance.uri
      instanceCounter += 1
    }
  }

  override def clear() {
    instanceCounter = 0
    blocks = IndexedSeq.tabulate(blockCount)(new Block(_))
    allInstances = Set[String]()
  }

  override def close() { }

  def instanceCount = instanceCounter

  /**
   * Reads a partition of a block.
   */
  override def read(block: Int, partition: Int) = blocks(block)(partition)

  /**
   * The number of partitions in a specific block.
   */
  override def partitionCount(block: Int) = blocks(block).size

  private class Block(block: Int) {
    private val instances = ArrayBuffer(ArrayBuffer[Instance]())
    private val indices = ArrayBuffer(ArrayBuffer[Index]())

    def apply(index: Int) = Partition(instances(index).toArray, indices(index).toArray)

    def add(instance: Instance, index: Index) {
      if (instances.last.size < maxPartitionSize) {
        instances.last.append(instance)
        indices.last.append(index)
      }
      else {
        instances.append(ArrayBuffer(instance))
        indices.append(ArrayBuffer(index))
        logger.info("Written partition " + (instances.size - 2) + " of block " + block)
      }
    }

    def size = instances.size
  }

}
