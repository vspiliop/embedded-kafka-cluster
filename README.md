[![License](https://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.vspiliop.testing/embedded-kafka-cluster/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.vspiliop.testing/embedded-kafka-cluster)

# kafka-testing-utilities #

Start a fully configurable docker based Kafka cluster as part of your tests by just by adding @EmbeddedKafkaCluster to your test class.

## Build/ Install

Requires a running docker service.

### Build and run tests

```
mvn clean package
```

### Run tests and install to local maven repo 

```
mvn clean install
```

### Release to Sonatype Nexus Repository Manager and Maven Central

```
mvn release:clean release:prepare
```

followed by

```
mvn release:perform
```

## @EmbeddedKafkaCluster

### Features

 * Start a docker based Kafka cluster that includes one or more Kafka brokers, Zookeepers and Confluent Schema Registries.
 * Parameterize the cluster (see Parameters section).
 * The cluster starts before the test spring context.
 * Reconfigure your spring test context to point to the embedded Kafka cluster (e.g point to the proper IPs and ports).
 * Run more than one unit test or Cucumber feature scenario in parallel and reduce the total execution time.

### Parameters

| Parameter | Description | Example | Default Value |
| --- | --- | --- | --- |
| topics | List of topics to be created | topics = {"topic1.t", "topic2.t"} or topics = {"${my.spring.property}"} | |
| brokersCount or value | # of brokers of the cluster | brokersCount = 3 | 1 |
| zookeepersCount | # of ZKs of the cluster | zookeepersCount = 3 | 1 |
| schemaRegistriesCount | # of Confluent Schema Registries of the cluster | schemaRegistriesCount = 3 | 1 |
| kafkaServersProperty | The spring test context property to reconfigure, so that it points to the embedded Kafka broker  | kafkaServersProperty = "my.broker.url" | config.kafka.bootstrap |
| schemaRegistryServersProperty | The spring test context property to reconfigure, so that it points to the embedded Schema Registry | schemaRegistryServersProperty = "my.registry.url" | config.kafka.registry |
| platformVersion | Confluent platform version | 4.1.2 | 4.1.2 |
| minInSyncReplicas | Minimum in sync replicas for all topics, apart from transaction internal ones. | 1 | 1 |
| minTransactionInSynceReplicas |  Minimum in sync replicas for transaction internal topics.  | 1 | 1 |
| defaultReplicationFactor | Default replication factor for all topics, apart from internal ones. | 1 | 1 |
| transactionReplicationFactor | Default replication factor for transaction topic. | 1 | 1 |
| offsetsReplicationFactor | Default replication factor for offsets internal topic. | 1 | 1 |
| schemaRegistryReplicationFactor | Replication factor for _schemas topic used by Confluent Schema Registry. | 1 | 1 |

### Usage Examples

<a href="https://www.testcontainers.org/">Testcontainers</a> based.

The typical usage of this annotation is like (see also tests): 

```
 @RunWith(SpringRunner.class)
 @EmbeddedKafkaCluster(topics = {"test.t"})
 public class MyKafkaTests {
    
    // optionally autowire if needed
    @Autowired
    private EmbeddedMultiNodeKafkaCluster embeddedMultiNodeKafkaCluster;

 }
```

Control the number of services that form the cluster (see also tests):

```
@EmbeddedKafkaCluster(topics = {"test.t"}, brokersCount = 1, zookeepersCount = 1, schemaRegistriesCount = 1)
```

Always use "topics" parameter to pre-create all topics that you use, as it speeds up the consumption of events!

## Running multiples tests in parallel

<a href="https://www.testcontainers.org/">Testcontainers</a> framework is used to spin up docker containers to form a proper Kafka cluster (broker, zookeeper, schema registry). Each service has its own container, while the test (e.g. junit or cucumber feature) runs locally (not in a container).

```
46141b0a712b        confluentinc/cp-kafka:4.1.2             "/etc/confluent/dock…"   32 seconds ago      Up 28 seconds       0.0.0.0:33158->9092/tcp, 0.0.0.0:33157->9093/tcp                            confident_williams
24685e0dd16a        alpine/socat:latest                     "/bin/sh -c 'socat T…"   41 seconds ago      Up 35 seconds       0.0.0.0:33156->2181/tcp, 0.0.0.0:33155->9093/tcp                            testcontainers-socat-Vsk8MbBx
2e975fe6b3fc        confluentinc/cp-schema-registry:4.1.2   "/etc/confluent/dock…"   41 seconds ago      Up 36 seconds       0.0.0.0:33151->8081/tcp                                                     fervent_neumann
6ead242bca82        confluentinc/cp-zookeeper:4.1.2         "/etc/confluent/dock…"   41 seconds ago      Up 36 seconds       0.0.0.0:33154->2181/tcp, 0.0.0.0:33153->2888/tcp, 0.0.0.0:33152->3888/tcp   friendly_heyrovsky
42bc258afbd2        quay.io/testcontainers/ryuk:0.2.2       "/app"                   46 seconds ago      Up 43 seconds       0.0.0.0:33150->8080/tcp        
```

Docker deals with all port allocations and there is no chance of port conflicts.

Tests (junit ones or cucumber features) may run in parallel. Select proper maven profile to control the level of parallelism (rule of thump is the # of cores). If you use too many jvms your laptop will be unresponsive and random tests will fail.

```
mvn package -Pparallel_2_jvms # default profile if none is set

mvn package -Pparallel_3_jvms

mvn package -Pparallel_4_jvms

mvn package -Pserial
```
  
#### alpine/socat:latest

A <a href="https://www.testcontainers.org/">testcontainer</a> specific instance so that the kafka clients (of the unit test that runs outside of the docker network) may access the Kafka container.

#### quay.io/testcontainers/ryuk:0.2.2 

A <a href="https://www.testcontainers.org/">testcontainer</a> specific instance used by the framework to start and stop containers.

## Add as a test dependency to your project

From Maven Central as follows:

```
<dependency>
  <groupId>io.github.vspiliop.testing</groupId>
  <artifactId>embedded-kafka-cluster</artifactId>
  <version>...</version>
  <scope>test</scope>
</dependency>
```
