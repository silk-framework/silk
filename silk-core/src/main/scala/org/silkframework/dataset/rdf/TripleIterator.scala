package org.silkframework.dataset.rdf

import org.silkframework.runtime.iterator.CloseableIterator

trait TripleIterator extends QuadIterator with CloseableIterator[Triple]
