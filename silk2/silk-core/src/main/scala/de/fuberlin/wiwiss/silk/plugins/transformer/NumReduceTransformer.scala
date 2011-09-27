package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "numReduce", label = "Numeric reduce", description = "Strip all non-numeric characters from a string.")
class NumReduceTransformer() extends RegexReplaceTransformer("[^0-9]+", "")
