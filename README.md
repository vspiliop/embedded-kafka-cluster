# kafka-testing-utilities #

Includes Kafka related testing utilities.

## Build/ Install

# Build and run tests

```
mvn clean package
```

# Run tests and install to local maven repo 

```
mvn clean install
```

## @EmbeddedKafkaCluster

### Features

 * Start a docker based Kafka cluster that includes one or more Kafka brokers, Zookeepers and Confluent Schema Registries.
 * Parameterize the cluster (see Parameters section).
 * The cluster starts before the test spring context.
 * Reconfigure your spring test context to point to the embedded Kafka cluster (e.g point to the proper IPs and ports).
 * Run more than one unit test or Cucumber feature scenario in parallel and reduce the total execution time.

### Parameters

| Parameter | Description | Example |
| --- | --- | --- |
| topics | List of topics to be created | topics = {"topic1.t", "topic2.t"}<br>topics = { "${my.spring.application.property}" } |
| brokersCount | # of brokers of the cluster | brokersCount = 3 |
| zookeepersCount | # of ZKs of the cluster | zookeepersCount = 3 |
| schemaRegistriesCount | # of Confluent Schema Registries of the cluster | zookeepersCount = 3 |

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

## IntegrationTestUtils

Helper methods to send and receive event based on ([IntegrationTestUtils.java](https://github.com/confluentinc/kafka-streams-examples/blob/master/src/test/java/io/confluent/examples/streams/IntegrationTestUtils.java))

When producing or consuming via the helper methods the port lookup for Kafka broker is as follows:
 1. explicitly passed
 2. UNIT_TEST_KAFKA_PORT jvm parameter is evaluated
 3. UNIT_TEST_KAFKA_PORT environmental property is evaluated
 4. Default value 9092 is used
 
When producing or consuming via the helper methods the port lookup for Schema Registry is as follows:
 1. explicitly passed
 2. UNIT_TEST_REGISTRY_PORT jvm parameter is evaluated
 3. UNIT_TEST_REGISTRY_PORT environmental property is evaluated
 4. Default value 8082 is used

## Add as a test dependency to your project

```
<dependency>
  <groupId>com.etiology.testing</groupId>
  <artifactId>kafka-testing-utilities</artifactId>
  <version>...</version>
  <scope>test</scope>
</dependency>
```