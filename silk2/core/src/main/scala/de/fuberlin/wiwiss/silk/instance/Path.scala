package de.fuberlin.wiwiss.silk.instance

import collection.mutable.{SynchronizedMap, WeakHashMap}
import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents an RDF path.
 */
case class Path(variable : String, operators : List[PathOperator], id : Int = Path.currentId.getAndIncrement())
{
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
    private val pathCache = new WeakHashMap[String, Path]() with SynchronizedMap[String, Path]

    private val currentId = new AtomicInteger(0)

    /**
     * Parses a path string.
     * May return a cached copy.
     */
    def parse(pathStr : String) =
    {
        //Split the path into the variable and the operators
        val variable = pathStr.tail.takeWhile(_ != '/')
        val operators = pathStr.dropWhile(_ != '/')

        //Try to retrieve a cached copy with the same operators. If not found, parse the path
        val path = pathCache.getOrElseUpdate(operators, PathParser.parse(pathStr))

        //Set the variable as the cached copy might use another variable
        path.copy(variable = variable)
    }
}
