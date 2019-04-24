package io.github.vspiliop.testing.kafka.spring.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;

import io.github.vspiliop.testing.kafka.junit.rule.EmbeddedMultiNodeKafkaCluster;
import lombok.extern.slf4j.Slf4j;

/**
 * The {@link ContextCustomizer} implementation for Spring Integration specific
 * environment.
 * <p>
 * Registers {@link EmbeddedKafkaCluster} bean.
 *
 */
@Slf4j
class EmbeddedKafkaContextCustomizer implements ContextCustomizer {

	private final EmbeddedKafkaCluster embeddedKafka;

	EmbeddedKafkaContextCustomizer(EmbeddedKafkaCluster embeddedKafka) {
		this.embeddedKafka = embeddedKafka;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		
		assertThat(embeddedKafka.brokersCount()).isEqualTo(embeddedKafka.value());
		assertThat(embeddedKafka.brokersCount()).isGreaterThanOrEqualTo(embeddedKafka.minInSyncReplicas());
		assertThat(embeddedKafka.brokersCount()).isGreaterThanOrEqualTo(embeddedKafka.minTransactionInSynceReplicas());
		assertThat(embeddedKafka.brokersCount()).isGreaterThanOrEqualTo(embeddedKafka.defaultReplicationFactor());
		assertThat(embeddedKafka.brokersCount()).isGreaterThanOrEqualTo(embeddedKafka.offsetsReplicationFactor());
		assertThat(embeddedKafka.brokersCount()).isGreaterThanOrEqualTo(embeddedKafka.transactionReplicationFactor());
		assertThat(embeddedKafka.brokersCount()).isGreaterThanOrEqualTo(embeddedKafka.schemaRegistryReplicationFactor());
		
		assertThat(embeddedKafka.minInSyncReplicas()).isLessThanOrEqualTo(embeddedKafka.defaultReplicationFactor());
		assertThat(embeddedKafka.minInSyncReplicas()).isLessThanOrEqualTo(embeddedKafka.offsetsReplicationFactor());
		assertThat(embeddedKafka.minInSyncReplicas()).isLessThanOrEqualTo(embeddedKafka.transactionReplicationFactor());
		assertThat(embeddedKafka.minInSyncReplicas()).isLessThanOrEqualTo(embeddedKafka.schemaRegistryReplicationFactor());
		
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		Assert.isInstanceOf(DefaultSingletonBeanRegistry.class, beanFactory);

		EmbeddedMultiNodeKafkaCluster embeddedSingleNodeKafkaCluster = new EmbeddedMultiNodeKafkaCluster(embeddedKafka);

		beanFactory.initializeBean(embeddedSingleNodeKafkaCluster, EmbeddedMultiNodeKafkaCluster.BEAN_NAME);
		beanFactory.registerSingleton(EmbeddedMultiNodeKafkaCluster.BEAN_NAME, embeddedSingleNodeKafkaCluster);
		((DefaultSingletonBeanRegistry) beanFactory).registerDisposableBean(EmbeddedMultiNodeKafkaCluster.BEAN_NAME,
				embeddedSingleNodeKafkaCluster);
		// here the cluster is 100% up and we may reconfigure the application test properties and register topics for creation
		registerTopicsForCreationByKafkaAdmin(beanFactory, embeddedSingleNodeKafkaCluster, context);
		reconfigureTestContextProperties(context, embeddedSingleNodeKafkaCluster);
	}
	
	private void reconfigureTestContextProperties(ConfigurableApplicationContext context, EmbeddedMultiNodeKafkaCluster embeddedSingleNodeKafkaCluster) {
		TestPropertyValues values = TestPropertyValues.of(
				embeddedKafka.kafkaServersProperty() + "=" + embeddedSingleNodeKafkaCluster.getKafkaBootstapServers(),
				embeddedKafka.schemaRegistryServersProperty() + "=" + embeddedSingleNodeKafkaCluster.getSchemaRegistryUrls());
		values.applyTo(context);
	}
	
	private void registerTopicsForCreationByKafkaAdmin(ConfigurableListableBeanFactory beanFactory, EmbeddedMultiNodeKafkaCluster embeddedSingleNodeKafkaCluster, ConfigurableApplicationContext context) {
		ConfigurableEnvironment environment = context.getEnvironment();
		Stream.of(embeddedKafka.topics())
						.map(environment::resolvePlaceholders)
						.map(topic -> new NewTopic(topic, 1, (short) embeddedKafka.defaultReplicationFactor()))
						.forEach(newTopic -> { 
								String topicBeanName = newTopic.name() + UUID.randomUUID();
								beanFactory.initializeBean(newTopic, topicBeanName);
								beanFactory.registerSingleton(topicBeanName, newTopic);
								log.debug("registered topic {} for creation by KafkaAdmin", newTopic.name());
						});
	}
}