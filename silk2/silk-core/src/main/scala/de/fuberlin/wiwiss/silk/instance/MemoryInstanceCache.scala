package de.fuberlin.wiwiss.silk.instance

import collection.mutable.ArrayBuffer
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.config.RuntimeConfig

/**
 * An instance cache, which caches the instance in memory and allows adding new instances at runtime.
 */
class MemoryInstanceCache(val instanceSpec: InstanceSpecification, runtimeConfig: RuntimeConfig = RuntimeConfig()) extends InstanceCache {
  private val logger = Logger.getLogger(getClass.getName)

  private var blocks = IndexedSeq.tabulate(blockCount)(new Block(_))

  private var allInstances = Set[String]()

  private var instanceCounter = 0

  @volatile private var writing = false

  /**
   * Writes to this cache.
   */
  override def write(instances: Traversable[Instance], indexFunction: Instance => Set[Int]) {
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
  private def add(instance: Instance, indexFunction: Instance => Set[Int]) {
    if (!allInstances.contains(instance.uri)) {
      val index = if(runtimeConfig.blocking.isEnabled) indexFunction(instance) else Set(0)

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

  override def blockCount: Int = runtimeConfig.blocking.enabledBlocks

  /**
   * The number of partitions in a specific block.
   */
  override def partitionCount(block: Int) = blocks(block).size

  private class Block(block: Int) {
    private val instances = ArrayBuffer(ArrayBuffer[Instance]())
    private val indices = ArrayBuffer(ArrayBuffer[Index]())

    def apply(index: Int) = Partition(instances(index).toArray, indices(index).toArray)

    def add(instance: Instance, index: Index) {
      if (instances.last.size < runtimeConfig.partitionSize) {
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
