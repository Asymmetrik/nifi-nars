# nifi-nars

This project contains assorted [Apache NiFi](http://nifi.apache.org/) components.

# Requirements

* Apache NiFi 1.1.0 or newer
* JDK 1.8 or newer
* [Apache Maven](http://maven.apache.org/) 3.1.0 or newer

# Getting Started

## Building
```
$ mvn clean package
```

This will create several .nar files, and collect them under `dist/target/asymmetrik-nifi-nars-${project.version}`. For convenience, this will also package all .nar files into a single distribution tar.gz file under `dist/target/`.

```
$ ls -1 dist/target/asymmetrik-nifi-nars-*
dist/target/asymmetrik-nifi-nars-0.1.0-SNAPSHOT.tar.gz

dist/target/asymmetrik-nifi-nars-0.1.0-SNAPSHOT:
asymmetrik-nifi-nars-0.1.0-SNAPSHOT
```

## Deploying

Navigate to your NiFi installation, and edit `conf/nifi.properties` adding the nars as an additional `nifi.nar.library.directory.*` entry. For more information, see the "Core Properties" section of the [NiFi System Administrator’s Guide](http://nifi.apache.org/docs/nifi-docs/html/administration-guide.html#system_properties).

For example:

```
nifi.nar.library.directory=./lib
nifi.nar.library.directory.ext1=/path/to/asymm-nifi-nars/dist/target/asymmetrik-nifi-nars-0.1.0-SNAPSHOT
```

Then start NiFi as you normally would:

```
$ ./bin/nifi.sh start
```
