package de.fuberlin.wiwiss.silk.linkspec

import java.io.File
import xml.XML

object ConfigLoader
{
    def load(file : File) =
    {
        val xml = XML.loadFile(file)
    }
}