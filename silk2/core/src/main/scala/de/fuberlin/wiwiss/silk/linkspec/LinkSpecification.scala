package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.output.Output

class LinkSpecification(val linkType : String, val sourceDatasetSpecification : DatasetSpecification,
                        val targetDatasetSpecification : DatasetSpecification, val blocking : Option[Blocking], val condition : Aggregation,
                        val filter : LinkFilter, val outputs : Traversable[Output])
{
    override def toString = "LinkSpecification(linkType=" + linkType + ", source=" + sourceDatasetSpecification +
        ", target=" + targetDatasetSpecification +  ", blocking=" + blocking + ", condition=" + condition +
        ", filter=" + filter + ", output=" + outputs + ")"
}
