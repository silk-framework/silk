package de.fuberlin.wiwiss.silk

import datasource.{SparqlBuilder, PathParser}
import util.parsing.input.CharSequenceReader

object Main
{
    def main(args : Array[String])
    {
        val input = new CharSequenceReader("""?artist/dbpedia:director[rdf:type = dbpedia:Album]/rdfs:label[@lang = 'en']""")
        val parser = new PathParser()

        val parseResult = parser.parseAll(parser.path, input)
        
        println(parseResult)

        if(parseResult.successful)
        {
            println("SPARQL: " + SparqlBuilder.build(parseResult.get))
        }
    }
}
