The SPARQL UPDATE query plugin is a task for outputting SPARQL UPDATE queries from the input RDF data source.

## Description

The `sparqlUpdateOperator` plugin is an example of a _task_. Notice well that this plugin is **neither** a _RDF task_
nor a _RDF dataset_. This is in contrast to e.g. the `sparqlSelectOperator` and the `sparqlEndpoint`, respectively.

More specifically, this means the following: This plugin does not execute SPARQL queries of any sort, but _generates_
them. It generates [SPARQL Update](https://www.w3.org/TR/sparql11-update/) queries from a templating engine. In order to
_execute_ these queries, we need to connect this task from an input into an output RDF dataset.

## Templating

The `sparqlUpdateOperator` plugin uses a **template** in order to construct and output SPARQL update queries.
Three template engines are supported: `Jinja` (the default), `Simple`, and
[`Velocity Engine`](https://velocity.apache.org/engine/2.4.1/user-guide.html).
The `Simple` and `Velocity Engine` modes are deprecated.

### Example of the `Jinja` mode

[Jinja](https://jinja.palletsprojects.com/) is the recommended template engine. It uses `{{ }}` for expressions and
`{% %}` for control flow statements such as conditionals.

```
DELETE DATA { <{{ input.entity.subject | validate_uri }}> rdfs:label "{{ input.entity.oldLabel | escape_literal }}" } ;
{% if input.entity.subject %}
  INSERT DATA { <{{ input.entity.subject | validate_uri }}> rdfs:label "{{ input.entity.newLabel | escape_literal }}" } ;
{% endif %}
```

The following variables are available:

- `input.entity.<property>`: the value of the given property on the current input entity.
- `input.config.<param>`: a parameter of the connected input task.
- `output.config.<param>`: a parameter of the connected output task.
- `project.<key>`: a project-scoped template variable.
- `global.<key>`: a global template variable.

Entity property names must be valid Jinja identifiers (`[a-zA-Z_][a-zA-Z0-9_]*`); bracket-subscript access such as
`input.entity["urn:prop:label"]` is not supported.

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

### Example of the `Simple` mode (deprecated)

```
  DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA2"} }
  INSERT DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA3"} }
```

This will insert the URI serialization of the property value `PROP_FROM_ENTITY_SCHEMA1` for the
`${<PROP_FROM_ENTITY_SCHEMA1>}` expression.
Furthermore, it will insert a plain literal serialization for the property values `PROP_FROM_ENTITY_SCHEMA2` and
`PROP_FROM_ENTITY_SCHEMA3` for the template literal expressions.

It is also possible to write something like `${"PROP"}^^<http://someDatatype>`  or `${"PROP"}@en`. In other words, we
can combine variable substitutions with fixed expressions to construct semi-flexible expressions within the template.

### Example of the `Velocity Engine` mode (deprecated)

```
  DELETE DATA { $row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $row.plainLiteral("PROP_FROM_ENTITY_SCHEMA2") }
  #if ( $row.exists("PROP_FROM_ENTITY_SCHEMA1") )
    INSERT DATA { $row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $row.plainLiteral("PROP_FROM_ENTITY_SCHEMA3") }
  #end
```

Input values are accessible via various methods of the `row` variable (used with `$row`):

- `$row.uri(inputPath: String)`: Renders an input value as **URI**. Throws an exception if the value isn't a valid URI.
- `$row.plainLiteral(inputPath: String)`: Renders an input value as **plain literal**, i.e. it escapes problematic characters, etc.
- `$row.rawUnsafe(inputPath: String)`: Renders an input value as is, i.e. **no escaping** is done. This should **only** be used if the input values can be trusted.
- `$row.exists(inputPath: String)`: Returns `true` if a value for the input path **exists**, else `false`.

The methods `uri`, `plainLiteral` and `rawUnsafe` throw an exception if no input value is available for the given input path.

In addition to input values, properties of the input and output tasks can be accessed via the `inputProperties` and
`outputProperties` objects in the same way as the `row` object. For example with `$inputProperties.uri("graph")`.

For more information about the Velocity Engine, visit http://velocity.apache.org.

### Internal Specifics

In contrast to the SPARQL select operator, no `FROM` clause gets injected into the query.

## Related plugins

As mentioned, this plugin is neither a RDF task nor a RDF dataset. Those two categories of plugins are, nevertheless,
related. Specifically, the RDF dataset plugins such as the `sparqlEndpoint` can be used as data input. Similarly,
possible output datasets could be an **in-memory dataset** or a **Knowledge Graph** such as the one handled by the
`eccencaDataPlatform` plugin, which is the flagship RDF dataset of
[Corporate Memory](https://eccenca.com/products/enterprise-knowledge-graph-platform-corporate-memory).
