#!/bin/sh

export IGNITE_HOME=$PWD/../conf
java -XX:+UseG1GC \
     -Xmx5G \
     -Xms512m \
     -XX:+DisableExplicitGC \
     -Dlog4j.configurationFile=../conf/log4j2.xml \
     -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory \
     -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
     -jar ../lib/${project.build.finalName}.jar -conf ../conf/feeder-conf.json