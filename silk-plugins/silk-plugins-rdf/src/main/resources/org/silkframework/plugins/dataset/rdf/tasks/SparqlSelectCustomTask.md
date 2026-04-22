The SPARQL SELECT plugin is a task for executing SPARQL SELECT queries on the input RDF data source.

## Description

The `sparqlSelectOperator` plugin is an example of a _RDF task_ or _operator_. Such a task can be used in a workflow,
connecting an input to an output. In this specific case, the _input_ is — in essence — a _SPARQL endpoint_ and the
_output_ is the entity table containing the _SPARQL results_ of the SPARQL SELECT query execution.

In general terms, a [SPARQL 1.1 SELECT](https://www.w3.org/TR/sparql11-query/#select) query is supported. One of the
simplest examples is `SELECT * WHERE { ?s ?p ?o }`.

The [result limit](https://www.w3.org/TR/sparql11-query/#modResultLimit) can be specified for the SPARQL SELECT plugin
itself, with the parameter `limit`. Additionally, a timeout can be specified with the parameter `sparqlTimeout`.

As usual, the SPARQL results contain both "variables" and "bindings", such as in
[this example](https://www.w3.org/TR/sparql11-results-json/#json-result-object).
This tabular raw form is transformed into an _entity table_.

### Templating

The select query supports [Jinja](https://jinja.palletsprojects.com/) templating. The following variables are available:

- `input.config.<param>`: a parameter of the connected input task.
- `output.config.<param>`: a parameter of the connected output task.
- `project.<key>`: a project-scoped template variable.
- `global.<key>`: a global template variable.

For example, to query the named graph that is configured on the input dataset:

```sparql
SELECT * WHERE { GRAPH <{{ input.config.graph | validate_uri }}> { ?s ?p ?o } }
```

Parameter and variable names must be valid Jinja identifiers (`[a-zA-Z_][a-zA-Z0-9_]*`); bracket-subscript access
is not supported.

Values are inserted verbatim by default, so URI brackets (`<...>`) and quotation marks around literals must be
written in the template. The following filters are provided to render values safely:

- `validate_uri`: validates that the value is a valid absolute IRI and returns it unchanged. Throws a validation
  error otherwise. Wrap the output in `<...>` in the template.
- `escape_literal`: escapes backslashes, quotes, newlines, carriage returns and tabs so the value can be used
  inside a short-form SPARQL string literal (`"..."` or `'...'`). No enclosing quotes are added.
- `escape_multiline_literal`: escapes backslashes and breaks any run of three or more consecutive single or double
  quotes. Use for values that are wrapped in triple-quoted SPARQL literals (`"""..."""` or `'''...'''`).

All transformer plugins are also available as Jinja filters under their plugin id (for example `lowerCase`,
`trim`, `urlEncode`).

The output schema (i.e. the result variables) is derived from the query at configuration time by evaluating the
template with default values, so the query must remain valid SPARQL regardless of the parameter values.

### Internal Specifics

If the SPARQL source is defined on a specific graph, a `FROM` clause will be added to the query at execution time,
except when there already exists a `GRAPH` or `FROM` clause in the query. `FROM NAMED` clauses are not injected.

## Related plugins

Other types of RDF tasks are the `sparqlCopyOperator` for executing SPARQL CONSTRUCT queries, and the
`sparqlUpdateOperator` for building SPARQL UPDATE queries from a templating engine.

Regarding the input dataset, any RDF dataset is acceptable. For further details on the RDF datasets, see for example the
documentation of the `sparqlEndpoint` plugin.
