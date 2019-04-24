package io.github.vspiliop.testing.kafka.spring.context;

import java.util.List;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * The {@link ContextCustomizerFactory} implementation to produce a
 * {@link EmbeddedKafkaContextCustomizer} if a {@link EmbeddedKafkaCluster} annotation
 * is present on the test class.
 *
 */
class EmbeddedKafkaContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		EmbeddedKafkaCluster EmbeddedKafkaCluster =
				AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedKafkaCluster.class);
		return EmbeddedKafkaCluster != null ? new EmbeddedKafkaContextCustomizer(EmbeddedKafkaCluster) : null;
	}

}
