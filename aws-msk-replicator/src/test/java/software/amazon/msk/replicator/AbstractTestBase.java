package software.amazon.msk.replicator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.common.collect.Sets;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.awssdk.services.kafka.model.*;
import software.amazon.awssdk.services.kafka.model.AmazonMskCluster;
import software.amazon.awssdk.services.kafka.model.ConsumerGroupReplication;
import software.amazon.awssdk.services.kafka.model.KafkaCluster;
import software.amazon.awssdk.services.kafka.model.KafkaClusterClientVpcConfig;
import software.amazon.awssdk.services.kafka.model.ReplicationInfo;
import software.amazon.awssdk.services.kafka.model.TopicReplication;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;

  protected static final String CLIENT_REQUEST_TOKEN = "ClientToken";
  protected static final String REPLICATOR_NAME = "ReplicatorName";
  protected static final String REPLICATOR_ARN = "arn:aws:kafka:us-east-1:083674906042:replicator/ReplicatorName";
  protected static final String SERVICE_EXECUTION_ROLE_ARN = "arn:aws:iam::083674906042:role/service-role/ServiceExecutionRole";
  protected static final String SOURCE_KAFKA_CLUSTER_ALIAS = "Source";
  protected static final String DESTINATION_KAFKA_CLUSTER_ALIAS = "Destination";
  protected static final String SOURCE_MSK_CLUSTER_ARN = "arn:aws:kafka:us-west-2:083674906042:cluster/SourceCluster";
  protected static final String DESTINATION_MSK_CLUSTER_ARN = "arn:aws:kafka:us-west-2:083674906042:cluster/DestinationCluster";
  protected static final String CONSUMER_GROUP_TO_EXCLUDE = "consumer-group-exclude";
  protected static final String CONSUMER_GROUP_TO_REPLICATE = "consumer-group-replicate";
  protected static final String TOPIC_TO_EXCLUDE = "topic-exclude";
  protected static final String TOPIC_TO_REPLICATE = "topic-replicate";
  protected static final String TARGET_COMPRESSION_TYPE = "GZIP";
  protected static final Map<String, String> TAGS = new HashMap<String, String>() {
      {
          put("TEST_TAG1", "TEST_TAG_VALUE1");
          put("TEST_TAG2", "TEST_TAG_VALUE2");
      }
  };

  protected static final AmazonMskCluster SOURCE_AMAZON_MSK_CLUSTER =
      AmazonMskCluster.builder()
          .mskClusterArn(SOURCE_MSK_CLUSTER_ARN)
          .build();
  protected static final AmazonMskCluster DESTINATION_AMAZON_MSK_CLUSTER =
      AmazonMskCluster.builder()
          .mskClusterArn(DESTINATION_MSK_CLUSTER_ARN)
          .build();
  protected static final Set<String> SUBNET_IDS =  Sets.newHashSet("Subnet-1", "Subnet-2");
  protected static final Set<String> SECURITY_GROUP_IDS = Sets.newHashSet("SecurityGroup-1");
  protected static final KafkaClusterClientVpcConfig CLUSTER_VPC_CONFIG =
      KafkaClusterClientVpcConfig.builder()
      .securityGroupIds(SECURITY_GROUP_IDS)
      .subnetIds(SUBNET_IDS)
      .build();
  protected static final KafkaCluster SOURCE_KAFKA_CLUSTER =
      KafkaCluster.builder()
          .amazonMskCluster(SOURCE_AMAZON_MSK_CLUSTER)
          .vpcConfig(CLUSTER_VPC_CONFIG)
          .build();
  protected static final KafkaCluster DESTINATION_KAFKA_CLUSTER =
      KafkaCluster.builder()
          .amazonMskCluster(DESTINATION_AMAZON_MSK_CLUSTER)
          .vpcConfig(CLUSTER_VPC_CONFIG)
          .build();
  protected static final KafkaClusterSummary SOURCE_KAFKA_CLUSTER_SUMMARY =
      KafkaClusterSummary.builder()
        .amazonMskCluster(SOURCE_AMAZON_MSK_CLUSTER)
        .kafkaClusterAlias(SOURCE_KAFKA_CLUSTER_ALIAS)
        .build();
  protected static final KafkaClusterSummary DESTINATION_KAFKA_CLUSTER_SUMMARY =
      KafkaClusterSummary.builder()
        .amazonMskCluster(DESTINATION_AMAZON_MSK_CLUSTER)
        .kafkaClusterAlias(DESTINATION_KAFKA_CLUSTER_ALIAS)
        .build();
  protected static final KafkaClusterDescription SOURCE_KAFKA_CLUSTER_DESCRIPTION =
      KafkaClusterDescription.builder()
          .amazonMskCluster(SOURCE_AMAZON_MSK_CLUSTER)
          .kafkaClusterAlias(SOURCE_KAFKA_CLUSTER_ALIAS)
          .vpcConfig(CLUSTER_VPC_CONFIG)
          .build();
  protected static final KafkaClusterDescription DESTINATION_KAFKA_CLUSTER_DESCRIPTION =
      KafkaClusterDescription.builder()
          .amazonMskCluster(DESTINATION_AMAZON_MSK_CLUSTER)
          .kafkaClusterAlias(DESTINATION_KAFKA_CLUSTER_ALIAS)
          .vpcConfig(CLUSTER_VPC_CONFIG)
          .build();
  protected static final Collection<KafkaCluster> KAFKA_CLUSTERS = Sets.newHashSet(SOURCE_KAFKA_CLUSTER, DESTINATION_KAFKA_CLUSTER);
  protected static final Collection<KafkaClusterSummary> KAFKA_CLUSTER_SUMMARIES = Sets.newHashSet(SOURCE_KAFKA_CLUSTER_SUMMARY, DESTINATION_KAFKA_CLUSTER_SUMMARY);
  protected static final Collection<KafkaClusterDescription> KAFKA_CLUSTER_DESCRIPTIONS = Sets.newHashSet(SOURCE_KAFKA_CLUSTER_DESCRIPTION, DESTINATION_KAFKA_CLUSTER_DESCRIPTION);
  protected static final ConsumerGroupReplication CONSUMER_GROUP_REPLICATION =
      ConsumerGroupReplication.builder()
          .consumerGroupsToExclude(CONSUMER_GROUP_TO_EXCLUDE)
          .consumerGroupsToReplicate(CONSUMER_GROUP_TO_REPLICATE)
          .detectAndCopyNewConsumerGroups(false)
          .synchroniseConsumerGroupOffsets(false)
          .build();
  protected static final TopicReplication TOPIC_REPLICATION =
      TopicReplication.builder()
          .topicsToExclude(TOPIC_TO_EXCLUDE)
          .topicsToReplicate(TOPIC_TO_REPLICATE)
          .detectAndCopyNewTopics(false)
          .copyTopicConfigurations(false)
          .copyAccessControlListsForTopics(false)
          .build();
  protected static final TopicReplication UPDATED_TOPIC_REPLICATION =
      TopicReplication.builder()
          .topicsToExclude(TOPIC_TO_EXCLUDE)
          .topicsToReplicate(TOPIC_TO_REPLICATE)
          .detectAndCopyNewTopics(true)
          .copyTopicConfigurations(true)
          .copyAccessControlListsForTopics(true)
          .build();
  protected static final ReplicationInfoSummary REPLICATION_INFO_SUMMARY =
      ReplicationInfoSummary.builder()
          .sourceKafkaClusterAlias(SOURCE_KAFKA_CLUSTER_ALIAS)
          .targetKafkaClusterAlias(DESTINATION_KAFKA_CLUSTER_ALIAS)
          .build();
  protected static final ReplicationInfoDescription REPLICATION_INFO_DESCRIPTION =
      ReplicationInfoDescription.builder()
          .sourceKafkaClusterAlias(SOURCE_KAFKA_CLUSTER_ALIAS)
          .targetKafkaClusterAlias(DESTINATION_KAFKA_CLUSTER_ALIAS)
          .targetCompressionType(TARGET_COMPRESSION_TYPE)
          .consumerGroupReplication(CONSUMER_GROUP_REPLICATION)
          .topicReplication(TOPIC_REPLICATION)
          .build();
  protected static final ReplicationInfoDescription UPDATED_REPLICATION_INFO_DESCRIPTION =
      ReplicationInfoDescription.builder()
          .sourceKafkaClusterAlias(SOURCE_KAFKA_CLUSTER_ALIAS)
          .targetKafkaClusterAlias(DESTINATION_KAFKA_CLUSTER_ALIAS)
          .targetCompressionType(TARGET_COMPRESSION_TYPE)
          .consumerGroupReplication(CONSUMER_GROUP_REPLICATION)
          .topicReplication(UPDATED_TOPIC_REPLICATION)
          .build();
  protected static final ReplicationInfo REPLICATION_INFO =
      ReplicationInfo.builder()
        .sourceKafkaClusterArn(SOURCE_MSK_CLUSTER_ARN)
        .targetKafkaClusterArn(DESTINATION_MSK_CLUSTER_ARN)
        .targetCompressionType(TARGET_COMPRESSION_TYPE)
        .consumerGroupReplication(CONSUMER_GROUP_REPLICATION)
        .topicReplication(TOPIC_REPLICATION)
        .build();
  protected static final ReplicationInfo UPDATED_REPLICATION_INFO =
      ReplicationInfo.builder()
          .sourceKafkaClusterArn(SOURCE_MSK_CLUSTER_ARN)
          .targetKafkaClusterArn(DESTINATION_MSK_CLUSTER_ARN)
          .consumerGroupReplication(CONSUMER_GROUP_REPLICATION)
          .topicReplication(UPDATED_TOPIC_REPLICATION)
          .build();
  protected static final Collection<ReplicationInfoSummary> REPLICATION_INFO_SUMMARIES = Sets.newHashSet(REPLICATION_INFO_SUMMARY);
  protected static final Collection<ReplicationInfo> REPLICATION_INFOS = Sets.newHashSet(REPLICATION_INFO);
  protected static final Collection<ReplicationInfo> UPDATED_REPLICATION_INFOS = Sets.newHashSet(UPDATED_REPLICATION_INFO);

  protected static final software.amazon.msk.replicator.AmazonMskCluster SOURCE_AMAZON_MSK_CLUSTER_MODEL =
      software.amazon.msk.replicator.AmazonMskCluster.builder()
          .mskClusterArn(SOURCE_MSK_CLUSTER_ARN)
          .build();
  protected static final software.amazon.msk.replicator.AmazonMskCluster DESTINATION_AMAZON_MSK_CLUSTER_MODEL =
      software.amazon.msk.replicator.AmazonMskCluster.builder()
          .mskClusterArn(DESTINATION_MSK_CLUSTER_ARN)
          .build();
  protected static final software.amazon.msk.replicator.KafkaClusterClientVpcConfig CLUSTER_VPC_CONFIG_MODEL =
      software.amazon.msk.replicator.KafkaClusterClientVpcConfig.builder()
          .securityGroupIds(SECURITY_GROUP_IDS)
          .subnetIds(SUBNET_IDS)
          .build();
  protected static final software.amazon.msk.replicator.KafkaCluster SOURCE_KAFKA_CLUSTER_MODEL =
      software.amazon.msk.replicator.KafkaCluster.builder()
          .amazonMskCluster(SOURCE_AMAZON_MSK_CLUSTER_MODEL)
          .vpcConfig(CLUSTER_VPC_CONFIG_MODEL)
          .build();
  protected static final software.amazon.msk.replicator.KafkaCluster DESTINATION_KAFKA_CLUSTER_MODEL =
      software.amazon.msk.replicator.KafkaCluster.builder()
          .amazonMskCluster(DESTINATION_AMAZON_MSK_CLUSTER_MODEL)
          .vpcConfig(CLUSTER_VPC_CONFIG_MODEL)
          .build();
  protected static final Set<software.amazon.msk.replicator.KafkaCluster> KAFKA_CLUSTERS_MODEL = Sets.newHashSet(SOURCE_KAFKA_CLUSTER_MODEL, DESTINATION_KAFKA_CLUSTER_MODEL);
  protected static final software.amazon.msk.replicator.ConsumerGroupReplication CONSUMER_GROUP_REPLICATION_MODEL =
      software.amazon.msk.replicator.ConsumerGroupReplication.builder()
          .consumerGroupsToExclude(Sets.newHashSet(CONSUMER_GROUP_TO_EXCLUDE))
          .consumerGroupsToReplicate(Sets.newHashSet(CONSUMER_GROUP_TO_REPLICATE))
          .detectAndCopyNewConsumerGroups(false)
          .synchroniseConsumerGroupOffsets(false)
          .build();
  protected static final software.amazon.msk.replicator.ConsumerGroupReplication UPDATED_CONSUMER_GROUP_REPLICATION_MODEL =
      software.amazon.msk.replicator.ConsumerGroupReplication.builder()
          .consumerGroupsToExclude(Sets.newHashSet(CONSUMER_GROUP_TO_EXCLUDE))
          .consumerGroupsToReplicate(Sets.newHashSet(CONSUMER_GROUP_TO_REPLICATE))
          .detectAndCopyNewConsumerGroups(true)
          .synchroniseConsumerGroupOffsets(true)
          .build();
  protected static final software.amazon.msk.replicator.TopicReplication TOPIC_REPLICATION_MODEL =
      software.amazon.msk.replicator.TopicReplication.builder()
          .topicsToExclude(Sets.newHashSet(TOPIC_TO_EXCLUDE))
          .topicsToReplicate(Sets.newHashSet(TOPIC_TO_REPLICATE))
          .detectAndCopyNewTopics(false)
          .copyTopicConfigurations(false)
          .copyAccessControlListsForTopics(false)
          .build();
  protected static final software.amazon.msk.replicator.TopicReplication UPDATED_TOPIC_REPLICATION_MODEL =
      software.amazon.msk.replicator.TopicReplication.builder()
          .topicsToExclude(Sets.newHashSet(TOPIC_TO_EXCLUDE))
          .topicsToReplicate(Sets.newHashSet(TOPIC_TO_REPLICATE))
          .detectAndCopyNewTopics(true)
          .copyTopicConfigurations(true)
          .copyAccessControlListsForTopics(true)
          .build();
  protected static final software.amazon.msk.replicator.ReplicationInfo REPLICATION_INFO_MODEL =
      software.amazon.msk.replicator.ReplicationInfo.builder()
          .sourceKafkaClusterArn(SOURCE_MSK_CLUSTER_ARN)
          .targetKafkaClusterArn(DESTINATION_MSK_CLUSTER_ARN)
          .consumerGroupReplication(CONSUMER_GROUP_REPLICATION_MODEL)
          .topicReplication(TOPIC_REPLICATION_MODEL)
          .build();
  protected static final software.amazon.msk.replicator.ReplicationInfo UPDATED_REPLICATION_INFO_TOPIC_MODEL =
      software.amazon.msk.replicator.ReplicationInfo.builder()
          .sourceKafkaClusterArn(SOURCE_MSK_CLUSTER_ARN)
          .targetKafkaClusterArn(DESTINATION_MSK_CLUSTER_ARN)
          .consumerGroupReplication(CONSUMER_GROUP_REPLICATION_MODEL)
          .topicReplication(UPDATED_TOPIC_REPLICATION_MODEL)
          .build();
  protected static final software.amazon.msk.replicator.ReplicationInfo UPDATED_REPLICATION_INFO_CONSUMER_GROUP_MODEL =
      software.amazon.msk.replicator.ReplicationInfo.builder()
          .sourceKafkaClusterArn(SOURCE_MSK_CLUSTER_ARN)
          .targetKafkaClusterArn(DESTINATION_MSK_CLUSTER_ARN)
          .consumerGroupReplication(UPDATED_CONSUMER_GROUP_REPLICATION_MODEL)
          .topicReplication(TOPIC_REPLICATION_MODEL)
          .build();
  protected static final Set<software.amazon.msk.replicator.ReplicationInfo> REPLICATION_INFOS_MODEL = Sets.newHashSet(REPLICATION_INFO_MODEL);
  protected static final Set<software.amazon.msk.replicator.ReplicationInfo> UPDATED_REPLICATION_INFOS_MODEL = Sets.newHashSet(UPDATED_REPLICATION_INFO_TOPIC_MODEL);
  protected static final Set<software.amazon.msk.replicator.ReplicationInfo> MULTIPLE_UPDATED_REPLICATION_INFOS_MODEL = Sets.newHashSet(UPDATED_REPLICATION_INFO_TOPIC_MODEL, UPDATED_REPLICATION_INFO_CONSUMER_GROUP_MODEL);


  static {
      MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
      logger = new LoggerProxy();
  }
  static ProxyClient<KafkaClient> MOCK_PROXY(
    final AmazonWebServicesClientProxy proxy,
    final KafkaClient kafkaClient) {
    return new ProxyClient<KafkaClient>() {
      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
      injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
        return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
      CompletableFuture<ResponseT>
      injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
      IterableT
      injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
      injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
      injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public KafkaClient client() {
        return kafkaClient;
      }
    };
  }

  protected static ResourceModel buildResourceModel() {
      return ResourceModel.builder()
          .replicatorName(REPLICATOR_NAME)
          .replicatorArn(REPLICATOR_ARN)
          .kafkaClusters(KAFKA_CLUSTERS_MODEL)
          .replicationInfoList(REPLICATION_INFOS_MODEL)
          .serviceExecutionRoleArn(SERVICE_EXECUTION_ROLE_ARN)
          .tags(TAGS)
          .build();
}

  protected ReplicatorSummary getReplicatorSummary(ReplicatorState replicatorState) {
      return ReplicatorSummary.builder()
          .replicatorState(replicatorState)
          .replicatorName(REPLICATOR_NAME)
          .replicatorArn(REPLICATOR_ARN)
          .kafkaClustersSummary(KAFKA_CLUSTER_SUMMARIES)
          .replicationInfoSummaryList(REPLICATION_INFO_SUMMARIES)
          .build();
  }

  protected DescribeReplicatorResponse getReplicator(ReplicatorState replicatorState) {
      return DescribeReplicatorResponse.builder()
          .replicatorState(replicatorState)
          .replicatorName(REPLICATOR_NAME)
          .replicatorArn(REPLICATOR_ARN)
          .kafkaClusters(KAFKA_CLUSTER_DESCRIPTIONS)
          .replicationInfoList(REPLICATION_INFO_DESCRIPTION)
          .serviceExecutionRoleArn(SERVICE_EXECUTION_ROLE_ARN)
          .tags(TAGS)
          .build();
  }
}
