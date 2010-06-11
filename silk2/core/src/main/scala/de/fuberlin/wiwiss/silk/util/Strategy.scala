package de.fuberlin.wiwiss.silk.util

/**
 * A strategy which can have different implementations.
 */
trait Strategy
{
    val params : Map[String, String]
}
