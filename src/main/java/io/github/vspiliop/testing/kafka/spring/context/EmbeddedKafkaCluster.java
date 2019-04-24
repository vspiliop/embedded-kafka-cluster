package io.github.vspiliop.testing.kafka.spring.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Using testcontainers framework to start a Confluent Kafka cluster (v4.1.2)
 * that consists of:
 * <ul>
 * <li>Kafka broker(s)</li>
 * <li>Zookeeper(s)</li>
 * <li>Schema Registry(s)</li>
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
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EmbeddedKafkaCluster {

	/**
	 * @return the number of brokers
	 */
	@AliasFor("brokersCount")
	int value() default 1;

	/**
	 * @return the number of brokers
	 */
	@AliasFor("value")
	int brokersCount() default 1;
	
	/**
	 * @return the number of zookeepers
	 */
	int zookeepersCount() default 1;
	
	/**
	 * @return the number of Confluent schema registries
	 */
	int schemaRegistriesCount() default 1;
	
	/**
	 * A list of topics to be automatically created before the client application starts.
	 * <p>
	 * This is required even if automatic topic creation is enabled:
	 * It may lead to a 5 minutes delay (default value of consumer property <a href="http://kafka.apache.org/090/documentation.html">metadata.max.age.ms</a> is 5 minutes) in case your client application is using the Streams API
	 * and the stream starts consuming before the automatically created topic has a leader for all its partitions. To avoid this you should provide all topics for creation before the spring context starts.
	 * <p>
	 * For more information check <a href="https://stackoverflow.com/questions/41975747/why-does-kafka-consumer-takes-long-time-to-start-consuming">this</a> out.
	 */
	String[] topics() default { };
	
	/**
	 * The spring test context property to reconfigure, so that it points to the embedded Kafka broker
	 */
	String kafkaServersProperty() default "config.kafka.bootstrap";

	/**
	 * The spring test context property to reconfigure, so that it points to the embedded Schema Registry
	 */
	String schemaRegistryServersProperty() default "config.kafka.registry";
	
	/**
	 * Confluent paltform version.
	 * <p>
	 * For versions information check <a href="https://docs.confluent.io/current/installation/versions-interoperability.html">here</a>.
	 * 
	 */
	String platformVersion() default "4.1.2";
	
	/**
	 * Minimum in sync replicas for all topics, apart from transaction internal ones. 
	 */
	int minInSyncReplicas() default 1;
	
	/**
	 * Minimum in sync replicas for transaction internal topics. 
	 */
	int minTransactionInSynceReplicas() default 1;
	
	/**
	 * Default replication factor (if not explicitly specified) for all topics, apart from internal ones.
	 */
	int defaultReplicationFactor() default 1;
	
	/**
	 * Default replication factor for transaction topic.
	 */
	int transactionReplicationFactor() default 1;
	
	/**
	 * Default replication factor for offsets internal topic.
	 */
	int offsetsReplicationFactor() default 1;
	
	/**
	 * Override replication factor for _schemas topic used by Confluent Schema Registry.
	 */
	int schemaRegistryReplicationFactor() default 1;
	
}
