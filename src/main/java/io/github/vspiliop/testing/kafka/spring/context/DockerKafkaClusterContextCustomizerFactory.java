package io.github.vspiliop.testing.kafka.spring.context;

import java.util.List;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * The {@link ContextCustomizerFactory} implementation to produce a
 * {@link DockerKafkaClusterContextCustomizer} if a {@link DockerKafkaCluster} annotation
 * is present on the test class.
 *
 */
class DockerKafkaClusterContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		DockerKafkaCluster DockerKafkaCluster =
				AnnotatedElementUtils.findMergedAnnotation(testClass, DockerKafkaCluster.class);
		return DockerKafkaCluster != null ? new DockerKafkaClusterContextCustomizer(DockerKafkaCluster) : null;
	}

}
