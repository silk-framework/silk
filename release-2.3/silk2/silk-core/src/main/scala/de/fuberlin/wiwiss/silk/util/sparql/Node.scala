package de.fuberlin.wiwiss.silk.util.sparql

sealed trait Node
{
    val value : String
}

case class Resource(value : String) extends Node

case class BlankNode(value : String) extends Node

case class Literal(value : String) extends Node
