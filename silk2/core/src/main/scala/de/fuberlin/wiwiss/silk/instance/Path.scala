package de.fuberlin.wiwiss.silk.instance

import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents an RDF path.
 */
case class Path(variable : String, operators : List[PathOperator])
{
    /**
     * Unique ID of this path.
     */
    val id = Path.currentId.getAndIncrement()

    /**
     * Serializes this path using the Silk RDF path language.
     */
    override def toString = "?" + variable + operators.mkString

    /**
     * Tests if this path equals another path
     */
    override def equals(other : Any) = other.isInstanceOf[Path] && toString == other.toString

    override def hashCode = toString.hashCode
}

object Path
{
    private var currentId : AtomicInteger = new AtomicInteger(0)

    def parse(path : String) = PathParser.parse(path)
}
