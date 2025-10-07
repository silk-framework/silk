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

**XPath** is an _expression language_ for finding and accessing to parts (elements) of an XML document. XPath is used by XSLT (finding elements is an obvious part of transforming them), but not exclusively: it is also used by XQuery (a query language for XML documents stored e.g. in XML databases) as well as from several programming languages (in their libraries for processing XML files and data streams).
