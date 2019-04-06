Feature: Using the Embedded Kafka Cluster Annotation with Cucumber
  Description: it does work

  Scenario: Produce and consume a single event
    When a single test event is produced to "test.t" topic
    Then that specific test event is consumed by consumer with groupId "test-consumer"
     And no other event is consumed
     
  Scenario: Run the same scenario again in a brand new cluster for each feature, using the same groupId
    When a single test event is produced to "test.t" topic
    Then that specific test event is consumed by consumer with groupId "test-consumer"
     And no other event is consumed
