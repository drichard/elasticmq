ElasticMQ
=========

tl;dr
-----

* message queue system
* runs stand-alone ([download](https://s3-eu-west-1.amazonaws.com/softwaremill-public/elasticmq-0.6.3.tar.gz)) or embedded
* [Amazon SQS](http://aws.amazon.com/sqs/)-compatible interface
* fully asynchronous implementation, no blocking calls

Summary
-------

ElasticMQ is a message queue system, offering an actor-based Scala and an [SQS](http://aws.amazon.com/sqs/)-compatible
REST (query) interface.

ElasticMQ follows the semantics of SQS. Messages are received by polling the queue.
When a message is received, it is blocked for a specified amount of time (the visibility timeout).
If the message isn't deleted during that time, it will be again available for delivery.
Moreover, queues and messages can be configured to always deliver messages with a delay.

The focus in SQS (and ElasticMQ) is to make sure that the messages are delivered.
It may happen, however, that a message is delivered twice (if, for example, a client dies after receiving a message and
processing it, but before deleting). That's why clients of ElasticMQ (and Amazon SQS) should be idempotent.

As ElasticMQ implements a subset of the [SQS](http://aws.amazon.com/sqs/) query (REST) interface, it is a great SQS
alternative both for testing purposes (ElasticMQ is easily embeddable) and for creating systems which work both within
and outside of the Amazon infrastructure.

The future will most probably bring even more exciting features :).

Community
---------

* [Blog](http://www.warski.org/blog/category/elasticmq/)
* Forum (discussions, help): [Google group](https://groups.google.com/forum/?fromgroups#!forum/elasticmq).

Installation: stand-alone
-------------------------

You can download the stand-alone distribution here:
* [https://s3/.../elasticmq-0.6.3.tar.gz](https://s3-eu-west-1.amazonaws.com/softwaremill-public/elasticmq-0.6.3.tar.gz)
* [https://s3/.../elasticmq-0.6.3.zip](https://s3-eu-west-1.amazonaws.com/softwaremill-public/elasticmq-0.6.3.zip)

Java 6 or above is required for running the server.

Simply run the jar and you should get a working server, which binds to `localhost:9324`:

    java -jar elasticmq-0.7.0.jar

ElasticMQ uses [Typesafe Config](https://github.com/typesafehub/config) for configuration. To use specify custom
configuration values, create a file (e.g. `custom.conf`), fill it in with the desired values, and pass it to the server:

    java -Dconfig.file=custom.conf -jar elasticmq-0.7.0.jar

The config file may contain any configuration for Akka, Spray and ElasticMQ. Current ElasticMQ configuration values are:

````
// What is the outside visible address of this ElasticMQ node (used by rest-sqs)
node-address {
    protocol = http
    host = localhost
    port = 9324
    context-path = ""
}

rest-sqs {
    enabled = true
    bind-port = 9324
    bind-hostname = "0.0.0.0"
    // Possible values: relaxed, strict
    sqs-limits = relaxed
}
````

You can also provide an alternative [Logback](http://logback.qos.ch/) configuration file (the default is configured to
log INFO logs and above to the console):

    java -Dlogback.configurationFile=my_logback.xml -jar elasticmq-0.7.0.jar

Starting an embedded ElasticMQ server with an SQS interface
-----------------------------------------------------------

    // First we need to create a Node
    val node = NodeBuilder.withStorage(new InMemoryStorage)
    // Then we can expose the native client using the SQS REST interface
    val server = new SQSRestServerBuilder(actorSystem, 9324, new NodeAddress()).start()
    // ... use ...
    // Finally we need to stop the server and the node
    server.stop()
    node.shutdown()

Using the Amazon Java SDK to access an ElasticMQ Server
-------------------------------------------------------

To use [Amazon Java SDK](http://aws.amazon.com/sdkforjava/) as an interface to an ElasticMQ server you just need
to change the endpoint:

    client = new AmazonSQSClient(new BasicAWSCredentials("x", "x"))
    client.setEndpoint("http://localhost:9324")

The endpoint value should be the same address as the `NodeAddress` provided as an argument to
`SQSRestServerBuilder` or in the configuration file.

The `rest-sqs-testing-amazon-java-sdk` module contains some more usage examples.

ElasticMQ dependencies in SBT
-----------------------------

    val elasticmqSqs        = "org.elasticmq" %% "elasticmq-rest-sqs"         % "0.7.0"

If you don't want the SQS interface, but just use the actors directly, you can add a dependency only to the `core`
module:

    val elasticmqCore       = "org.elasticmq" %% "elasticmq-core"             % "0.7.0"

If you want to use a snapshot version, you will need to add the [https://oss.sonatype.org/content/repositories/snapshots/](https://oss.sonatype.org/content/repositories/snapshots/) repository to your configuration.

ElasticMQ dependencies in Maven
-------------------------------

Dependencies:

    <dependency>
        <groupId>org.elasticmq</groupId>
        <artifactId>elasticmq-rest-sqs_2.10</artifactId>
        <version>0.7.0</version>
    </dependency>

If you want to use a snapshot version, you will need to add the [https://oss.sonatype.org/content/repositories/snapshots/](https://oss.sonatype.org/content/repositories/snapshots/) repository to your configuration.

Replication, journaling, SQL backend
------------------------------------

Until version 0.7.0, ElasticMQ included optional replication, journaling and an SQL message storage. These modules
have not yet been reimplemented using the new Akka core.

Current versions
----------------

*Stable*: 0.7.0

*Development*: 0.7.1-SNAPSHOT

Logging
-------

ElasticMQ uses [Slf4j](http://www.slf4j.org/) for logging. By default no logger backend is included as a dependency,
however [Logback](http://logback.qos.ch/) is recommended.

Performance
-----------

Tests done on a 2009 MBP, 2.4GHz Core2Duo, 8GB RAM, no replication. Throughput is in messages per second (messages are
small).

Directly accessing the client:

    Running test for [in-memory], iterations: 10, msgs in iteration: 100000, thread count: 1.
    Overall in-memory throughput: 41373.603641

    Running test for [in-memory], iterations: 10, msgs in iteration: 100000, thread count: 2.
    Overall in-memory throughput: 32646.665143

    Running test for [in-memory], iterations: 3, msgs in iteration: 1000000, thread count: 1.
    Overall in-memory throughput: 35157.211330

Through the SQS REST interface:

    Running test for [rest-sqs + in-memory], iterations: 10, msgs in iteration: 1000, thread count: 20.
    Overall rest-sqs + in-memory throughput: 781.460628

Note that both the client and the server were on the same machine.

Test class: `org.elasticmq.performance.LocalPerformanceTest`.

Technology
----------

* Core: [Scala](http://scala-lang.org) and [Akka](http://akka.io/).
* Rest server: [Spray](http://http://spray.io/), a high-performance,
  asynchronous, REST/HTTP toolkit.
* Testing the SQS interface: [Amazon Java SDK](http://aws.amazon.com/sdkforjava/);
  see the `rest-sqs-testing-amazon-java-sdk` module for the testsuite.

Change log
----------

#### Version 0.7.0 (?? June 2013)

* reimplemented using Akka and Spray
* long polling support
* bug fixes

#### Version 0.6.3 (21 January 2013)

* Scala 2.10 support
* Changing the way the stand-alone server is configured

#### Version 0.6.2 (13 December 2012)

* bug fixes
* properly handling SQS receipt handles - message can be deleted only when passing the most recent receipt handle

#### Version 0.6.1 (18 November 2012)

* using Sonatype's OSS repositories for releases
* library upgrades

#### Version 0.6 (19 October 2012)

* batch operations in SQS (send, receive, delete, change visibility)
* changed `SQSRestServerFactory` to `SQSRestServerBuilder`
* "strict" and "relaxed" modes when creating an SQS server: the limits enforced by SQS are optionally checked, e.g. for
batch operations (max 10 messages), maximum message size (64KB). Strict by default.

#### Version 0.5 (26 May 2012)

* stand-alone distribution ([download](https://github.com/downloads/adamw/elasticmq/elasticmq-0.5.tar.gz))
* file log for message storage (journal)
* factoring out `storage-database` module, to decrease the dependencies of the core modules

#### Version 0.4 (27 Mar 2012)

* replication

#### Version 0.3 (6 Feb 2012)

* in-memory storage
* new native API
* bug fixes

#### Version 0.2 (12 Jan 2012)

* new SQS functions support
* testing with Amazon Java SDK
* bug fixes

#### Version 0.1 (12 Oct 2011)

* initial release
* DB storage
* SQS interface support