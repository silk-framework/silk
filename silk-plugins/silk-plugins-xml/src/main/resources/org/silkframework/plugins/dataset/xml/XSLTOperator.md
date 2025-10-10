## Description of the plugin

The plugin `xsltOperator` is a custom task which can be used in a workflow in order to transform a given **XML file** using an **XSL transformation** from an XSLT file. The filename extension of such a XSL transformation is, accordingly, `.xslt`.

The **output** of the XML transformation is saved as an output file resource. In practice, the output will often be another XML file (i.e. an "XML to XML transformation"), but this need not be the case.

In essence and from a technical point of view, the `xsltOperator` is simply a wrapper around the XSLT processor provided by [Saxonica](https://www.saxonica.com/products/products.xml).

If you are well-versed in the XSL ecosystem, this is everything you need to know. If not, the remaining of the documentation provides some amount of information and detail on the parts of XSL and XSLT which are relevant for our purposes.
## Description of XSL and XSLT

### The XSL ecosystem

The acronym **XSL** stands for "eXtensible Stylesheet Language". XSL is not a single technology or specification, but a _family of languages_ for processing (transforming) and rendering (presenting) XML documents. It consists of three parts:

	1. XSLT: XSL Transformations
	2. XPath: XML Path Language
	3. XSL-FO: XSL Formatting Objects

In a nutshell, this is simply the separation of concerns between "processing" XML and "rendering" the results.

The most relevant of these parts for us, is **XSLT**. XSLT is a language for *transforming* or *processing* XML documents. Originally (around 1999), XSLT was designed for _styling_ XML documents, which is still seen in the nomenclature, e.g. in the term `<xsl:stylesheet>` or in the acronyms "XSL" and "XSLT" themselves. But other than just _styling XML markup_, XSLT 2.0 (and beyond) is a Turing-complete language, which is used for **transforming XML _data_**. The modern perspective and understanding is therefore not on "XML as a markup language for documents which are presented to a web browser, or converted to a form suitable for printing, such as PDF or PostScript", but, in general terms, on "XML as a means to represent (highly-structured) data, which can be arbitrarily transformed by XSLT".

_If_ the aspect of formatting semantics is relevant to us, then we'd need to consider and describe the XSL Formatting Objects (**XSL-FO**) vocabulary. This is beyond the scope of this document. Our focus is on the transformation of XML data via XSL transformations.

**XPath** is an _expression language_ for finding and accessing to parts (elements) of an XML document. XPath is used by XSLT (finding elements is an obvious part of transforming them), but not exclusively: it is also used by XQuery, a query language for XML documents stored e.g. in XML databases, as well as from several programming languages, in their libraries for processing XML files and data streams.

## XSL Transformations (**XSLT**)

### What is XSLT?

As mentioned, XSLT is a general purpose XML processing language, whose use-cases and applications exceed the originally intended aspect of styling XML files as preparation for a XSL formatter.

The **XSL transform** turns the so-called **source tree** into a **result tree** (see [here](https://www.w3.org/TR/xslt20/#terminology) for further details on the terminology). The _source tree_ is simply the original XML file or source of XML data provided as input of the XSL transformation, whereas the _result tree_ is (part of) the output of the XSL transformation, usually in the form of a `<xsl:result-document>` element, an instance of a _**final** result tree_. Next to final result trees, there are _**temporary** trees_ meant for holding intermediate results during a transformation, like in the case of `<xsl:variable>` elements used in a two-phase transformation. The _structure_ of these trees is specified by and described in the [XQuery and XPath Data Model](https://www.w3.org/TR/xpath-datamodel/), the **XDM**. Conceptually important is the fact that this is a data model which is (a) independent of XSL and XSLT, and (b) shared across XSLT, XPath and XQuery. In other words: it is yet another component of the _XSL ecosystem_, with its own W3C recommendation.

### How does XSLT look like?

#### Minimal example

A minimal example of the (1) XML input data, (2) a corresponding XSL transformation and the (3) generated output is the following:

**XML data** (`.xml` file):
```xml
<book><title>1984</title></book>
```

**XSL stylesheet** (`.xslt` file):
```xml
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:template match="/book">
    <html><body><h1><xsl:value-of select="title"/></h1></body></html>
  </xsl:template>
</xsl:stylesheet>
```

**Output:** (`.html` file)
```html
<html><body><h1>1984</h1></body></html>
```

In this example:
1. The **XML** holds the input information (`<book><title>1984</title></book>`).
2. The **XSL stylesheet** specifies how that information should be formatted (it takes the title and places it inside HTML).
3. The **XSL transformation** processes both files to create the **final HTML result** (`<h1>1984</h1>`).

#### Generic basic example

A slightly more complex example is the following:

**XML data** (`.xml` file):
```xml
<person><name>Alice</name><age>30</age></person>
```

**XSL stylesheet** (`.xslt` file):
```xml
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:template match="/person">
    <html><body><p><xsl:value-of select="name"/> is <xsl:value-of select="age"/> years old.</p></body></html>
  </xsl:template>
</xsl:stylesheet>
```

**Output:** (`.html` file)
```html
<html><body><p>Alice is 30 years old.</p></body></html>
```

#### Simple XML to RDF conversion example

##### XML style

A rather straightforward but specific example could be the following _conversion from XML data to **RDF**:

**XML**:
```xml
<person id="p1">
  <name>Alice</name>
  <email>alice@example.org</email>
</person>
```

**XSL stylesheet**:
```xml
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:template match="/person">
    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
             xmlns:foaf="http://xmlns.com/foaf/0.1/">
      <foaf:Person rdf:about="#{@id}">
        <foaf:name><xsl:value-of select="name"/></foaf:name>
        <foaf:mbox rdf:resource="mailto:{email}"/>
      </foaf:Person>
    </rdf:RDF>
  </xsl:template>
</xsl:stylesheet>
```

**Result (RDF/XML)**:
```xml
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:foaf="http://xmlns.com/foaf/0.1/">
  <foaf:Person rdf:about="#p1">
    <foaf:name>Alice</foaf:name>
    <foaf:mbox rdf:resource="mailto:alice@example.org"/>
  </foaf:Person>
</rdf:RDF>
```

In this example:
1. The **XSLT processor** matches the `<person>` element in the input XML.
2. It **creates** an `<rdf:RDF>` root element with the required namespaces.
3. It **constructs** a `<foaf:Person>` resource, using the `id` attribute as the subject URI.
4. It **writes** a `<foaf:name>` element with the value from `<name>`.
5. It **adds** a `<foaf:mbox>` element with a `mailto:` URI built from `<email>`.
6. It **outputs** the final RDF/XML document representing the RDF triples.
##### JSON style

The same straightforward example of an XML to RDF conversion, but using the `JSON-LD` style for the result:

**XSL stylesheet**
```xml
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text" />
  <xsl:template match="/person">
    {
      "@id": "#<xsl:value-of select='@id'/>",
      "@type": "foaf:Person",
      "foaf:name": "<xsl:value-of select='name'/>",
      "foaf:mbox": "mailto:<xsl:value-of select='email'/>"
    }
  </xsl:template>
</xsl:stylesheet>
```

**Result (JSON-LD style)**:
```json
{
  "@id": "#p1",
  "@type": "foaf:Person",
  "foaf:name": "Alice",
  "foaf:mbox": "mailto:alice@example.org"
}
```

This example gives the gist of converting structured XML datasets into RDF for use in Semantic Web applications or triple stores.

### How does XSLT work?

Technically, an `.xslt` file is a **template**, XSLT as a W3C specification or recommendation is the **templating language**, and the XSLT processor  is a **template engine**. The template is a file which is written using the templating language, and which is processed by the template engine.

Essentially, this is an example of _pattern matching_: The XSLT template (or "stylesheet") contains **patterns**, which in XSLT parlance are known as "[**template rules**](https://www.w3.org/TR/xslt-30/#dt-template-rule)". The template engine of the processor uses the XML input as source of **items** as well as the XSLT file as source of **patterns**. Each pattern specifies a set of conditions on an item; if they match, then the matching `<xsl:template>` of the XSLT file is applied on the corresponding XML element of the XML data.

The _application_ of the template is specified with `<xsl:apply-templates/>` within the XSLT file. It takes an **XPath expression** in its `select` attribute.

In short, the `<xsl:template>` tells which elements it should `match`, whereas the `<xsl:apply-templates/>` tells which elements it should `select`. Both make use of XPath to address (identify and select) the elements in the XML data.

### What is XSLT used for?

**XSLT** is used to transform XML into various output formats, such as:

- **HTML/CSS** – for web pages and styled content.
- **RDF/XML or JSON-LD** – for Semantic Web data.
- **XSL-FO → PDF** – for high-quality printed or digital documents.
- **TeX/LaTeX** – for scientific publishing workflows.
- **Markdown** – for lightweight documentation.
- **EPUB** – for generating e-books (often via intermediate XHTML).
- **SVG** – for generating vector graphics or data visualizations directly from XML.

More conceptually, other user cases of XSLT include:

- **Data migration and integration** — converting legacy XML to modern schemas or RDF.
- **Web services** — transforming XML-based APIs into HTML or JSON.
- **Content publishing** — generating multiple output formats (HTML, PDF, EPUB) from a single XML source.
- **Configuration and code generation** — producing scripts, configuration files, or documentation from XML metadata.
- **Visualization** — creating SVG charts, diagrams, or interactive web elements from structured XML data.
## How does XSL relate to RDF?

**XSL**, specifically **XSLT**, relates to **RDF** by providing a way to **transform XML data into RDF serializations** (like RDF/XML or JSON-LD). It acts as a bridge between structured XML sources and Semantic Web representations, enabling automated RDF generation from existing XML-based datasets.

## How do you import XML into a Knowledge Graph?

To import XML into a **Knowledge Graph**, you typically:

1. **Define a mapping** — decide how XML elements/attributes correspond to RDF classes and properties (using vocabularies like FOAF, schema.org, etc.).
2. **Transform the XML** — use **XSLT**, **RML**, or **XSPARQL** to convert XML into RDF (e.g., RDF/XML, Turtle, or JSON-LD).
3. **Validate the RDF** — check it against the ontology or SHACL shapes.
4. **Load the RDF** — import it into a **triple store** or **graph database** (e.g., Fuseki, GraphDB, Neo4j).

Whereas this process *does* the job of importing XML into a knowledge graph, using a **data integration** solution based on **knowledge graphs**, such as [eccenca Corporate Memory](https://eccenca.com/products/enterprise-knowledge-graph-platform-corporate-memory), is a *much better fit*. For an example on this, see the [tutorial on lifting data from an XML source](https://documentation.eccenca.com/latest/build/lift-data-from-json-and-xml-sources/) (CMEM), and notice how each of the steps (mapping, transforming, validating, loading) is realized. In such an improved setting, notice how the `xsltOperator` plugin  is **_not** used for transforming XML into RDF_, but only for _transforming the data you want to import and bring it to the XML format_. The second step of the list (_transform the XML into RDF_) is taken care of by CMEM itself. The usage of XSLT is, therefore, limited to what is required by your input data and data processing requirements, not by the technicalities behind the semantic data integration.

Notice as well that the source of XML data does not need to be an XML _file_. An alternative could be a Web API providing XML instead of JSON responses. See the [tutorial on extracting data from a Web API](https://documentation.eccenca.com/latest/build/extracting-data-from-a-web-api/), and use an **XML parser** and **XML Dataset** instead of the JSON variants described in the tutorial. Otherwise, the process is the same.
