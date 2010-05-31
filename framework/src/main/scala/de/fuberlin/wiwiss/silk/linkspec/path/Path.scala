package de.fuberlin.wiwiss.silk.linkspec.path

import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents an RDF path.
 */
case class Path(variable : String, operators : List[PathOperator])
{
    val id = Path.currentId.getAndIncrement()

    override def toString = operators.mkString

    override def equals(other : Any) = other.isInstanceOf[Path] && toString == other.toString

    override def hashCode = toString.hashCode
}

object Path
{
    private var currentId : AtomicInteger = new AtomicInteger(0)

    def parse(path : String) = PathParser.parse(path)
}
