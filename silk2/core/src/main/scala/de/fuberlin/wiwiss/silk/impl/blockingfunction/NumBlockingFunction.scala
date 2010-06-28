package de.fuberlin.wiwiss.silk.impl.blockingfunction

import de.fuberlin.wiwiss.silk.linkspec.BlockingFunction

class NumBlockingFunction(val params : Map[String, String] = Map.empty) extends BlockingFunction
{
     def apply(value : String) : Double =
     {
         //Remove leading zeros and convert each digit to a float value
         val digits = value.dropWhile(_ == '0').map(c => if(c.isDigit) c.getNumericValue / 10.0 else 0)

         digits.foldRight(0.0)((digit, sum) => sum / 10.0 + digit)
     }
}