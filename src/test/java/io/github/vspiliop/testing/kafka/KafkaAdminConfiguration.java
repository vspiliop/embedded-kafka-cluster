package io.github.vspiliop.testing.kafka;

import java.util.HashMap;
import java.util.Map;

import io.github.vspiliop.testing.kafka.cluster.DockerKafkaClusterFacade;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import lombok.extern.slf4j.Slf4j;

/**
 * KafkaAdmin is required for the test topics to be auto-created.
 * 
 * @author SPILIOPOULOSV
 *
 */
@Slf4j
@Configuration
public class KafkaAdminConfiguration {
	
	@Autowired
  DockerKafkaClusterFacade dockerKafkaClusterFacade;
	
	@Bean
	public KafkaAdmin kafkaAdmin() {
		log.debug("Creating test KafkaAdmin.");
	    Map<String, Object> configs = new HashMap<>();
	    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, dockerKafkaClusterFacade.getKafkaBootstapServers());
	    configs.put(AdminClientConfig.CLIENT_ID_CONFIG, "test-kafka-admin-client-id");
	    return new KafkaAdmin(configs);
	}

}
