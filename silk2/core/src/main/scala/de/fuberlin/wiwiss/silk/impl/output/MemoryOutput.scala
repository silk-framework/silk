package de.fuberlin.wiwiss.silk.impl.output

import de.fuberlin.wiwiss.silk.output.{Link, Output}

class MemoryOutput(val params : Map[String, String] = Map.empty) extends Output
{
    private var _links = List[Link]()

    def links = _links

    def clear()
    {
        _links = List[Link]()
    }

    override def write(link : Link, predicateUri : String) : Unit =
    {
        _links ::= link
    }
}