package io.github.vspiliop.testing.kafka.cluster;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.vspiliop.testing.kafka.testcontainers.ToxiproxiedKafkaContainer;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import io.github.vspiliop.testing.kafka.spring.context.DockerKafkaCluster;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * Using testcontainers framework to start a Confluent Kafka cluster (v4.1.2)
 * that consists of:
 * <ul>
 * <li>a Kafka broker</li>
 * <li>a Zookeeper</li>
 * <li>a Schema Registry</li>
 * </ul>
 * 
 * For versions information check <a href=
 * "https://docs.confluent.io/current/installation/versions-interoperability.html">here</a>.
 * <p>
 * Hooks the starting and stopping of the cluster to Spring's beans lifecycle.
 * 
 * @author spiliopoulosv
 *
 */
@Slf4j
public class DockerKafkaClusterFacade implements InitializingBean, DisposableBean {

	public static final String BEAN_NAME = "dockerMultiNodeKafkaCluster";

	final List<ToxiproxiedKafkaContainer> brokers;

	final List<GenericContainer> zookeepers;

	final List<GenericContainer> schemaRegistries;

	/**
	 * Creates and starts the cluster.
	 */
	public DockerKafkaClusterFacade(DockerKafkaCluster dockerKafkaCluster) {
		// e.g. zookeeper1:2181,zookeeper2:2181,zookeeper3:2181
		String externalZookeeperUrl = IntStream.range(1, dockerKafkaCluster.zookeepersCount() + 1)
			.mapToObj(i -> "zookeeper" + i + ":2181")
			.reduce((url1, url2) -> url1 + "," + url2)
			.get();
		
		// e.g. zookeeper1:2888:3888,zookeeper2:2888:3888,zookeeper3:2888:3888
		String zookeeperServers = IntStream.range(1, dockerKafkaCluster.zookeepersCount() + 1)
				.mapToObj(i -> "zookeeper" + i + ":2888:3888")
				.reduce((url1, url2) -> url1 + ";" + url2)
				.get();
		
		// e.g. PLAINTEXT://kafka1:9092,PLAINTEXT://kafka2:9092,PLAINTEXT://kafka3:9092
		String brokerUrl = IntStream.range(1, dockerKafkaCluster.brokersCount() + 1)
				.mapToObj(i -> "PLAINTEXT://kafka" + i + ":9092")
				.reduce((url1, url2) -> url1 + "," + url2)
				.get();
		
		Network dockerNetwork = Network.newNetwork();
		
		brokers = IntStream.range(1, dockerKafkaCluster.brokersCount() + 1)
				.mapToObj(i -> new ToxiproxiedKafkaContainer(dockerKafkaCluster.platformVersion())
            .withNetwork(dockerNetwork)
            .withNetworkAliases("kafka" + i)
            .withEnv("CONFLUENT_SUPPORT_METRICS_ENABLE", "false")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", dockerKafkaCluster.transactionReplicationFactor() + "")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", dockerKafkaCluster.minTransactionInSynceReplicas() + "")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", dockerKafkaCluster.offsetsReplicationFactor() + "")
            .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1")
            .withEnv("KAFKA_MIN_INSYNC_REPLICAS", dockerKafkaCluster.minInSyncReplicas() + "")
            .withEnv("KAFKA_NUM_PARTITIONS", "1")
            .withEnv("KAFKA_DEFAULT_REPLICATION_FACTOR", dockerKafkaCluster.defaultReplicationFactor() + "")
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withEnv("KAFKA_BROKER_ID", i+"")
            .withExternalZookeeper(externalZookeeperUrl))
        .map(ToxiproxiedKafkaContainer.class::cast)
				.collect(Collectors.toList());
		
