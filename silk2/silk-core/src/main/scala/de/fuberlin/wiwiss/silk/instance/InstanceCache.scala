package de.fuberlin.wiwiss.silk.instance

import xml.Node

/**
 * A cache of instances.
 */
trait InstanceCache {
  /**
   * The instance specification of the instances in this cache.
   */
  def instanceSpec: InstanceSpecification

  /**
   * Writes to this cache.
   */
  def write(instances: Traversable[Instance], indexFunction: Instance => Set[Int])

  /**
   * True, if the cache is being written at the moment.
   */
  def isWriting: Boolean

  /**
   * Reads a partition of a block.
   */
  def read(block: Int, partition: Int): Partition

  /**
   * Reads the complete cache.
   */
  def readAll = new Traversable[Instance] {
    def foreach[U](f: Instance => U) {
      for (block <- 0 until blockCount;
           partition <- 0 until partitionCount(block);
           instance <- read(block, partition).instances) {
        f(instance)
      }
    }
  }

  /**
   * Removes all instances from this cache.
   */
  def clear()

  def close()

  /**
   *  The number of blocks in this cache.
   */
  def blockCount: Int

  /**
   * The number of partitions in a specific block.
   */
  def partitionCount(block: Int): Int

  /**
   * Serializes the complete Cache as XML
   */
  def toXML = {
    <InstanceCache>
      {for (block <- 0 until blockCount) yield {
      <Block id={block.toString}>
        {for (partition <- 0 until partitionCount(block)) yield {
        <Partition>
          {for (instance <- read(block, partition).instances) yield instance.toXML}
        </Partition>
      }}
      </Block>
    }}
    </InstanceCache>
  }

  /**
   * Reads instances from XML
   */
  def fromXML(node: Node, instanceSpec: InstanceSpecification) {
    val instances = new Traversable[Instance] {
      var currentBlock = 0

      override def foreach[U](f: Instance => U) {
        for (blockNode <- node \ "Block") {
          currentBlock = (blockNode \ "@id" text).toInt

          for (partitionNode <- blockNode \ "Partition";
               instanceNode <- partitionNode \ "_") {
            f(Instance.fromXML(instanceNode, instanceSpec))
          }
        }
      }
    }

    write(instances, _ => Set(instances.currentBlock))
  }
}