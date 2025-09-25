The SPARQL UPDATE query plugin is a task for outputting SPARQL UPDATE queries from the input RDF data source.

## Description

The `sparqlUpdateOperator` plugin is an example of a _task_. Notice well that this plugin is **neither** a _RDF task_
nor a _RDF dataset_. This is in contrast to e.g. the `sparqlSelectOperator` and the `sparqlEndpoint`, respectively.

More specifically, this means the following: This plugin does not execute SPARQL queries of any sort, but _generates_
them. It generates [SPARQL Update](https://www.w3.org/TR/sparql11-update/) queries from a templating engine. In order to
_execute_ these queries, we need to connect this task from an input into an output RDF dataset.

### Internal Specifics

In contrast to the SPARQL select operator, no `FROM` clause gets injected into the query.

## Related plugins

As mentioned, this plugin is neither a RDF task nor a RDF dataset. Those two categories of plugins are, nevertheless,
related. Specifically, the RDF dataset plugins such as the `sparqlEndpoint` can be used as data input. Similarly,
possible output datasets could be an **in-memory dataset** or a **Knowledge Graph** such as the one handled by the
`eccencaDataPlatform` plugin, which is  the flagship RDF dataset of
[Corporate Memory](https://eccenca.com/products/enterprise-knowledge-graph-platform-corporate-memory).

