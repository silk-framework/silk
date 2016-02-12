Hi all,

we are happy to announce version 2.7 of the Silk Linked Data Integration Framework.

Silk is an open source framework for integrating heterogeneous data sources. The primary uses cases of Silk include:

- Generating links between related data items within different Linked Data sources.
- Linked Data publishers can use Silk to set RDF links from their data sources to other data sources on the Web.
- Applying data transformations to structured data sources.

Silk is based on the Linked Data paradigm, which is built on two simple ideas: First, RDF provides an expressive data model for representing structured information. Second, RDF links are set between entities in different data sources. 

What’s new:

- Panayiotis Smeros (National and Kapodistrian University of Athens) contributed plugins for a wide variety of spatial and temporal relations. Specifically, the spatial relations that were introduced derive from the Dimensionally Extended 9-Intersection, the Egenhofer’s and the OGC Simple Features Models and the Region Connection Calculus (e.g., Intersects, Touches, Contains etc.) whereas the temporal relations are based on the Allen’s Interval Calculus (e.g., Before, After, During etc.). Also, by pairing a spatial and a temporal relation, Silk is able to discover spatiotemporal relations between spatial objects that change over time or between moving objects.

- Many improvements to Silk Workbench.

- Silk packages are now located in the package org.silkframework

The Silk framework is provided under the terms of the Apache License, Version 2.0. More information about the Silk Link Discovery Framework is available at: 

http://silkframework.org/

Commercial extensions and support for Silk is provided by eccenca:

http://eccenca.com

Happy linking,

The Silk Team
