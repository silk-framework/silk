

Hi all,

we are happy to announce the second version of the Silk – Link Discovery
Framework for the Web of Data.

The Web of Data is built upon two simple ideas: Employ the RDF data model to
publish structured data on the Web and to set explicit RDF links between
entities within different data sources. While there are more and more tools
available for publishing Linked Data on the Web, there is still a lack of
scalable tools that support data publishers in setting RDF links to other
data sources on the Web.

As the Web of Data is growing quickly, we thought the community would be in
need of an easy-to-use link generation tool which scales to the billion
triples use cases starting to arise on the Web of Data.

Therefore, we ported the Silk – framework from Python to Scala and
redesigned the internal data processing workflow so that Silk can handle
larger linking tasks.

The new Silk 2.0 framework is about 20 times faster than the original
Python-based Silk 0.2 framework. On a Core2 Duo machine with 2GB RAM, Silk
2.0 computes around 180 million comparisons per hour.

Other new features of Silk 2.0 include:

1. A blocking directive which allows users to reduce the number of
comparisons on cost of recall, if necessary.
2. Support of the OAEI Alignment format as additional output format.
3. Slight redesign of the Silk-LSL syntax in order to make the language
easier to use.
4. The Silk-LSL parser produces better error messages which makes debugging
linking specifications less cumbersome.

We will keep on improving Silk and plan to have the next release in August.
This release will include

1. Parallelization based on Hadoop. This will enable Silk to be
deployed on EC2
2. Generating links from a stream of incoming RDF data items. This will
allow Silk to be used in conjunction with Linked Data crawlers like
LDspider.

More information about the Silk framework, the Silk-LSL language
specification, as well as several examples that demonstrate how Silk is used
to set links between different data sources in the LOD cloud is found at

http://www4.wiwiss.fu-berlin.de/bizer/silk/

The Silk framework is provided under the terms of the Apache License,
Version 2.0 and can be downloaded from

http://sourceforge.net/projects/silk2/

The Silk 2.0 – User Manual and Language Reference is found at

http://www4.wiwiss.fu-berlin.de/bizer/silk/spec/

Lots of thanks to

1. Robert Isele and Anja Jentzsch (both Freie Universität Berlin) who
reimplemented Silk in Scala, introduced the blocking features and redesigned
the Silk-LSL language.
2. Julius Volz (Google) who designed and implemented the original Python
version of the Silk Framework.
3. Vulcan Inc. (http://www.vulcan.com/) which enabled us to do this work by
sponsoring Silk as part of its Project Halo (www.projecthalo.com).

Happy linking,

Chris Bizer, Robert Isele and Anja Jentzsch

—
Prof. Dr. Christian Bizer
Web-based Systems Group
Freie Universität Berlin
+49 30 838 55509
http://www.bizer.de
chris@bizer.de
