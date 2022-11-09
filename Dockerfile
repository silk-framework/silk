FROM openjdk:11-jdk as builder

COPY . /build
WORKDIR /build
RUN \
  echo "install yarn" \
  && apt-get update -y \
  && apt-get install -y apt-transport-https curl \
  && curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
  && echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
  && apt-get update -y \
  && curl -sL https://deb.nodesource.com/setup_14.x | bash - \
  && apt-get install -y nodejs yarn \
  && curl -L https://www.npmjs.com/install.sh | sh \
  && echo "\ncurrent yarn version:" \
  && npm install -g npm \
  && yarn --version \
RUN \
  ./sbt "project workbench" compile universal:packageZipTarball
RUN \
  mkdir -p /build/app \
  && tar -xvzf /build/silk-workbench/target/universal/silk-workbench*.tgz -C /build/app

FROM openjdk:11-jre
ENV \
  SILK_HOME="/opt/silk" \
  WORKDIR="/opt/silk/workspace" \
  # set JAVA OPTIONS
  # provide a random env better suited for headless work such as docker images \
  # http:\/\/www.labouisse.com\/misc\/2014\/06\/19\/tomcat-startup-time-surprises \
  DEFAULT_JAVA_OPTS="-server -Djava.security.egd=file:/dev/./urandom" \
  JAVA_OPTS="-Xms1g -Xmx2g" \
  # configure application port and expose it
  PORT=80 \
  SERVER_CONTEXTPATH="/"

# add configuration & webapp
COPY --from=builder /build/app/silk-workbench* /silk-workbench
COPY --from=builder /build/conf/defaultProduction.conf /opt/config/production.conf

# expose port
EXPOSE ${PORT}

# set working dir
WORKDIR ${WORKDIR}
VOLUME "${WORKDIR}"

HEALTHCHECK --interval=5s --timeout=10s --retries=20 \
  CMD curl "http://localhost:${PORT}${SERVER_CONTEXTPATH}"

# start application
CMD /silk-workbench/bin/silk-workbench -Dconfig.file=/opt/config/production.conf -Dplay.server.http.port=${PORT} -Dpidfile.path=/dev/null
