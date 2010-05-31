package de.fuberlin.wiwiss.silk

import linkspec.path.Path

class Instance(val variable : String, val uri : String, values : Map[Int, Set[String]])
{
    //TODO if this returns a set, multiple equal values will be returned as one which will affect aggregations like the average
    def evaluate(path : Path) : Set[String] = values.get(path.id).getOrElse(Set())

    override def toString = uri + "\n{\n  " + values.values.mkString("\n  ") + "\n}"
}
