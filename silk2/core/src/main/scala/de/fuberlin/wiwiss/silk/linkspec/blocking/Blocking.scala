package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance
import input.PathInput

class Blocking(val inputs : Traversable[PathInput], val blockingFunction : BlockingFunction,
               val blocks : Int = 10, val overlap : Double = 0.0) extends (Instance => Set[Int])
{
    require(blocks > 0, "blocks > 0")
    require(overlap >= 0.0, "overlap >= 0.0")
    require(overlap < 0.5, "overlap < 0.5")

    def apply(instance : Instance) =
    {
        //Find the input which provides the value for the given instance
        val instanceInputs = inputs.filter(_.path.variable == instance.variable)

        if(instanceInputs.isEmpty) throw new IllegalArgumentException("No input found with variable " + instance.variable)

        val values = instanceInputs.flatMap(_(Traversable(instance)))

        values.map(blockingFunction).flatMap(getBlock).toSet
    }

    /**
     * Retrieves the block which corresponds to a specific value.
     */
    private def getBlock(value : Double) : Set[Int] =
    {
        val block = value * blocks
        val blockIndex = block.toInt

        if(block <= 0.5)
        {
            Set(0)
        }
        else if(block >= blocks - 0.5)
        {
            Set(blocks - 1)
        }
        else
        {
            if(block - blockIndex < overlap)
            {
                Set(blockIndex, blockIndex - 1)
            }
            else if(block + 1 - blockIndex < overlap)
            {
                Set(blockIndex, blockIndex + 1)
            }
            else
            {
                Set(blockIndex)
            }
        }
    }
}