		zookeepers = IntStream.range(1, dockerKafkaCluster.zookeepersCount() + 1)
				.mapToObj(i -> new GenericContainer("confluentinc/cp-zookeeper:" + dockerKafkaCluster.platformVersion())
            .withNetwork(dockerNetwork)
            .withNetworkAliases("zookeeper" + i)
            .withEnv("ZOOKEEPER_CLIENT_PORT", "2181")
            .withEnv("ZOOKEEPER_SERVER_ID", i + "")
            .withEnv("ZOOKEEPER_SERVERS", zookeeperServers)
            .withEnv("ZOOKEEPER_TICK_TIME", "2000")
            .withEnv("ZOOKEEPER_INIT_LIMIT", "5")
            .withEnv("ZOOKEEPER_SYNC_LIMIT", "2"))
				.collect(Collectors.toList());
		
		schemaRegistries = IntStream.range(1, dockerKafkaCluster.schemaRegistriesCount() + 1)
				.mapToObj(i -> new GenericContainer("confluentinc/cp-schema-registry:" + dockerKafkaCluster.platformVersion())
            .withNetwork(dockerNetwork)
            .withNetworkAliases("schema-registry" + i)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry" + i)
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_TOPIC_REPLICATION_FACTOR", dockerKafkaCluster.schemaRegistryReplicationFactor() + "")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", brokerUrl)
            .withEnv("SCHEMA_REGISTRY_DEBUG", log.isTraceEnabled()? "true" : "false")
            .withEnv("SCHEMA_REGISTRY_MASTER_ELIGIBILITY", "true")
            .withStartupTimeout(Duration.ofMinutes(2)).withExposedPorts(8081))
				.collect(Collectors.toList());
	}

	/**
	 * Creates and starts the cluster.
	 */
	public void start() {
		log.info("Starting Kafka cluster..");
		Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
		zookeepers.stream().parallel().filter(service -> !service.isRunning()).forEach(GenericContainer::start);
		brokers.stream().parallel().filter(service -> !service.isRunning()).forEach(GenericContainer::start);
		schemaRegistries.stream().parallel().filter(service -> !service.isRunning()).forEach(GenericContainer::start);
		// enable logging of docker containers
		if (log.isTraceEnabled()) {
			zookeepers.stream().parallel().forEach(service -> service.followOutput(logConsumer));
			brokers.stream().parallel().forEach(service -> service.followOutput(logConsumer));
			schemaRegistries.stream().forEach(service -> service.followOutput(logConsumer));
		}
		log.info("Kafka cluster started..");
	}

	/**
	 * Stops the cluster.
	 */
	public void stop() {
		log.info("Stopping Kafka cluster..");
		zookeepers.stream().parallel().filter(service -> service.isRunning()).forEach(GenericContainer::stop);
		brokers.stream().parallel().filter(service -> service.isRunning()).forEach(GenericContainer::stop);
		schemaRegistries.stream().parallel().filter(service -> service.isRunning()).forEach(GenericContainer::stop);
		log.info("Kafka cluster Stopped..");
	}

	@Override
	public void destroy() {
    stop();
	}

	@Override
	public void afterPropertiesSet() {
    start();
	}

	public String getKafkaBootstapServers() {
		// this should not be required as all docker services have a waiting policy and
		// the unit test that calls this method should always run after the cluster is
		// fully available
		brokers.stream()
				.forEach(broker -> Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> broker.isRunning()));
		return brokers.stream().map(broker -> broker.getBootstrapServers()).reduce((url1, url2) -> url1 + "," + url2)
				.get();
	}

	public String getSchemaRegistryUrls() {
		// this should not be required as all docker services have a waiting policy and
		// the unit test that calls this method should always run after the cluster is
		// fully available
		schemaRegistries.stream()
				.forEach(registry -> Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> registry.isRunning()));
		return schemaRegistries.stream().map(registry -> String.format("http://%s:%s", registry.getContainerIpAddress(),
				registry.getMappedPort(8081))).reduce((url1, url2) -> url1 + "," + url2).get();
	}
}
