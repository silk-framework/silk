package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "removeBlanks", label = "Remove blanks", description = "Remove whitespace from a string.")
class RemoveBlanksTransformer() extends ReplaceTransformer(" ", "")
