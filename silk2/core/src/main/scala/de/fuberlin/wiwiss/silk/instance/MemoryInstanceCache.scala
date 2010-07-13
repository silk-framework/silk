package de.fuberlin.wiwiss.silk.instance

import collection.mutable.ArrayBuffer

/**
 * An instance cache, which caches the instance in memory and allows adding new instances at runtime.
 */
class MemoryInstanceCache(val blockCount : Int = 1, maxPartitionSize : Int = 1000) extends InstanceCache
{
    private var blocks = IndexedSeq.fill(blockCount)(new Block)

    private var instanceCounter = 0

    /**
     * Writes to this cache.
     * All previous partitions will be deleted.
     */
    override def write(instances : Traversable[Instance], blockingFunction : Instance => Set[Int] = _ => Set(0))
    {
        instanceCounter = 0
        blocks = IndexedSeq.fill(blockCount)(new Block)
        for(instance <- instances)
        {
            add(instance, blockingFunction)
        }
    }

    /**
     * Adds a single instance to the cache.
     */
    def add(instance : Instance, blockingFunction : Instance => Set[Int] = _ => Set(0))
    {
        for(block <- blockingFunction(instance))
        {
            blocks(block).add(instance)
        }
        instanceCounter += 1
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