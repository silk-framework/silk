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
        val lc = c.toLower

        //Distribute numbers to the first 10 indexes
        if(lc >= '0' && lc <= '9')
        {
            (lc - '0')
        }
        //Distribute alphabetic characters to the subsequent 26 indexes
        else if(lc >= 'a' && lc <= 'z')
        {
            10 + (lc - 'a')
        }
        //Assign all remaining characters to the last index
        else
        {
            36
        }
    }
}