The SPARQL endpoint plugin is a dataset for connecting to an existing, remote SPARQL endpoint.

## Description

The `sparqlEndpoint` plugin is an example of a _RDF dataset_. A **dataset** is a collection of data to be read from or
written into, both a _source_ and a _sink_ of information. A **RDF dataset** is a dataset that can deal with RDF data.
There are _several_ plugins for dealing with RDF datasets in the BUILD stage, depending on where the RDF dataset is
located and how it is being accessed. In this case, the `sparqlEndpoint` dataset is a plugin that can access a _remote_
SPARQL endpoint such as one of those [listed by the W3C](https://www.w3.org/wiki/SparqlEndpoints), e.g. Wikidata or
DBpedia.

The **SPARQL dataset** (this plugin) is an instance of a **RDF dataset**. All RDF datasets provide the abstraction and
functionality of a **SPARQL endpoint**. The SPARQL endpoint used in this plugin is a **remote** SPARQL endpoint. It can
handle and execute SPARQL [SELECT](https://www.w3.org/TR/rdf-sparql-query/#select),
[ASK](https://www.w3.org/TR/rdf-sparql-query/#ask) and [CONSTRUCT](https://www.w3.org/TR/rdf-sparql-query/#construct)
queries. Additionally, it can execute [updates](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#updateLanguage).
