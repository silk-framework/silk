FROM openjdk:8-jdk as builder

COPY . /build
WORKDIR /build
RUN \
  echo "Install node, npm and yarn" \
  && bash /build/dockerfiles/install-node.sh \
  && echo "Install Maven 3.3.x" \
  && curl -sL https://archive.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz | tar -xz -C /opt \
  && echo "Link current Maven" \
  && ln -s /opt/apache-maven-3.3.9 /opt/maven \
  && echo "\ncurrent maven version:" \
  && /opt/maven/bin/mvn --version \
  && echo "fetch jai-core to fix bug with missing maven artifacts" \
  && /opt/maven/bin/mvn dependency:get -DremoteRepositories=http://maven.geotoolkit.org -Dartifact=javax.media:jai_core:1.1.3 -Ddest=/tmp \
  && ./sbt "project workbench" universal:package-zip-tarball || echo "" \
  && ./sbt "project workbench" universal:package-zip-tarball || echo "" \
  && mkdir -p /build/app \
  && tar -xvzf /build/silk-workbench/target/universal/silk-workbench*.tgz -C /build/app

FROM openjdk:8u151-jre
ENV \
  SILK_HOME="/opt/silk" \
  WORKDIR="/opt/silk/workspace" \
  # set JAVA OPTIONS
  # provide a random env better suited for headless work such as docker images \
  # http:\/\/www.labouisse.com\/misc\/2014\/06\/19\/tomcat-startup-time-surprises \
  DEFAULT_JAVA_OPTS="-server -Djava.security.egd=file:/dev/./urandom" \
  JAVA_OPTS="-Xms1g -Xmx2g" \
  # configure application port and expose it
  SERVER_PORT=80 \
  SERVER_CONTEXTPATH="/"

# add configuration & webapp
COPY --from=builder /build/app/silk-workbench* /silk-workbench

# Cleanup
RUN \
  echo "Cleanup" \
  && npm cache clean --force \
  && yarn cache clean \
  && apt-get autoremove -y \
  && apt-get clean -y \
  && rm -rf /build/* /var/tmp/* \
  && rm -rf /var/lib/apt/lists/*

# expose port
EXPOSE ${SERVER_PORT}

# set working dir
WORKDIR ${WORKDIR}
VOLUME "${WORKDIR}"

HEALTHCHECK --interval=5s --timeout=10s --retries=20 \
  CMD curl "http://localhost:${SERVER_PORT}${SERVER_CONTEXTPATH}"

# start application
CMD [ "/silk-workbench/bin/silk-workbench", "-Dplay.server.http.port=80", "-Dpidfile.path=/dev/null" ]
