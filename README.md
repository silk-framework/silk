# Silk Link Discovery Framework

Silk is an open source framework for integrating heterogeneous data sources. The primary uses cases of Silk include:

- Generating links between related data items within different Linked Data sources.
- Linked Data publishers can use Silk to set RDF links from their data sources to other data sources on the Web.
- Applying data transformations to structured data sources.

General information about Silk can be found on the official [website](http://silk-framework.com).

## Documentation

Community documentation is maintained in the [doc](doc/) folder.

## Requirements

- JDK 7 or later
- [Simple Build Tool](http://www.scala-sbt.org/) (sbt)

Downloading and installing sbt is not necessary as it is available from this directory. Depending on the operating system you may need to adapt the commands below to run it from the local directory, i.e., by using `./sbt` instead of `sbt`

## Running the Silk Workbench

- Execute: `sbt "project workbench" run`
- In your browser, navigate to 'http://localhost:9000'

## Packaging the Silk Workbench

- Execute: `sbt "project workbench" dist`
- The package should be available in the folder silk-workbench/target/universal

## Building a Silk Single Machine Jar

- Execute: `sbt "project singlemachine" assembly`
- The generated jar can be executed with: `java -DconfigFile=<Silk-LSL file> [-DlinkSpec=<Interlink ID>] [-Dthreads=<threads>]  [-DlogQueries=(true/false)] [-Dreload=(true/false)] -jar silk.jar`
