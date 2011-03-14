package de.fuberlin.wiwiss.silk.linkspec.input

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * A TransformInput applies a transformation to input values.
 */
case class TransformInput(inputs : Seq[Input], transformer : Transformer) extends Input
{
  require(inputs.size > 0, "Number of inputs must be > 0.")

  def apply(instances : SourceTargetPair[Instance]) : Traversable[String] =
  {
    val strings = for (input <- inputs) yield input.apply(instances)
    for (sequence <- cartesianProduct(strings)) yield transformer.evaluate(sequence)
  }

  private def cartesianProduct(strings : Seq[Traversable[String]]) : Traversable[List[String]] =
  {
    if (strings.tail.isEmpty) for (string <- strings.head) yield string :: Nil
    else for (string <- strings.head; seq <- cartesianProduct(strings.tail)) yield string :: seq
  }

  override def toString = transformer match
  {
    case Transformer(name, params) => "Transformer(type=" + name + ", params=" + params + ", inputs=" + inputs + ")"
  }

  override def toXML(implicit prefixes : Prefixes) = transformer match
  {
    case Transformer(id, params) =>
    {
      <TransformInput function={id}>
        { inputs.map{input => input.toXML} }
        { params.map{case (name, value) => <Param name={name} value={value} />} }
      </TransformInput>
    }
  }
}
