package de.fuberlin.wiwiss.silk.linkspec.blocking

import de.fuberlin.wiwiss.silk.linkspec.BlockingFunction

class AlphaNumBlockingFunction(val params : Map[String, String] = Map.empty) extends BlockingFunction
{
     def apply(value : String) : Double =
     {
         value.headOption.map(_.toLower) match
         {
             case Some(c) =>
             {
                 //Distribute numbers to the first quarter
                 if(c >= '0' && c <= '9')
                 {
                     (c - '0').toDouble / ('9' - '0') * 0.25
                 }
                 //Distribute letters to the remaining three quarters
                 else if(c >= 'a' && c <= 'z')
                 {
                     (c - 'a').toDouble / ('z' - 'a') * 0.75 + 0.25
                 }
                 //Asign all remaining characters to the last block
                 else
                 {
                     1.0
                 }
             }
             case None => 0.0
         }
     }
}
