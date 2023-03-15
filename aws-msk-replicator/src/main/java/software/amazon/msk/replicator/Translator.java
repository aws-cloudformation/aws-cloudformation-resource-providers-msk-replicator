package software.amazon.msk.replicator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import software.amazon.awssdk.services.kafka.model.AmazonMskCluster;
import software.amazon.awssdk.services.kafka.model.ConsumerGroupReplication;
import software.amazon.awssdk.services.kafka.model.ConsumerGroupReplicationUpdate;
import software.amazon.awssdk.services.kafka.model.CreateReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.DeleteReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorResponse;
import software.amazon.awssdk.services.kafka.model.KafkaCluster;
import software.amazon.awssdk.services.kafka.model.KafkaClusterClientVpcConfig;
import software.amazon.awssdk.services.kafka.model.ListReplicatorsRequest;
import software.amazon.awssdk.services.kafka.model.ListReplicatorsResponse;
import software.amazon.awssdk.services.kafka.model.ReplicationInfo;
import software.amazon.awssdk.services.kafka.model.ReplicatorSummary;
import software.amazon.awssdk.services.kafka.model.TagResourceRequest;
import software.amazon.awssdk.services.kafka.model.TopicReplication;
import software.amazon.awssdk.services.kafka.model.TopicReplicationUpdate;
import software.amazon.awssdk.services.kafka.model.UntagResourceRequest;
import software.amazon.awssdk.services.kafka.model.UpdateReplicationInfoRequest;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateReplicatorRequest translateToCreateRequest(final ResourceModel model) {
    return CreateReplicatorRequest.builder()
      .replicatorName(model.getReplicatorName())
      .description(model.getDescription())
      .kafkaClusters(model.getKafkaClusters().stream().map(
        kafkaCluster -> KafkaCluster.builder()
          .amazonMskCluster(AmazonMskCluster.builder()
            .mskClusterArn(kafkaCluster.getAmazonMskCluster().getMskClusterArn())
            .build())
          .vpcConfig(KafkaClusterClientVpcConfig.builder()
            .securityGroupIds(kafkaCluster.getVpcConfig().getSecurityGroupIds())
            .subnetIds(kafkaCluster.getVpcConfig().getSubnetIds())
            .build())
          .build())
        .collect(Collectors.toList()))
      .replicationInfoList(model.getReplicationInfoList().stream().map(
        replicationInfo -> ReplicationInfo.builder()
          .sourceKafkaClusterArn(replicationInfo.getSourceKafkaClusterArn())
          .targetKafkaClusterArn(replicationInfo.getTargetKafkaClusterArn())
          .targetCompressionType(replicationInfo.getTargetCompressionType())
          .consumerGroupReplication(ConsumerGroupReplication.builder()
            .consumerGroupsToExclude(replicationInfo.getConsumerGroupReplication().getConsumerGroupsToExclude())
            .consumerGroupsToReplicate(replicationInfo.getConsumerGroupReplication().getConsumerGroupsToReplicate())
            .detectAndCopyNewConsumerGroups(replicationInfo.getConsumerGroupReplication().getDetectAndCopyNewConsumerGroups())
            .synchroniseConsumerGroupOffsets(replicationInfo.getConsumerGroupReplication().getSynchroniseConsumerGroupOffsets())
            .build())
          .topicReplication(TopicReplication.builder()
            .topicsToExclude(replicationInfo.getTopicReplication().getTopicsToExclude())
            .topicsToReplicate(replicationInfo.getTopicReplication().getTopicsToReplicate())
            .copyAccessControlListsForTopics(replicationInfo.getTopicReplication().getCopyAccessControlListsForTopics())
            .copyTopicConfigurations(replicationInfo.getTopicReplication().getCopyTopicConfigurations())
            .detectAndCopyNewTopics(replicationInfo.getTopicReplication().getDetectAndCopyNewTopics())
            .build())
          .build())
        .collect(Collectors.toList()))
      .serviceExecutionRoleArn(model.getServiceExecutionRoleArn())
      .tags(TagHelper.convertToMap(model.getTags()))
      .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeReplicatorRequest translateToReadRequest(final ResourceModel model) {
    return DescribeReplicatorRequest.builder()
      .replicatorArn(model.getReplicatorArn())
      .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param describeReplicatorResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final DescribeReplicatorResponse describeReplicatorResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L58-L73
    Map<String, String> kafkaClusterAliasToArnMap = describeReplicatorResponse
      .kafkaClusters()
      .stream()
      .collect(
        Collectors.toMap(
          kafkaCluster -> kafkaCluster.kafkaClusterAlias(),
          kafkaCluster -> kafkaCluster.amazonMskCluster().mskClusterArn()
        )
      );

    return ResourceModel.builder()
      .replicatorArn(describeReplicatorResponse.replicatorArn())
      .replicatorName(describeReplicatorResponse.replicatorName())
      .currentVersion(describeReplicatorResponse.currentVersion())
      .description(describeReplicatorResponse.replicatorDescription())
      .kafkaClusters(describeReplicatorResponse.kafkaClusters().stream().map(
        kafkaCluster -> software.amazon.msk.replicator.KafkaCluster.builder()
          .amazonMskCluster(software.amazon.msk.replicator.AmazonMskCluster.builder()
            .mskClusterArn(kafkaCluster.amazonMskCluster().mskClusterArn())
            .build())
          .vpcConfig(software.amazon.msk.replicator.KafkaClusterClientVpcConfig.builder()
            .securityGroupIds(kafkaCluster.vpcConfig().securityGroupIds())
            .subnetIds(kafkaCluster.vpcConfig().subnetIds())
            .build())
          .build())
        .collect(Collectors.toSet()))
      .replicationInfoList(describeReplicatorResponse.replicationInfoList().stream().map(
        replicationInfo -> software.amazon.msk.replicator.ReplicationInfo.builder()
          .sourceKafkaClusterArn(kafkaClusterAliasToArnMap.get(replicationInfo.sourceKafkaClusterAlias()))
          .targetKafkaClusterArn(kafkaClusterAliasToArnMap.get(replicationInfo.targetKafkaClusterAlias()))
          .targetCompressionType(replicationInfo.targetCompressionTypeAsString())
          .topicReplication(software.amazon.msk.replicator.TopicReplication.builder()
            .topicsToReplicate(Sets.newHashSet(replicationInfo.topicReplication().topicsToReplicate()))
            .topicsToExclude(Sets.newHashSet(replicationInfo.topicReplication().topicsToExclude()))
            .copyTopicConfigurations(replicationInfo.topicReplication().copyTopicConfigurations())
            .copyAccessControlListsForTopics(replicationInfo.topicReplication().copyAccessControlListsForTopics())
            .detectAndCopyNewTopics(replicationInfo.topicReplication().detectAndCopyNewTopics())
            .build())
          .consumerGroupReplication(software.amazon.msk.replicator.ConsumerGroupReplication.builder()
            .consumerGroupsToReplicate(Sets.newHashSet(replicationInfo.consumerGroupReplication().consumerGroupsToReplicate()))
            .consumerGroupsToExclude(Sets.newHashSet(replicationInfo.consumerGroupReplication().consumerGroupsToExclude()))
            .synchroniseConsumerGroupOffsets(replicationInfo.consumerGroupReplication().synchroniseConsumerGroupOffsets())
            .detectAndCopyNewConsumerGroups(replicationInfo.consumerGroupReplication().detectAndCopyNewConsumerGroups())
            .build())
          .build())
        .collect(Collectors.toSet()))
      .serviceExecutionRoleArn(describeReplicatorResponse.serviceExecutionRoleArn())
      .tags(TagHelper.convertToSet(describeReplicatorResponse.tags()))
      .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteReplicatorRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteReplicatorRequest.builder().replicatorArn(model.getReplicatorArn()).build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @param desiredReplicationInfo changed replication info
   * @return UpdateReplicationInfoRequest the aws service request to modify a resource
   */
  static UpdateReplicationInfoRequest translateToUpdateReplicationInfoRequest(
    final ResourceModel model,
    software.amazon.msk.replicator.ReplicationInfo desiredReplicationInfo) {
    return UpdateReplicationInfoRequest.builder()
      .currentVersion(model.getCurrentVersion())
      .replicatorArn(model.getReplicatorArn())
      .sourceKafkaClusterArn(desiredReplicationInfo.getSourceKafkaClusterArn())
      .targetKafkaClusterArn(desiredReplicationInfo.getTargetKafkaClusterArn())
      .topicReplication(TopicReplicationUpdate.builder()
        .copyAccessControlListsForTopics(desiredReplicationInfo.getTopicReplication().getCopyAccessControlListsForTopics())
        .copyTopicConfigurations(desiredReplicationInfo.getTopicReplication().getCopyTopicConfigurations())
        .detectAndCopyNewTopics(desiredReplicationInfo.getTopicReplication().getDetectAndCopyNewTopics())
        .topicsToExclude(desiredReplicationInfo.getTopicReplication().getTopicsToExclude())
        .topicsToReplicate(desiredReplicationInfo.getTopicReplication().getTopicsToReplicate())
        .build())
      .consumerGroupReplication(ConsumerGroupReplicationUpdate.builder()
        .consumerGroupsToExclude(desiredReplicationInfo.getConsumerGroupReplication().getConsumerGroupsToExclude())
        .consumerGroupsToReplicate(desiredReplicationInfo.getConsumerGroupReplication().getConsumerGroupsToReplicate())
        .detectAndCopyNewConsumerGroups(desiredReplicationInfo.getConsumerGroupReplication().getDetectAndCopyNewConsumerGroups())
        .synchroniseConsumerGroupOffsets(desiredReplicationInfo.getConsumerGroupReplication().getSynchroniseConsumerGroupOffsets())
        .build())
      .build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListReplicatorsRequest translateToListRequest(final String nextToken) {
    return ListReplicatorsRequest.builder()
      .nextToken(nextToken)
      .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   *
   * @param listReplicatorsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListResponse(final ListReplicatorsResponse listReplicatorsResponse) {
    final List<ReplicatorSummary> replicatorsList = listReplicatorsResponse.replicators();
    return streamOfOrEmpty(replicatorsList)
      .map(replicator -> ResourceModel.builder().replicatorArn(replicator.replicatorArn()).build())
      .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
      .map(Collection::stream)
      .orElseGet(Stream::empty);
  }

  /**
   * Request to add tags to a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static TagResourceRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
    return TagResourceRequest.builder()
      .resourceArn(model.getReplicatorArn())
      .tags(addedTags)
      .build();
  }

  /**
   * Request to add tags to a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static UntagResourceRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
    return UntagResourceRequest.builder()
      .resourceArn(model.getReplicatorArn())
      .tagKeys(removedTags)
      .build();
  }
}
