[![Build Status](https://app.travis-ci.com/silk-framework/silk.svg?branch=develop)](https://app.travis-ci.com/silk-framework/silk)
[![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy?template=https://github.com/silk-framework/silk)

# Silk Link Discovery Framework

Silk is an open source framework for integrating heterogeneous data sources. The primary uses cases of Silk include:

- Generating links between related data items within different Linked Data sources.
- Linked Data publishers can use Silk to set RDF links from their data sources to other data sources on the Web.
- Applying data transformations to structured data sources.

General information about Silk can be found on the official [website](http://silkframework.org).

## Documentation

Community documentation is maintained in the [doc](doc/) folder.

## Requirements

### Local build

- JDK 21
- [Simple Build Tool](http://www.scala-sbt.org/) (sbt)
- [Yarn](https://yarnpkg.com) for React/JavaScript build pipeline (e.g. ^1.22.0)
- Node (e.g. ^14.17.0)

Downloading and installing sbt is not necessary as it is available from this directory. Depending on the operating system you may need to adapt the commands below to run it from the local directory, i.e., by using `./sbt` instead of `sbt`

### docker based build

- docker (version >=17.05-xx)

## Running the Silk Workbench

- Execute: `sbt "project workbench" run`
- In your browser, navigate to 'http://localhost:9000'

## Running the Silk Workbench as docker container

- Build or pull the latest docker image:
  - Build the docker image with:
    ```
    sbt universal:packageZipTarball
    docker build -t silkworkbench/silk-framework:latest .
    ```
    (This maybe take some minutes)
    For a production build you can set following ENV variables when building the application artefact, e.g.
    ```
    BUILD_ENV=production GIT_DESCRIBE=$(git describe) sbt universal:packageZipTarball
    ```
  - Pull the docker image via: docker pull silkworkbench/silk-framework
- Run the docker container with: `docker run -d --name silk-workbench -p 80:80 silkworkbench/silk-framework:latest`
- In your browser, navigate to 'http://DOCKER_HOST:80'
- To make the userdata available from outside the docker container you can add a volume mount, therefore add `-v $PWD:/opt/silk/workspace` to the docker run command.

__Example__

```bash
docker run -d --name silk-workbench -v $PWD:/opt/silk/workspace -p 80:80 silkworkbench/silk-framework:latest
```
This will start a silk-workbench with a docker container and can be accessed via http port 80.
The default production configuration can be found in `conf/defaultProduction.conf`. If you want
to use a different configuration add `-v <PATH_TO_OTHER_CONFIG>:/opt/config/production.conf` to the `docker run` command.

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
