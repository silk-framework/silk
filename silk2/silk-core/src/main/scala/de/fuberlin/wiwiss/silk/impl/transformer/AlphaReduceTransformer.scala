package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "alphaReduce", label = "Alpha reduce", description = "Strips all non-alphabetic characters from a string.")
class AlphaReduceTransformer() extends RegexReplaceTransformer("[^\\pL]+", "")