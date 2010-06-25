package de.fuberlin.wiwiss.silk.output

/**
 * Represents a link between two instances.
 */
class Link(val sourceUri : String, val targetUri : String, val confidence : Double)
{
    override def toString = "<" + sourceUri + ">  <" + targetUri + "> (" + confidence + ")"

    /**
     * Compares two Links for equality.
     * Two Links are considered equal if their source and target URIs match.
     * The confidence is ignored in the comparison.
     */
    override def equals(other : Any) = other match
    {
        case otherLink : Link => otherLink.sourceUri == sourceUri && otherLink.targetUri == targetUri
        case _ => false
    }

    override def hashCode = (sourceUri + targetUri).hashCode
}
