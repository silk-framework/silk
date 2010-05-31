package de.fuberlin.wiwiss.silk.linkspec

class LinkSpecification(val linkType : String, val sourceDatasetSpecification : DatasetSpecification,
                        val targetDatasetSpecification : DatasetSpecification, val linkConditions : Aggregation,
                        val acceptThreshold : Double , val verifyThreshold : Double, val linkLimit : LinkLimit,
                        val outputs : Traversable[Output])
{
    
}
