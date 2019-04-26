package io.github.vspiliop.testing.kafka.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility functions to make integration testing more convenient.
 * 
 * Order of port lookup for Kafka borker and Schema Registry:
 * <ol>
 * <li>UNIT_TEST_KAFKA_PORT_JVM_PROPERTY</li>
 * <li>UNIT_TEST_KAFKA_PORT_ENVIRONMENTAL_PROPERTY</li>
 * <li>Using default values: DEFAULT_KAFKA_BROKER_PORT and
 * DEFAULT_SCHEMA_REGISTRY_PORT</li>
 * </ol>
 * 
 */
@Slf4j
public class IntegrationTestUtils {

  private static final int UNLIMITED_MESSAGES = -1;

  public static final String UNIT_TEST_KAFKA_PORT_ENVIRONMENTAL_PROPERTY = "UNIT_TEST_KAFKA_PORT";
  public static final String UNIT_TEST_KAFKA_PORT_JVM_PROPERTY = "UNIT_TEST_KAFKA_PORT";
  public static final String UNIT_TEST_REGISTRY_PORT_ENVIRONMENTAL_PROPERTY = "UNIT_TEST_REGISTRY_PORT";
  public static final String UNIT_TEST_REGISTRY_PORT_JVM_PROPERTY = "UNIT_TEST_REGISTRY_PORT";
  public static final String DEFAULT_KAFKA_BROKER_PORT = "9092";
  public static final String DEFAULT_SCHEMA_REGISTRY_PORT = "8082";

  public static final long DEFAULT_TIMEOUT = 30 * 1000L;

  public static String kafkaBrokerPort;

  public static String schemaRegistryPort;

  static {
    String brokerPortJvmParameter = System.getProperty(UNIT_TEST_KAFKA_PORT_JVM_PROPERTY);
    String brokerPortEnvVariable = System.getenv(UNIT_TEST_KAFKA_PORT_ENVIRONMENTAL_PROPERTY);
    log.debug("brokerPortJvmParameter: {}", brokerPortJvmParameter);
    log.debug("brokerPortEnvVariable: {}", brokerPortEnvVariable);
    // jvm parameter takes precedence
    IntegrationTestUtils.kafkaBrokerPort = brokerPortJvmParameter != null ? brokerPortJvmParameter
        : brokerPortEnvVariable;
    // if both are null use default port
    IntegrationTestUtils.kafkaBrokerPort = IntegrationTestUtils.kafkaBrokerPort == null ? DEFAULT_KAFKA_BROKER_PORT
        : IntegrationTestUtils.kafkaBrokerPort;
    log.debug("IntegrationTestUtils.kafkaBrokerPort: {}", IntegrationTestUtils.kafkaBrokerPort);

    String registryJvmParameter = System.getProperty(UNIT_TEST_REGISTRY_PORT_JVM_PROPERTY);
    String registryPortEnvVariable = System.getenv(UNIT_TEST_REGISTRY_PORT_ENVIRONMENTAL_PROPERTY);
    log.debug("registryJvmParameter: {}", registryJvmParameter);
    log.debug("registryPortEnvVariable: {}", registryPortEnvVariable);
    // jvm parameter takes precedence
    IntegrationTestUtils.schemaRegistryPort = registryJvmParameter != null ? registryJvmParameter
        : registryPortEnvVariable;
    // if both are null use schemaRegistryPort port
    IntegrationTestUtils.schemaRegistryPort = IntegrationTestUtils.schemaRegistryPort == null
        ? DEFAULT_SCHEMA_REGISTRY_PORT
        : IntegrationTestUtils.schemaRegistryPort;
    log.debug("IntegrationTestUtils.schemaRegistryPort: {}", IntegrationTestUtils.schemaRegistryPort);
  }

  public static Properties producerProperties(String clientId, Class<?> valueSerializerClass) {
    return producerProperties(clientId, String.format("http://localhost:%s", kafkaBrokerPort),
        String.format("http://localhost:%s", schemaRegistryPort), valueSerializerClass);
  }

