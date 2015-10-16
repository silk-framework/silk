Hi all,

we are happy to announce version 2.2 of the Silk – Link Discovery Framework for the Web of Data.

The central idea of the Web of Data is to interlink data items using RDF links. However, in practice most data sources are not sufficiently interlinked with related data sources. The Silk Link Discovery Framework addresses this problem by providing tools to generate links between data items based on user-provided link specifications. It can be used by data publishers to generate links between data sets as well as by Linked Data consumers to augment Web data with additional RDF links.

The new Silk 2.2 framework is now provided in three different variants which address different use cases:
1. Silk Single Machine is used to generate RDF links on a single machine. The datasets that should be interlinked can either reside on the same machine or on remote machines which are accessed via the SPARQL protocol. Silk Single Machine provides multithreading and caching. In addition, the performance can be further enhanced using an optional blocking feature.
2. Silk MapReduce is used to generate RDF links between data sets using a cluster of multiple machines. Silk MapReduce is based on Hadoop and can for instance be run on Amazon Elastic MapReduce. Silk MapReduce enables Silk to scale out to very big datasets by distributing the link generation to multiple machines.
3. Silk Server can be used as an identity resolution component within applications that consume Linked Data from the Web. Silk Server provides an HTTP API for matching instances from an incoming stream of RDF data while keeping track of known entities. It can be used for instance together with a Linked Data crawler to populate a local duplicate-free cache with data from the Web.

More information about the Silk framework, the Silk-LSL language specification, as well as several examples that demonstrate how Silk is used to set links between different data sources in the LOD cloud is found at

http://www4.wiwiss.fu-berlin.de/bizer/silk/

The Silk framework is provided under the terms of the Apache License, Version 2.0 and can be downloaded from

http://sourceforge.net/projects/silk2/

The development of Silk was supported by Vulcan Inc. as part of its Project Halo (www.projecthalo.com) and by the EU FP7 project LOD2 – Creating Knowledge out of Interlinked Data (http://lod2.eu/, Ref. No. 257943).

Happy linking,

Robert Isele, Anja Jentzsch and Chris Bizer
