The SPARQL SELECT plugin is a task for executing SPARQL SELECT queries on the input RDF data source.

## Description

The `sparqlSelectOperator` plugin is an example of a _RDF task_ or _operator_. Such a task can be used in a workflow,
connecting an input to an output. In this specific case, the _input_ is a _SPARQL endpoint_ and the _output_ is the
entity table containing the _SPARQL results_ of the SPARQL SELECT query execution.

In general terms, a [SPARQL 1.1 SELECT](https://www.w3.org/TR/sparql11-query/#select) query is supported. One of the
simplest examples is `SELECT * WHERE { ?s ?p ?o }`.

The [result limit](https://www.w3.org/TR/sparql11-query/#modResultLimit) can be specified for the SPARQL SELECT plugin
itself, with the parameter `limit`. Additionally, a timeout can be specified with the parameter `sparqlTimeout`.

As usual, the SPARQL results contain both "variables" and "bindings", such as in
[this example](https://www.w3.org/TR/sparql11-results-json/#json-result-object).
This tabular raw form is transformed into an _entity table_.

### Internal Specifics

If the SPARQL source is defined on a specific graph, a `FROM` clause will be added to the query at execution time,
except when there already exists a `GRAPH` or `FROM` clause in the query. `FROM NAMED` clauses are not injected.

## Related plugins

Other types of RDF tasks are the `sparqlCopyOperator` for executing SPARQL CONSTRUCT queries, and the
`sparqlUpdateOperator` for building SPARQL UPDATE queries from a templating engine.

Regarding the input dataset, any RDF dataset is acceptable. For further details on the RDF datasets, see for example the
documentation of the `sparqlEndpoint` plugin.
