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

package org.silkframework.cache

import org.silkframework.entity.{Entity, EntitySchema, Index}

import scala.xml.Node

/**
 * A cache of entities.
 */
trait EntityCache {
  /**
   * The entity description of the entities in this cache.
   */
  def entitySchema: EntitySchema

  /**
   * The index function according to which the entities are indexed.
   */
  def indexFunction: (Entity => Index)

  /**
   * Writes to this cache.
   */
  def write(entity: Entity): Unit

  /**
   * Reads a partition of a block.
   */
  def read(block: Int, partition: Int): Partition

  /**
    * Reads the complete cache.
    */
  def readAll: Iterable[Entity] = {
    for (block <- ((0 until blockCount).view);
         partition <- 0 until partitionCount(block);
         entity <- read(block, partition).entities) yield {
      entity
    }
  }

  /**
   * Removes all entities from this cache.
   */
  def clear(): Unit

  /**
   * Closes this cache and writes all unwritten entities.
   */
  def close(): Unit

  /**
   *  The number of blocks in this cache.
   */
  def blockCount: Int

  /**
   * The number of partitions in a specific block.
   */
  def partitionCount(block: Int): Int

  /**
    * Total number of entities in this cache.
    */
  def size: Int

  /**
   * Reads entities from XML
   */
  def fromXML(node: Node, entityDesc: EntitySchema): Unit = {
      var currentBlock = 0

      for (blockNode <- node \ "Block") {
        currentBlock = (blockNode \ "@id").text.toInt

        for (partitionNode <- blockNode \ "Partition";
             entityNode <- partitionNode \ "_") {
          write(Entity.fromXML(entityNode, entityDesc))
        }
      }
    }
}