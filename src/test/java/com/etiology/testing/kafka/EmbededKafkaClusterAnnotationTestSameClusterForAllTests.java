package com.etiology.testing.kafka;

import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

/**
 * 
 * By using @DirtiesContext(classMode = ClassMode.AFTER_CLASS) the cluster is reused for all junit tests.
 * 
 * @author spiliopoulosv
 *
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class EmbededKafkaClusterAnnotationTestSameClusterForAllTests extends TestParent {

	@Test
	public void shouldBeAbleToProduceAndConsumeAvroEvents() throws Exception {
		sendAnEventAndConsumeIt();
	}
	
	/**
	 * Execute a couple of tests to verify that the cluster starts and stops properly between jUnit tests. 
	 * 
	 * All tests use a different consumer-group-id (i.e. test-consumer-"prefix"). As a result, all messages
	 * from previous tests are reconsumed. 
	 */
	@Test
	public void clusterShouldStopAndStartForEachUnitTest_1() throws Exception {
		sendAnEventAndConsumeIt(1);
	}
	
	@Test
	public void clusterShouldStopAndStartForEachUnitTest_2() throws Exception {
		sendAnEventAndConsumeIt(2);
	}
	
	@Test
	public void clusterShouldStopAndStartForEachUnitTest_3() throws Exception {
		sendAnEventAndConsumeIt(3);
	}
	
	@Test
	public void clusterShouldStopAndStartForEachUnitTest_4() throws Exception {
		sendAnEventAndConsumeIt(4);
	}
}
