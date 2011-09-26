package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "removeBlanks", label = "Remove blanks", description = "Remove whitespace from a string.")
class RemoveBlanksTransformer() extends ReplaceTransformer(" ", "")
