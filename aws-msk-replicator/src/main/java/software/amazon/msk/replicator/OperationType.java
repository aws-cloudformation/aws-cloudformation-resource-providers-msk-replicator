package software.amazon.msk.replicator;

import software.amazon.cloudformation.proxy.Logger;

import static software.amazon.msk.replicator.HandlerHelper.getUpdatedReplicationInfos;

public enum OperationType {
    UPDATE_REPLICATION_INFO {
        @Override
        public boolean isUpdated(final ResourceModel desiredModel, final ResourceModel currentModel, final Logger logger) {
            boolean isReplicationInfoUpdated = false;
            if (getUpdatedReplicationInfos(desiredModel, currentModel).size() > 0){
                isReplicationInfoUpdated = true;
                logger.log(String.format(
                    "Found request to update replication info for replicator: %s",
                    currentModel.getReplicatorArn())
                );
            }
            return isReplicationInfoUpdated;
        }
    };

    public boolean isUpdated(final ResourceModel desiredModel, final ResourceModel currentModel, final Logger logger) {
        return false;
    }
}