  public static Properties producerProperties(String clientId, String bootstrapUrl, String schemaRegistryUrl,
      Class<?> valueSerializerClass) {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapUrl);
    properties.put(ProducerConfig.ACKS_CONFIG, "all");
    properties.put(ProducerConfig.RETRIES_CONFIG, 1);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass.getName());
    properties.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    properties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
    return properties;
  }

  public static Properties consumerProperties(String clientId, String groupId, Class<?> valueSerializerClass) {
    return consumerProperties(clientId, groupId, String.format("http://localhost:%s", kafkaBrokerPort),
        String.format("http://localhost:%s", schemaRegistryPort), valueSerializerClass);
  }

  public static Properties consumerProperties(String clientId, String groupId, String bootstrapUrl,
      String schemaRegistryUrl, Class<?> valueSerializerClass) {
    Properties properties = new Properties();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapUrl);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueSerializerClass.getName());

    if (valueSerializerClass.equals(KafkaAvroDeserializer.class)) {
      properties.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    }
    // required for casting the event to TestEvents and not using
    // org.apache.avro.generic.GenericData$Record
    properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG,
        valueSerializerClass.equals(KafkaAvroDeserializer.class) ? true : false);
    properties.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);
    return properties;
  }

  /**
   * Returns up to `maxMessages` message-values from the topic.
   *
   * @param topic
   *          Kafka topic to read messages from
   * @param consumerConfig
   *          Kafka consumer configuration
   * @param maxMessages
   *          Maximum number of messages to read via the consumer.
   * @return The values retrieved via the consumer.
   */
  public static <K, V> List<V> readValues(String topic, Properties consumerConfig, int maxMessages) {
    List<KeyValue<K, V>> kvs = readKeyValues(topic, consumerConfig, maxMessages);
    return kvs.stream().map(kv -> kv.value).collect(Collectors.toList());
  }

  /**
   * Returns as many messages as possible from the topic until a (currently
   * hardcoded) timeout is reached.
   *
   * @param topic
   *          Kafka topic to read messages from
   * @param consumerConfig
   *          Kafka consumer configuration
   * @return The KeyValue elements retrieved via the consumer.
   */
  public static <K, V> List<KeyValue<K, V>> readKeyValues(String topic, Properties consumerConfig) {
    return readKeyValues(topic, consumerConfig, UNLIMITED_MESSAGES);
  }

  /**
   * Returns up to `maxMessages` by reading via the provided consumer (the
   * topic(s) to read from are already configured in the consumer).
   *
   * @param topic
   *          Kafka topic to read messages from
   * @param consumerConfig
   *          Kafka consumer configuration
   * @param maxMessages
   *          Maximum number of messages to read via the consumer
   * @return The KeyValue elements retrieved via the consumer
   */
  public static <K, V> List<KeyValue<K, V>> readKeyValues(String topic, Properties consumerConfig, int maxMessages) {
    KafkaConsumer<K, V> consumer = new KafkaConsumer<>(consumerConfig);
    consumer.subscribe(Collections.singletonList(topic));
    int pollIntervalMs = 100;
    int maxTotalPollTimeMs = 2000;
    int totalPollTimeMs = 0;
    List<KeyValue<K, V>> consumedValues = new ArrayList<>();
    while (totalPollTimeMs < maxTotalPollTimeMs && continueConsuming(consumedValues.size(), maxMessages)) {
      totalPollTimeMs += pollIntervalMs;
      ConsumerRecords<K, V> records = consumer.poll(pollIntervalMs);
      for (ConsumerRecord<K, V> record : records) {
        consumedValues.add(new KeyValue<>(record.key(), record.value()));
      }
    }
    consumer.close();
    return consumedValues;
  }

  private static boolean continueConsuming(int messagesConsumed, int maxMessages) {
    return maxMessages <= 0 || messagesConsumed < maxMessages;
  }

  /**
   * @param topic
   *          Kafka topic to write the data records to
   * @param records
   *          Data records to write to Kafka
   * @param producerConfig
   *          Kafka producer configuration
   * @param <K>
   *          Key type of the data records
   * @param <V>
   *          Value type of the data records
   */
  public static <K, V> void produceKeyValuesSynchronously(String topic, Collection<KeyValue<K, V>> records,
      Properties producerConfig) throws ExecutionException, InterruptedException {
    Producer<K, V> producer = new KafkaProducer<>(producerConfig);
    for (KeyValue<K, V> record : records) {
      Future<RecordMetadata> f = producer.send(new ProducerRecord<>(topic, record.key, record.value));
      f.get();
    }
    producer.flush();
    producer.close();
  }

  public static <K, V> List<KeyValue<K, V>> waitUntilMinKeyValueRecordsReceived(Properties consumerConfig, String topic,
      int expectedNumRecords) throws InterruptedException {

    return waitUntilMinKeyValueRecordsReceived(consumerConfig, topic, expectedNumRecords, DEFAULT_TIMEOUT);
  }

  /**
   * Wait until enough data (key-value records) has been consumed.
   *
   * @param consumerConfig
   *          Kafka Consumer configuration
   * @param topic
   *          Topic to consume from
   * @param expectedNumRecords
   *          Minimum number of expected records
   * @param waitTime
   *          Upper bound in waiting time in milliseconds
   * @return All the records consumed, or null if no records are consumed
   * @throws AssertionError
   *           if the given wait time elapses
   */
  public static <K, V> List<KeyValue<K, V>> waitUntilMinKeyValueRecordsReceived(Properties consumerConfig, String topic,
      int expectedNumRecords, long waitTime) throws InterruptedException {
    List<KeyValue<K, V>> accumData = new ArrayList<>();
    long startTime = System.currentTimeMillis();
    while (true) {
      List<KeyValue<K, V>> readData = readKeyValues(topic, consumerConfig);
      accumData.addAll(readData);
      if (accumData.size() >= expectedNumRecords)
        return accumData;
      if (System.currentTimeMillis() > startTime + waitTime)
        throw new AssertionError("Expected " + expectedNumRecords + " but received only " + accumData.size()
            + " records before timeout " + waitTime + " ms");
      Thread.sleep(Math.min(waitTime, 100L));
    }
  }

  public static <V> List<V> waitUntilMinValuesRecordsReceived(Properties consumerConfig, String topic,
      int expectedNumRecords) throws InterruptedException {

    return waitUntilMinValuesRecordsReceived(consumerConfig, topic, expectedNumRecords, DEFAULT_TIMEOUT, true);
  }

  public static <V> List<V> waitUntilMinValuesRecordsReceived(Properties consumerConfig, String topic,
      int expectedNumRecords, boolean throwAssertionError) throws InterruptedException {

    return waitUntilMinValuesRecordsReceived(consumerConfig, topic, expectedNumRecords, DEFAULT_TIMEOUT,
        throwAssertionError);
  }

  /**
   * Wait until enough data (value records) has been consumed.
   *
   * @param consumerConfig
   *          Kafka Consumer configuration
   * @param topic
   *          Topic to consume from
   * @param expectedNumRecords
   *          Minimum number of expected records
   * @param waitTime
   *          Upper bound in waiting time in milliseconds
   * @param throwAssertionError
   *          if true throw AssertionError if expectedNumRecords is not received
   * @return All the records consumed, or null if no records are consumed
   * @throws AssertionError
   *           if the given wait time elapses
   */
  public static <V> List<V> waitUntilMinValuesRecordsReceived(Properties consumerConfig, String topic,
      int expectedNumRecords, long waitTime, boolean throwAssertionError) throws InterruptedException {
    List<V> accumData = new ArrayList<>();
    long startTime = System.currentTimeMillis();
    while (true) {
      List<V> readData = readValues(topic, consumerConfig, expectedNumRecords);
      accumData.addAll(readData);
      if (accumData.size() >= expectedNumRecords)
        return accumData;
      if (System.currentTimeMillis() > startTime + waitTime && throwAssertionError)
        throw new AssertionError("Expected " + expectedNumRecords + " but received only " + accumData.size()
            + " records before timeout " + waitTime + " ms");
      if (System.currentTimeMillis() > startTime + waitTime && !throwAssertionError)
        return accumData;
      Thread.sleep(Math.min(waitTime, 100L));
    }
  }

  /**
   * Wait until enough data (value records) has been consumed and throw
   * AssertionError if expectedNumRecords is not received
   *
   * @param consumerConfig
   *          Kafka Consumer configuration
   * @param topic
   *          Topic to consume from
   * @param expectedNumRecords
   *          Minimum number of expected records
   * @param waitTime
   *          Upper bound in waiting time in milliseconds
   * @return All the records consumed, or null if no records are consumed
   * @throws AssertionError
   *           if the given wait time elapses
   */
  public static <V> List<V> waitUntilMinValuesRecordsReceived(Properties consumerConfig, String topic,
      int expectedNumRecords, long waitTime) throws InterruptedException {
    return waitUntilMinValuesRecordsReceived(consumerConfig, topic, expectedNumRecords, waitTime, true);
  }

}
