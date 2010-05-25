package de.fuberlin.wiwiss.silk

import datasource.PathParser
import util.parsing.input.CharSequenceReader

object Main
{
    def main(args : Array[String])
    {
        val input = new CharSequenceReader("?movie")

        val parseResult = new PathParser().path(input)
        
        println(parseResult)
    }
}