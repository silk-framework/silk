package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.learning.individual.LinkConditionNode

trait IndividualGenerator extends (() => LinkConditionNode)