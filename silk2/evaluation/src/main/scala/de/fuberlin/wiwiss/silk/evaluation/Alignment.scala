package de.fuberlin.wiwiss.silk.evaluation

/**
 * Represents an alignment between two instances
 */
class Alignment(val sourceUri : String, val targetUri : String, val confidence : Double)
{
    override def toString = "[" + sourceUri + ", " + targetUri + ", " + confidence + "]"

    /**
     * Compares two Alignments for equality.
     * Two Alignments are considered equal if their source and target URIs match.
     * The confidence is ignored in the comparison.
     */
    override def equals(other : Any) = other match
    {
        case otherAlignment : Alignment => otherAlignment.sourceUri == sourceUri && otherAlignment.targetUri == targetUri
        case _ => false
    }

    override def hashCode = (sourceUri + targetUri).hashCode
}
