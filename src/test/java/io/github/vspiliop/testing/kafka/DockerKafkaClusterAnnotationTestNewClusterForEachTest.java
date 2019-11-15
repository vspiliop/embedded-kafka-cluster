package io.github.vspiliop.testing.kafka;

import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

/**
 * 
 * By using @DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD) the cluster is separate for each test.
 * 
 * @author spiliopoulosv
 *
 */
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class DockerKafkaClusterAnnotationTestNewClusterForEachTest extends TestParent {

	@Test
	public void shouldBeAbleToProduceAndConsumeAvroEvents() throws Exception {
		sendAnEventAndConsumeIt();
	}

	/**
	 * Execute a couple of tests to verify that the cluster starts and stops
	 * properly between jUnit tests.
	 * 
	 * All tests use the same consumer-group-id (i.e. test-consumer). As a result,
	 * no message from previous tests are reconsumed, as each junit test has a new
	 * unique cluster.
	 */
	@Test
	public void clusterShouldStopAndStartForEachUnitTest_1() throws Exception {
		sendAnEventAndConsumeIt();
	}

	@Test
	public void clusterShouldStopAndStartForEachUnitTest_2() throws Exception {
		sendAnEventAndConsumeIt();
	}
}
