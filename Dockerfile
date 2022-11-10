FROM openjdk:11-jdk as builder

COPY ./silk-workbench/target/universal /build
COPY ./conf /build/conf

WORKDIR /build
RUN \
  mkdir -p /build/app \
  && tar -xvzf /build/silk-workbench*.tgz -C /build/app

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
