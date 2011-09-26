package de.fuberlin.wiwiss.silk.entity

import xml.Elem
import de.fuberlin.wiwiss.silk.config.Prefixes

case class Restriction(operator: Option[Restriction.Operator]) {
  def toXml = {
    <Restriction>
      {for (op <- operator) yield op.toXml}
    </Restriction>
  }
}

object Restriction {
  def empty = new Restriction(None)

  sealed trait Operator {
    def toXml: Elem
  }

  /**
   * A condition which evaluates to true if the provided path contains at least one of the given values.
   */
  case class Condition(path: Path, values: Set[String]) extends Operator {
    def toXml = {
      <Condition path={path.toString}>
        {values.map(v => <Value>
        {v}
      </Value>)}
      </Condition>
    }
  }

  object Condition {
    def resolve(path: Path, values: Set[String])(implicit prefixes: Prefixes) = {
      Condition(path, values.map(prefixes.resolve))
    }
  }

  /**
   * Negates the provided operator.
   * Currently not supported.
   */
  case class Not(op: Operator) {
    def toXml = <Not>
      {op.toXml}
    </Not>
  }

  /**
   * Evaluates to true if all provided operators evaluate to true.
   */
  case class And(children: Traversable[Operator]) extends Operator {
    def toXml = <And>
      {children.map(_.toXml)}
    </And>
  }

  /**
   * Evaluates to true if at least one of the provided operators evaluate to true.
   */
  case class Or(children: Traversable[Operator]) extends Operator {
    def toXml = <Or>
      {children.map(_.toXml)}
    </Or>
  }

}
