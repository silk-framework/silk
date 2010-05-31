package de.fuberlin.wiwiss.silk

import linkspec.path.Path

class Instance(val uri : String, values : Map[Int, Set[String]])
{
    def evaluate(path : Path) : Set[String] = values.get(path.id).getOrElse(Set())

    override def toString = "Instance " + uri + ": " + values
}
