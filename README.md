## Akka Streams application example

The application checks discrepancy in data between Vertica and Aerospike databases.

### HOWTO: Build

* Docker image, locally: `sbt docker:publishLocal`
* Docker image: `sbt docker:publish`
* Distribution package (bare): `sbt "show stage"`
* Distribution package (zip): `sbt "show stage"` or `sbt "show dist"`

### HOWTO: Run locally

```
set ENV=local
sbt run
```

```
\$ ./target/universal/stage/bin/discrepancy-vertica-aerospike \
    -Dparam1=value1 ...
```

## HOWTO: Run in Docker

```
\$ export JAVA_OPTS=" \
-Dparam1=value1 \
...
"
\$ docker run -e JAVA_OPTS -i discrepancy-vertica-aerospike
```
