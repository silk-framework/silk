package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "removeSpecialChars", label = "Remove special chars", description = "Remove special characters (including punctuation) from a string.")
class RemoveSpecialCharsTransformer() extends RegexReplaceTransformer("[^\\d\\pL\\w]+", "")