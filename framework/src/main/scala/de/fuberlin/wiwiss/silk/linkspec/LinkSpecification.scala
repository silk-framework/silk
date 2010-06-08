package de.fuberlin.wiwiss.silk.linkspec

class LinkSpecification(val linkType : String, val sourceDatasetSpecification : DatasetSpecification,
                        val targetDatasetSpecification : DatasetSpecification, val condition : Aggregation,
                        val filter : LinkFilter, val outputs : Traversable[Output])
{
    
}
