package de.fuberlin.wiwiss.silk.cache

import xml.Node
import de.fuberlin.wiwiss.silk.entity.{Index, Entity, EntityDescription}

/**
 * A cache of entities.
 */
trait EntityCache {
  /**
   * The entity description of the entities in this cache.
   */
  def entityDesc: EntityDescription

  /**
   * The index function according to which the entities are indexed.
   */
  def indexFunction: (Entity => Index)

  /**
   * Writes to this cache.
   */
  def write(entities: Traversable[Entity])

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
  def readAll = new Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      for (block <- 0 until blockCount;
           partition <- 0 until partitionCount(block);
           entity <- read(block, partition).entities) {
        f(entity)
      }
    }
  }

  /**
   * Removes all entities from this cache.
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
    <EntityCache>
      {for (block <- 0 until blockCount) yield {
      <Block id={block.toString}>
        {for (partition <- 0 until partitionCount(block)) yield {
        <Partition>
          {for (entity <- read(block, partition).entities) yield entity.toXML}
        </Partition>
      }}
      </Block>
    }}
    </EntityCache>
  }

  /**
   * Reads entities from XML
   */
  def fromXML(node: Node, entityDesc: EntityDescription) {
    val entities = new Traversable[Entity] {
      var currentBlock = 0

      override def foreach[U](f: Entity => U) {
        for (blockNode <- node \ "Block") {
          currentBlock = (blockNode \ "@id" text).toInt

          for (partitionNode <- blockNode \ "Partition";
               entityNode <- partitionNode \ "_") {
            f(Entity.fromXML(entityNode, entityDesc))
          }
        }
      }
    }

    write(entities)
  }
}