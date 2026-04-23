The SPARQL SELECT plugin is a task for executing SPARQL SELECT queries on an RDF data source.

It can be used in a workflow, connecting an input to an output. The _output_ is an entity table containing the _SPARQL results_ of the query
execution. A [SPARQL 1.1 SELECT](https://www.w3.org/TR/sparql11-query/#select) query is supported; the simplest
example is `SELECT * WHERE { ?s ?p ?o }`.

The _input_ depends on the configuration:

- By default, the query is executed against the connected input, which must be a _SPARQL endpoint_
  (i.e. an RDF dataset).
- When **Use default RDF dataset** (`useDefaultDataset`) is enabled, the query is executed against the project's
  default RDF dataset instead. If the template references input entity properties, the task accepts an entity
  input and generates one query per entity; otherwise it needs no input at all.

The [result limit](https://www.w3.org/TR/sparql11-query/#modResultLimit) can be restricted with the `limit`
parameter. A query timeout (in milliseconds) can be set via `sparqlTimeout`.

As usual, the SPARQL results contain both "variables" and "bindings", such as in
[this example](https://www.w3.org/TR/sparql11-results-json/#json-result-object).
This tabular raw form is transformed into an _entity table_.

### Templating

The select query is rendered by a template engine.
[`Jinja`](https://jinja.palletsprojects.com/) is the default and is described below; for the deprecated `Simple`
and `Velocity Engine` modes, see the "Legacy template engines" section further down.

The following variables are available:

- `input.config.<param>`: a parameter of the connected input task.
- `output.config.<param>`: a parameter of the connected output task.
- `input.entity.<property>`: the value of the given property on the current input entity. Only populated when
  the task is configured to receive input entities (see **Use default RDF dataset** above).
- `project.<key>`: a project-scoped template variable.
- `global.<key>`: a global template variable.

For example, to query the named graph that is configured on the input dataset:

```sparql
SELECT * WHERE { GRAPH <{{ input.config.graph | validate_uri }}> { ?s ?p ?o } }
```

Parameter, property and variable names must be valid Jinja identifiers (`[a-zA-Z_][a-zA-Z0-9_]*`);
bracket-subscript access such as `input.entity["urn:prop:label"]` is not supported.

The `defaultScope` parameter declares one scope whose variables are additionally exposed at the top level of the
template context, so they can be referenced without the scope prefix. It defaults to `input.entity`: a template
may write `{{ property }}` as a shorthand for `{{ input.entity.property }}`, and both forms resolve to the same
value. Set `defaultScope` to the empty string to disable this aliasing and require every variable to be addressed
with its full scope.

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

### Automatic `FROM` clause injection

If the SPARQL source is defined on a specific graph, a `FROM` clause will be added to the query at execution time,
except when there already exists a `GRAPH` or `FROM` clause in the query. `FROM NAMED` clauses are not injected.

### Legacy template engines

In addition to Jinja, two deprecated template engines are supported for backwards compatibility: `Simple`
and [`Velocity Engine`](https://velocity.apache.org/engine/2.4.1/user-guide.html). Their syntax is identical
to the one used by the `SPARQL Update operator` and is documented there.
