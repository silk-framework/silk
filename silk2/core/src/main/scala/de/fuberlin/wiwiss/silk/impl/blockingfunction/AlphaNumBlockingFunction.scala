package de.fuberlin.wiwiss.silk.impl.blockingfunction

import de.fuberlin.wiwiss.silk.linkspec.BlockingFunction

class AlphaNumBlockingFunction(val params : Map[String, String] = Map.empty) extends BlockingFunction
{
    /** The size of the index each character is assigned to */
    private val numIndexes = 37

    def apply(value : String) : Double =
    {
        val indexes = value.map(c => index(c).toDouble / numIndexes)

        indexes.foldRight(0.0)((index, sum) => sum / numIndexes + index)
    }

    /**
     * Assigns a index to a single character according to the following schema:
     *
     * Numbers: 0 - 9
     * Alphabetic character: 10 - 35
     * Others: 36
     */
    private final def index(c : Char) : Int =
    {
        //Distribute numbers to the first 10 indexes
        if(c >= '0' && c <= '9')
        {
            (c - '0')
        }
        //Distribute alphabetic characters to the remaining 24 indexes
        else if(c >= 'a' && c <= 'z')
        {
            10 + (c - 'a')
        }
        //Assign all remaining characters to the last indexes
        else
        {
            36
        }
    }
}