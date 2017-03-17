# Compile

```mvn clean install```

# Run

## Feeder

Upload the tgz `ignite-cq-bench-feeder/target/ignite-cq-bench-feeder-1.0-SNAPSHOT-feeder.tgz` on a server
Untar the archive
Update `-Xmx5G` of `bin/startup.sh` according to your env
Udpate `conf/ignite-cq.xml` with the addresses of your distributed environment

```
cd bin/
./startup.sh
```

It stops after `simulation.duration` defined in `conf/feeder-conf.json`

## Consumer

Same as feeder

# Results

