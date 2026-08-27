The SPARQL SELECT plugin is a task for executing SPARQL SELECT queries on an RDF data source.
It can be used in a workflow, connecting an input to an output. A
[SPARQL 1.1 SELECT](https://www.w3.org/TR/sparql11-query/#select) query is supported; the simplest example is
`SELECT * WHERE { ?s ?p ?o }`.

## Input and output

The _input_ depends on the configuration:

- By default, the query is executed against the connected input, which must be a _SPARQL endpoint_
  (i.e. an RDF dataset).
- When **Use fallback RDF dataset** (`useDefaultDataset`) is enabled, the query is executed against the
  fallback RDF dataset (as configured in `dataset.defaultRdf`) instead. The input port then depends on what
  the template references:
    - If the template references input entity properties (`input.entity.*`), the task accepts an entity input
      and generates one query per input entity.
    - If it references only parameters of the input task (`input.config.*`), an input connection is still
      required — it supplies the parameter values — but the query is rendered and executed only once.
    - If it references neither, the task has no input port.

The _output_ is an entity table built from the query's
[SPARQL results](https://www.w3.org/TR/sparql11-results-json/#json-result-object): each projected variable becomes
a column, and each result binding becomes a row.

The [result size](https://www.w3.org/TR/sparql11-query/#modResultLimit) can be capped with the `limit` parameter,
and a query timeout (in milliseconds) can be set via `sparqlTimeout`.

## Automatic `FROM` clause injection

If the SPARQL source is defined on a specific graph, a `FROM` clause will be added to the query at execution time,
except when there already exists a `GRAPH` or `FROM` clause in the query. `FROM NAMED` clauses are not injected.

## Templating

The select query is rendered by a template engine before execution.
[`Jinja`](https://jinja.palletsprojects.com/) is the default and is described below; for the deprecated `Simple`
and `Velocity Engine` modes, see "Legacy template engines" at the end.

Jinja uses `{{ ... }}` for value expressions and `{% ... %}` for control flow such as conditionals.

### Template variables

The following variables are available:

- `input.config.<param>`: a parameter of the task connected to the input port. `<param>` is a parameter id
  of that task's plugin, e.g. `graph` on a SPARQL dataset.
- `output.config.<param>`: a parameter of the task the output is connected to.
- `input.entity.<property>`: the value(s) of the given property of the current input entity. Only available
  with **Use fallback RDF dataset** enabled, since only then the task receives input entities
  (see _Input and output_ above).
- `project.<key>`: a project-scoped template variable.
- `global.<key>`: a global template variable.

A single-valued entity property is inserted as a plain string. A multi-valued property can be iterated with
`{% for value in input.entity.<property> %}`; inserting it directly concatenates all values without a
separator. Referencing a variable that is not available at execution time — an unknown parameter name or an
entity property without a value — fails the query generation with an error.

Parameter, property and variable names must be valid Jinja identifiers (`[a-zA-Z_][a-zA-Z0-9_]*`);
bracket-subscript access such as `input.entity["urn:prop:label"]` is not supported.

For example, to query the named graph that is configured on the input dataset:

```sparql
SELECT * WHERE { GRAPH <{{ input.config.graph | validate_uri }}> { ?s ?p ?o } }
```

### Default scope

The `defaultScope` parameter declares one scope whose variables are additionally exposed at the top level of the
template context, so they can be referenced without the scope prefix. It defaults to `input.entity`, which means
a template may write `{{ property }}` as a shorthand for `{{ input.entity.property }}`:

```
{{ property }}   ≡   {{ input.entity.property }}
```

Both forms resolve to the same value. Set `defaultScope` to the empty string to disable this aliasing and require
every variable to be addressed with its full scope.

### Filters

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

### Input schema inference

The input schema (the entity properties the task expects) is derived by scanning the raw template for
`input.entity.<property>` references (or bare references resolved via `defaultScope`). This scan operates
on the template text before rendering, so SPARQL line comments (`# ...`) are **not** stripped: a
commented-out line such as

```sparql
# {{ input.entity.property }}
```

will still cause `property` to appear in the inferred input schema.

### Output schema inference

The output schema is derived from the raw template by a heuristic, without rendering it. The heuristic first
blanks out line comments, Jinja comments, string literals, IRIs and Jinja tags, so that nothing inside them is
mistaken for query structure. It then takes the projection between `SELECT` and the first `WHERE`, `FROM` or `{`
outside of parentheses (a variable named like a keyword, e.g. `?from`, is not a boundary), drops a leading
`DISTINCT` / `REDUCED`, and then:

- For `SELECT *`, collects every distinct `?var` token in the query, unless a sub-select, `EXISTS` or `MINUS`
  could bind variables that are not part of the result.
- Otherwise, collects each top-level `?var` and the trailing `AS ?alias` from parenthesised expressions
  (e.g. `(COUNT(?s) AS ?count)` yields `count`).

Each variable becomes a string-typed path. If no variables can be detected, or a Jinja tag could change the set
of result variables (a block tag before the query, a tag inside the projection, or, for `SELECT *`, a tag anywhere after
the projection that is not inside an IRI or string literal), the output port is reported with an unknown schema and the
schema is taken from the actual query results at execution time. The heuristic need not be complete but must not be
wrong, so whenever it is in doubt (for example on unbalanced parentheses) it reports an unknown schema as well.

### Validation

At task creation, the Jinja template is checked against the available template variables:

- Every `project.<...>` or `global.<...>` reference must resolve to a known variable, matched on the full
  scoped name (so e.g. `project.metaData.label` is looked up at that exact scope).
- Every `input.<...>` or `output.<...>` reference must use `config` or `entity` as its second segment.

Bare references are resolved through `defaultScope` before applying the same rules. The template is not
rendered and the resulting SPARQL is not parsed.

### Legacy template engines

In addition to Jinja, two deprecated template engines are supported for backwards compatibility: `Simple`
and [`Velocity Engine`](https://velocity.apache.org/engine/2.4.1/user-guide.html). Their syntax is identical
to the one used by the `SPARQL Update operator` and is documented there.
