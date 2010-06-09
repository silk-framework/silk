package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance


class TransformInput(val inputs : Seq[Input], val transformer : Transformer) extends Input
{
    require(inputs.size > 0, "Number of inputs must be > 0.")

    def evaluate(sourceInstance : Instance, targetInstance : Instance) : Traversable[String] =
    {
        val strings = for (input <- inputs) yield input.evaluate(sourceInstance, targetInstance)
        for (sequence <- cartesianProduct(strings)) yield transformer.evaluate(sequence)
    }

    def cartesianProduct(strings : Seq[Traversable[String]]) : Traversable[List[String]] =
    {
        if (strings.tail.isEmpty) for (string <- strings.head) yield string :: Nil
        else for (string <- strings.head; seq <- cartesianProduct(strings.tail)) yield string :: seq
    }
}
