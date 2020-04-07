package io.github.vspiliop.testing.kafka.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import io.github.vspiliop.testing.kafka.cluster.DockerKafkaClusterFacade;
import org.apache.kafka.streams.KeyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import io.github.vspiliop.schema.test.TestCreated;
import io.github.vspiliop.schema.test.TestEvents;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.When;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.github.vspiliop.testing.kafka.util.IntegrationTestUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ContextConfiguration()
@io.github.vspiliop.testing.kafka.spring.context.DockerKafkaCluster(topics = {"test.t"}, brokersCount = 1, zookeepersCount = 1, schemaRegistriesCount = 1)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class FeatureSteps {

	@Autowired
	private DockerKafkaClusterFacade embeddedKafkaCluster;
	
	String brokers;
	
	String schemaRegistries;
	
	TestEvents event1;

	@Before()
	public void beforeScenario(Scenario scenario) throws IOException {
		log.debug("before each scenario..");
		brokers = embeddedKafkaCluster.getKafkaBootstapServers();
		schemaRegistries = embeddedKafkaCluster.getSchemaRegistryUrls();
		log.debug("brokers: {}", brokers);
		log.debug("schemaRegistries: {}", schemaRegistries);
	}

	@After()
	public void afterScenario() {
		log.debug("after each scenario..");
	}

	@When("^a single test event is produced to \"test.t\" topic$")
	public void eventIsProduced() throws Exception {
		log.debug("brokers: {}", brokers);
		log.debug("schemaRegistries: {}", schemaRegistries);
		
		event1 = TestEvents.newBuilder().setCorrelationId(UUID.randomUUID().toString())
				.setType("testCreated")
				.setPayload(TestCreated.newBuilder().setTestNumber("test1").setText("this is the create").build())
				.build();
		
		List<KeyValue<String, TestEvents>> inputEvents = Arrays.asList(new KeyValue<String, TestEvents>("A", event1));
		Properties producerConfig = IntegrationTestUtils.producerProperties("unit-test-producer", brokers,
				schemaRegistries, KafkaAvroSerializer.class);
		IntegrationTestUtils.produceKeyValuesSynchronously("test.t", inputEvents, producerConfig);
	}

	@When("^that specific test event is consumed by consumer with groupId \"test-consumer\"$")
	public void specificEventIsConsumed() throws Exception {
		String brokers = embeddedKafkaCluster.getKafkaBootstapServers();
		String schemaRegistries = embeddedKafkaCluster.getSchemaRegistryUrls();
		
		Properties consumerConfig = IntegrationTestUtils.consumerProperties("unit-test-consumer",
				"test-consumer", brokers, schemaRegistries, KafkaAvroDeserializer.class);
		// wait for either 2 events to be consumed or for a max of 5 seconds
		List<TestEvents> eventValues = IntegrationTestUtils.waitUntilMinValuesRecordsReceived(consumerConfig, "test.t",
				2, 5000, false);
		// verify that all messages from previous tests are reconsumed
		assertThat(eventValues.size()).isEqualTo(1);
		assertThat(eventValues.get(0)).isEqualTo(event1);
	}

	@When("^no other event is consumed$")
	public void noOtherEventIsConsumed() throws Exception {
		// this is verified by specificEventIsConsumed()
	}
}
