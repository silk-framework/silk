## Description of the plugin

The plugin `xsltOperator` is a custom task which can be used in a workflow in order to transform a given **XML file** using a **XSL transformation** from a XSLT file. The filename extension of such a XSL transformation is, accordingly, `.xslt`.

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

### How does XSLT work?

Technically, an `.xslt` file is a **template**, XSLT as a W3C specification or recommendation is the **templating language**, and the XSLT processor  is a **template engine**. The template is a file which is written using the templating language, and which is processed by the template engine.

Essentially, this is an example of _pattern matching_: The XSLT template (or "stylesheet") contains **patterns**, which in XSLT parlance are known as "[**template rules**](https://www.w3.org/TR/xslt-30/#dt-template-rule)". The template engine of the processor uses the XML input as source of **items** as well as the XSLT file as source of **patterns**. Each pattern specifies a set of conditions on an item; if they match, then the matching `<xsl:template>` of the XSLT file is applied on the corresponding XML element of the XML data.

The _application_ of the template is specified with `<xsl:apply-templates/>` within the XSLT file. It takes an **XPath expression** in its `select` attribute.

In short, the `<xsl:template>` tells which elements it should `match`, whereas the `<xsl:apply-templates/>` tells which elements it should `select`. Both make use of XPath to address (identify and select) the elements in the XML data.
