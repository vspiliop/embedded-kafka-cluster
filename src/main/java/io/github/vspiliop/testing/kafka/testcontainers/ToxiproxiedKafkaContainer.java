package io.github.vspiliop.testing.kafka.testcontainers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.images.builder.Transferable;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToxiproxiedKafkaContainer extends KafkaContainer {

  private int port = PORT_NOT_ASSIGNED;

  private static final int PORT_NOT_ASSIGNED = -1;

  private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

  /**
   * The toxiproxy for this container for broker-to-broker communication on 9092.
   */
  private ToxiproxyContainer.ContainerProxy proxyInternalBrokerCommunication = null;

  /**
   * The toxiproxy for this container for external client communication on 9093.
   */
  private ToxiproxyContainer.ContainerProxy proxyExternalBrokerCommunication = null;

  private ToxiproxyContainer toxiproxyContainer = null;

  public ToxiproxiedKafkaContainer(String confluentPlatformVersion) {
    super(confluentPlatformVersion);
    toxiproxyContainer = new ToxiproxyContainer().withNetwork(getNetwork());
    dependsOn(toxiproxyContainer);
  }

  /**
   * <p>
   * When the leader broker is accessed on 9092 (by a follower broker) the corresponding advertised listener will
   * redirect to the proxyInternalBrokerCommunication of the current broker.
   *</p>
   * <p>
   * When the leader broker is accessed on 9093 (external client) the corresponding advertised listener will
   * redirect to the proxyExternalBrokerCommunication of the current broker.
   *</p>
   *
   * @param containerInfo
   */
//  @Override
//  protected void containerIsStarting(InspectContainerResponse containerInfo) {
//    try {
//      port = getMappedPort(KAFKA_PORT);
//
//      proxyInternalBrokerCommunication = toxiproxyContainer.getProxy(this, 9092);
//      proxyExternalBrokerCommunication = toxiproxyContainer.getProxy(this, port);
//
//      final String zookeeperConnect;
//      if (externalZookeeperConnect != null) {
//        zookeeperConnect = externalZookeeperConnect;
//      } else {
//        zookeeperConnect = startZookeeper();
//      }
//      ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(getContainerId())
//        .withCmd("sh", "-c", "export KAFKA_ZOOKEEPER_CONNECT=" + zookeeperConnect + "\nexport KAFKA_ADVERTISED_LISTENERS="
//          + getBootstrapServers() + "," + String.format("BROKER://%s:%s", proxyInternalBrokerCommunication.getContainerIpAddress(), proxyInternalBrokerCommunication.getProxyPort()) + "\n/etc/confluent/docker/run").exec();
//      dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ExecStartResultCallback()).awaitStarted(10, TimeUnit.SECONDS);
//    } catch (final java.lang.Throwable $ex) {
//      throw lombok.Lombok.sneakyThrow($ex);
//    }
//  }

  @Override
  @SneakyThrows
  protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
    super.containerIsStarting(containerInfo, reused);

    port = getMappedPort(KAFKA_PORT);

//    proxyInternalBrokerCommunication = toxiproxyContainer.getProxy(this, 9092);
    proxyExternalBrokerCommunication = toxiproxyContainer.getProxy(this, port);

    if (reused) {
      return;
    }

    String command = "#!/bin/bash\n";
    final String zookeeperConnect;
    if (externalZookeeperConnect != null) {
      zookeeperConnect = externalZookeeperConnect;
    } else {
      zookeeperConnect = "localhost:" + ZOOKEEPER_PORT;
      command += "echo 'clientPort=" + ZOOKEEPER_PORT + "' > zookeeper.properties\n";
      command += "echo 'dataDir=/var/lib/zookeeper/data' >> zookeeper.properties\n";
      command += "echo 'dataLogDir=/var/lib/zookeeper/log' >> zookeeper.properties\n";
      command += "zookeeper-server-start zookeeper.properties &\n";
    }

    command += "export KAFKA_ZOOKEEPER_CONNECT='" + zookeeperConnect + "'\n";
    command += "export KAFKA_ADVERTISED_LISTENERS='" + Stream
      .concat(
        Stream.of(getBootstrapServers()),
        containerInfo.getNetworkSettings().getNetworks().values().stream()
          .map(it -> "BROKER://" + it.getIpAddress() + ":9092")
      )
      .collect(Collectors.joining(",")) + "'\n";

    command += ". /etc/confluent/docker/bash-config \n";
    command += "/etc/confluent/docker/configure \n";
    command += "/etc/confluent/docker/launch \n";

    copyFileToContainer(
      Transferable.of(command.getBytes(StandardCharsets.UTF_8), 0777),
      STARTER_SCRIPT
    );
  }

  public String getBootstrapServers() {
    if (port == PORT_NOT_ASSIGNED) {
      throw new IllegalStateException("You should start Kafka container first");
    }
    return String.format("PLAINTEXT://%s:%s", proxyExternalBrokerCommunication.getContainerIpAddress(), proxyExternalBrokerCommunication.getProxyPort());
  }

//  private String startZookeeper() {
//    try {
//      ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(getContainerId()).withCmd("sh", "-c", "printf \'clientPort=" + ZOOKEEPER_PORT + "\ndataDir=/var/lib/zookeeper/data\ndataLogDir=/var/lib/zookeeper/log\' > /zookeeper.properties\nzookeeper-server-start /zookeeper.properties\n").exec();
//      dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ExecStartResultCallback()).awaitStarted(10, TimeUnit.SECONDS);
//      return "localhost:" + ZOOKEEPER_PORT;
//    } catch (final InterruptedException $ex) {
//      throw lombok.Lombok.sneakyThrow($ex);
//    }
//  }

  public ToxiproxyContainer.ContainerProxy getProxyExternalBrokerCommunication() {
    return proxyExternalBrokerCommunication;
  }

  public ToxiproxyContainer.ContainerProxy getProxyInternalBrokerCommunication() {
    return proxyInternalBrokerCommunication;
  }
}

