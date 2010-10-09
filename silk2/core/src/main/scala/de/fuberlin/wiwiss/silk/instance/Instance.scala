package de.fuberlin.wiwiss.silk.instance

/**
 * A single instance.
 */
@serializable
class Instance(val variable : String, val uri : String, values : Map[Int, Set[String]])
{
    def evaluate(path : Path) : Set[String] = values.get(path.id).getOrElse(Set())

    override def toString = uri + "\n{\n  " + values.values.mkString("\n  ") + "\n}"
}
