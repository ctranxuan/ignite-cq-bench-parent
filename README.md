# Intro

This is a project related to the StackOverflow question: http://stackoverflow.com/questions/42805562/can-apache-ignite-continuous-queries-miss-some-update

# Compile

```mvn clean install```

# Run

Starts the consumer first.

## Consumer

Upload the tgz `ignite-cq-bench-consumer/target/ignite-cq-bench-consumer-1.0-SNAPSHOT-consumer.tgz` on a server
Untar the archive
Update `-Xmx5G` of `bin/startup.sh` according to your env
Udpate `conf/ignite-cq.xml` with the addresses of your distributed environment

```
cd bin/
./startup.sh
```

It stops after `simulation.duration` defined in `conf/feeder-conf.json`

## Feeder

Same as consumer

# Results

If you have error messages in `logs/client.log` like that `17:30:04.770 [sys-#41%null%] ERROR o.t.i.c.IgniteClientVerticle - misses index 646 (currentIndex was 648)` then it means that some updates have been received in the wrong orders.
