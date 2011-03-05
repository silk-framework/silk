package de.fuberlin.wiwiss.silk.linkspec.condition

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import xml.Elem
import de.fuberlin.wiwiss.silk.linkspec.input.{Transformer, TransformInput, PathInput, Input}

/**
 * A Link Condition specifies the conditions which must hold true so that a link is generated between two instances.
 */
case class LinkCondition(rootOperator : Option[Operator])
{
  /**
   * Computes the similarity between two instances.
   *
   * @param instances The instances to be compared.
   * @param threshold The similarity threshold.
   *
   * @return The similarity as a value between 0.0 and 1.0.
   *         Returns 0.0 if the similarity is lower than the threshold.
   *         None, if no similarity could be computed.
   */
  def apply(instances : SourceTargetPair[Instance], threshold : Double) : Double =
  {
    rootOperator match
    {
      case Some(operator) => operator(instances, threshold).getOrElse(0.0)
      case None => 0.0
    }
  }

  /**
   * Indexes an instance.
   *
   * @param instance The instance to be indexed
   * @param threshold The similarity threshold.
   *
   * @return A set of (multidimensional) indexes. Instances within the threshold will always get the same index.
   */
  def index(instance : Instance, threshold : Double) : Set[Int] =
  {
    rootOperator match
    {
      case Some(operator) =>
      {
        val indexes = operator.index(instance, threshold)

        //Convert the index vectors to scalars
        for(index <- indexes) yield
        {
          (index zip operator.blockCounts).foldLeft(0){case (iLeft, (iRight, blocks)) => iLeft * blocks + iRight}
        }
      }
      case None => Set.empty
    }

  }

  /**
   * The number of blocks in each dimension of the index.
   */
  val blockCount =
  {
    rootOperator match
    {
      case Some(operator) => operator.blockCounts.foldLeft(1)(_ * _)
      case None => 1
    }
  }

  /**
   * Serializes this Link Condition as XML.
   */
  def toXML =
  {
    <LinkCondition>
      { rootOperator.toList.map(serializeOperator) }
    </LinkCondition>
  }

  //TODO move to respective classes
  private def serializeOperator(operator : Operator) : Elem = operator match
  {
    case Aggregation(required, weight, operators, Aggregator(aggregator, params)) =>
    {
      <Aggregate required={required.toString} weight={weight.toString} type={aggregator}>
        { operators.map(serializeOperator) }
      </Aggregate>
    }
    case Comparison(required, weight, inputs, Metric(metric, params)) =>
    {
      <Compare required={required.toString} weight={weight.toString} metric={metric}>
        { serializeInput(inputs.source) }
        { serializeInput(inputs.target) }
        { params.map{case (name, value) => <Param name={name} value={value} />} }
      </Compare>
    }
  }

  //TODO move to input class
  private def serializeInput(param : Input) : Elem = param match
  {
    case PathInput(path) => <Input path={path.toString} />
    case TransformInput(inputs, Transformer(transformer, params)) =>
    {
      <TransformInput function={transformer}>
        { inputs.map{input => serializeInput(input)} }
        { params.map{case (name, value) => <Param name={name} value={value} />} }
      </TransformInput>
    }
  }
}
