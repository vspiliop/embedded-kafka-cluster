package com.etiology.testing.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.streams.KeyValue;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.etiology.testing.kafka.junit.rule.EmbeddedMultiNodeKafkaCluster;
import com.etiology.testing.kafka.spring.context.EmbeddedKafkaCluster;
import com.etiology.testing.kafka.util.IntegrationTestUtils;
import com.etiology.schema.test.TestCreated;
import com.etiology.schema.test.TestEvents;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@EmbeddedKafkaCluster(topics = {"test.t"})
@ContextConfiguration(classes = {KafkaAdminConfiguration.class})
@Slf4j
abstract public class TestParent {

	@Autowired
	private EmbeddedMultiNodeKafkaCluster embeddedKafkaCluster;
	
	public void sendAnEventAndConsumeIt() throws Exception {
		sendAnEventAndConsumeIt(0);
	}
	
	protected void sendAnEventAndConsumeIt(int consumerGrouIdPrefix) throws Exception {
		String brokers = embeddedKafkaCluster.getKafkaBootstapServers();
		String schemaRegistries = embeddedKafkaCluster.getSchemaRegistryUrls();
		
		log.debug("brokers: {}", brokers);
		log.debug("schemaRegistries: {}", schemaRegistries);

		TestEvents event1 = TestEvents.newBuilder().setCorrelationId(UUID.randomUUID().toString())
				.setType("testCreated")
				.setPayload(TestCreated.newBuilder().setTestNumber("test1").setText("this is the create").build())
				.build();

		List<KeyValue<String, TestEvents>> inputEvents = Arrays.asList(new KeyValue<String, TestEvents>("A", event1));
		//
		// Step 1: Produce one event
		//
		Properties producerConfig = IntegrationTestUtils.producerProperties("unit-test-producer", brokers,
				schemaRegistries, KafkaAvroSerializer.class);
		// produce with key
		IntegrationTestUtils.produceKeyValuesSynchronously("test.t", inputEvents, producerConfig);
		//
		// Step 2: Verify that the event has reached "test.t"
		//
		Properties consumerConfig = IntegrationTestUtils.consumerProperties("unit-test-consumer", "test-consumer-" + consumerGrouIdPrefix,
				brokers, schemaRegistries, KafkaAvroDeserializer.class);
		// wait for either 2 events to be consumed or for a max of 5 seconds
		List<TestEvents> eventValues = IntegrationTestUtils.waitUntilMinValuesRecordsReceived(consumerConfig, "test.t", 2, 5000, false);
		// verify that all messages from previous tests are reconsumed
		assertThat(eventValues.size()).isEqualTo(consumerGrouIdPrefix + 1);
	}

}
