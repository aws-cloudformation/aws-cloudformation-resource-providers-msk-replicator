package software.amazon.msk.replicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HandlerHelper {
    static boolean isConsumerGroupReplicationUpdated(
        final ConsumerGroupReplication desiredConsumerGroupReplication,
        final ConsumerGroupReplication currentConsumerGroupReplication) {

        return !(
            Objects.deepEquals(desiredConsumerGroupReplication.getConsumerGroupsToReplicate(), currentConsumerGroupReplication.getConsumerGroupsToReplicate()) &&
            Objects.deepEquals(desiredConsumerGroupReplication.getConsumerGroupsToExclude(), currentConsumerGroupReplication.getConsumerGroupsToExclude()) &&
            Objects.equals(desiredConsumerGroupReplication.getDetectAndCopyNewConsumerGroups(),currentConsumerGroupReplication.getDetectAndCopyNewConsumerGroups()) &&
            Objects.equals(desiredConsumerGroupReplication.getSynchroniseConsumerGroupOffsets(), currentConsumerGroupReplication.getSynchroniseConsumerGroupOffsets())
        );
    };

    static boolean isTopicReplicationUpdated(
        final TopicReplication desiredTopicReplication,
        final TopicReplication currentTopicReplication) {

        return !(
            Objects.deepEquals(desiredTopicReplication.getTopicsToReplicate(), currentTopicReplication.getTopicsToReplicate()) &&
            Objects.deepEquals(desiredTopicReplication.getTopicsToExclude(), currentTopicReplication.getTopicsToExclude()) &&
            Objects.equals(desiredTopicReplication.getCopyTopicConfigurations(),currentTopicReplication.getCopyTopicConfigurations()) &&
            Objects.equals(desiredTopicReplication.getDetectAndCopyNewTopics(), currentTopicReplication.getDetectAndCopyNewTopics()) &&
            Objects.equals(desiredTopicReplication.getCopyAccessControlListsForTopics(), currentTopicReplication.getCopyAccessControlListsForTopics())
        );
    };

    static List<ReplicationInfo> getUpdatedReplicationInfos(
        final ResourceModel desiredModel,
        final ResourceModel currentModel) {

        List<ReplicationInfo> updatedDesiredReplicationInfos = new ArrayList<>();

        desiredModel.getReplicationInfoList().forEach(desiredReplicationInfo -> {
            currentModel.getReplicationInfoList().forEach(currentReplicationInfo -> {
                if (
                    desiredReplicationInfo.getSourceKafkaClusterArn()
                        .equals(currentReplicationInfo.getSourceKafkaClusterArn()) &&
                    desiredReplicationInfo.getTargetKafkaClusterArn()
                        .equals(currentReplicationInfo.getTargetKafkaClusterArn())) {
                    if (
                        isTopicReplicationUpdated(
                            desiredReplicationInfo.getTopicReplication(),
                            currentReplicationInfo.getTopicReplication()) ||
                        isConsumerGroupReplicationUpdated(
                            desiredReplicationInfo.getConsumerGroupReplication(),
                            currentReplicationInfo.getConsumerGroupReplication())) {
                        updatedDesiredReplicationInfos.add(desiredReplicationInfo);
                    }
                }
            });
        });

        return updatedDesiredReplicationInfos;
    };
}
