# Silk Link Discovery Framework
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fsilk-framework%2Fsilk.svg?type=shield)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fsilk-framework%2Fsilk?ref=badge_shield)


Silk is an open source framework for integrating heterogeneous data sources. The primary uses cases of Silk include:

- Generating links between related data items within different Linked Data sources.
- Linked Data publishers can use Silk to set RDF links from their data sources to other data sources on the Web.
- Applying data transformations to structured data sources.

General information about Silk can be found on the official [website](http://silkframework.org).

## Documentation

Community documentation is maintained in the [doc](doc/) folder.

## Requirements

- JDK 8 or later
- [Simple Build Tool](http://www.scala-sbt.org/) (sbt)

Downloading and installing sbt is not necessary as it is available from this directory. Depending on the operating system you may need to adapt the commands below to run it from the local directory, i.e., by using `./sbt` instead of `sbt`

## Running the Silk Workbench

- Execute: `sbt "project workbench" run`
- In your browser, navigate to 'http://localhost:9000'

## Packaging the Silk Workbench as archive

- Execute: `sbt "project workbench" universal:package-zip-tarball`
- The package should be available in the folder `silk-workbench/target/universal`

## Packaging the Silk Workbench as WAR

- Execute: `sbt "project workbench" war`
- The package should be available in the folder `silk-workbench/target/`

## Building a Silk Single Machine Jar

- Execute: `sbt "project singlemachine" assembly`
- The package should be available in the folder `silk-tools/silk-singlemachine/target/scala-{version}`.
- The generated jar can be executed with: `java -DconfigFile=<Silk-LSL file> [-DlinkSpec=<Interlink ID>] [-Dthreads=<threads>]  [-DlogQueries=(true/false)] [-Dreload=(true/false)] -jar silk.jar`


## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fsilk-framework%2Fsilk.svg?type=large)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fsilk-framework%2Fsilk?ref=badge_large)