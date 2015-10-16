Hi all,

we are happy to announce version 2.5 of the Silk Link Discovery Framework for the Web of Data.

The Silk framework is a tool for discovering relationships between data items within different Linked Data sources. Data publishers can use Silk to set RDF links from their data sources to other data sources on the Web. Using the declarative Silk – Link Specification Language (Silk-LSL), developers can specify the linkage rules data items must fulfill in order to be interlinked. These linkage rules may combine various similarity metrics and can take the graph around a data item into account, which is addressed using an RDF path language.

Linkage rules can either be written manually or developed using the Silk Workbench. The Silk Workbench, is a web application which guides the user through the process of interlinking different data sources.

Version 2.5 includes the following additions to the last major release 2.4:

(1) Silk Workbench now includes a function to learn linkage rules from the reference links. The learning function is based on genetic programming and capable of learning complex linkage rules. Similar to a genetic algorithm, genetic programming starts with a randomly created population of linkage rules. From that starting point, the algorithm iteratively transforms the population into a population with better linkage rules by applying a number of genetic operators. As soon as either a linkage rule with a full f-Measure has been found or a specified maximum number of iterations is reached, the algorithm stops and the user can select a linkage rule.

(2) A new sampling tab allows for fast creation of the reference link set. It can be used to bootstrap the learning algorithm by generating a number of links which are then rated by the user either as correct or incorrect. In this way positive and negative reference links are defined which in turn can be used to learn a linkage rule. If a previous learning run has already been executed, the sampling tries to generate links which contain features which are not yet covered by the current reference link set.

(2) The new help sidebar provides the user with a general description of the current tab as well as with suggestions for the next steps in the linking process. As new users are usually not familiar with the steps involved in interlinking two data sources, the help sidebar currently provides basic guidance to the user and will be extended in future versions.

(3) Introducing per-comparison thresholds:
- On popular request, thresholds can now be specified on each comparison.
- Backwards-compatible: Link specifications using a global threshold
can still be executed.

(4) New distance measures:
- Jaccard Similarity
- Dice’s coefficient
- DateTime Similarity
- Tokenwise Similarity, contributed by Florian Kleedorfer, Research Studios Austria

(5) New data transformations:
- RemoveEmptyValues
- Tokenizer
- Merge Values of multiple inputs

(6) New DataSources and Outputs
- In addition to reading from SPARQL endpoints, Silk now also supports reading from RDF dumps in all common formats. Currently the data set is held in memory and it is not available in the Workbench yet, but future versions will improve this.
- New SPARQL/Update Output: In addition to writing the links to a file, Silk now also supports writing directly to a triple store using SPARQL/Update.

(7) Various improvements and bugfixes

---------------------------------------------------------------------------------

More information about the Silk Link Discovery Framework is available at:

http://www4.wiwiss.fu-berlin.de/bizer/silk/

The Silk framework is provided under the terms of the Apache License, Version 2.0 and can be downloaded from:

http://www4.wiwiss.fu-berlin.de/bizer/silk/releases/

The development of Silk was supported by Vulcan Inc. as part of its Project Halo (www.projecthalo.com) and by the EU FP7 project LOD2-Creating Knowledge out of Interlinked Data (http://lod2.eu/, Ref. No. 257943).

Thanks to Christian Becker, Michal Murawicki and Andrea Matteini for contributing to the Silk Workbench.

Happy linking,

Robert Isele, Anja Jentzsch and Chris Bizer
