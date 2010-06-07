package de.fuberlin.wiwiss.silk.linkspec

class LinkLimit(val max : Int)

object LinkLimit
{
    /**
     * Creates a new Link Limit.
     */
    def apply(max : Int, method : String) : LinkLimit =
    {
        new LinkLimit(max)
    }
}