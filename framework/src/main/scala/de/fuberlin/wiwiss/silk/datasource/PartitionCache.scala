package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.Instance

trait PartitionCache
{
    /**
     * The number of partitions in this cache.
     * If isWriting is true, this number is not final.
     */
    def size : Int

    /**
     * True, if this cache is currently being written to.
     */
    def isWriting : Boolean

    /**
     * Retrieves a partition.
     */
    def apply(index : Int) : Array[Instance]

    /**
     * Writes to this cache.
     * All previous partitions will be deleted.
     */
    def write(instances : Traversable[Instance])
}
