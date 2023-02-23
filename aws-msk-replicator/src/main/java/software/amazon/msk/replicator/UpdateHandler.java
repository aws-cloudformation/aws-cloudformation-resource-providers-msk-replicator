package software.amazon.msk.replicator;

import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.awssdk.services.kafka.model.ReplicatorState;
import software.amazon.awssdk.services.kafka.model.UpdateReplicationInfoRequest;
import software.amazon.awssdk.services.kafka.model.UpdateReplicationInfoResponse;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static software.amazon.msk.replicator.HandlerHelper.getUpdatedReplicationInfos;
import static software.amazon.msk.replicator.OperationType.UPDATE_REPLICATION_INFO;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<KafkaClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        TagHelper tagHelper = new TagHelper();

        final ResourceModel desiredModel = request.getDesiredResourceState();

        logger.log(String.format("desiredModel: %s", desiredModel));

        final String clientRequestToken = request.getClientRequestToken();

        ProgressEvent<ResourceModel, CallbackContext> readResponse =
            describeReplicator(proxy, proxyClient, desiredModel, callbackContext, clientRequestToken, logger);

        logger.log(String.format("readResponse: %s", readResponse));

        if (readResponse.getStatus() == OperationStatus.FAILED) {
            return ProgressEvent.failed(readResponse.getResourceModel(), readResponse.getCallbackContext(),
            readResponse.getErrorCode(), readResponse.getMessage());
        }

        final ResourceModel currentModel = readResponse.getResourceModel();

        logger.log(String.format("currentModel: %s", currentModel));

        ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.progress(desiredModel, callbackContext);
        if (tagHelper.shouldUpdateTags(request.getDesiredResourceState(), request)) {
            final Map<String, String> previousTags = tagHelper.getPreviouslyAttachedTags(request);
            final Map<String, String> desiredTags = tagHelper.getNewDesiredTags(desiredModel, request);
            final Map<String, String> addedTags = tagHelper.generateTagsToAdd(previousTags, desiredTags);
            final Set<String> removedTags = tagHelper.generateTagsToRemove(previousTags, desiredTags);

            progressEvent =  progressEvent
                .then(progress -> untagResource(
                    proxy, proxyClient,
                    currentModel, request,
                    callbackContext, progress,
                    clientRequestToken, removedTags
                ))
                .then(progress -> tagResource(
                    proxy, proxyClient,
                    currentModel, request,
                    callbackContext, progress,
                    clientRequestToken, addedTags
                ));
        }

        return progressEvent
            .then(progress -> makeUpdateReplicatorRequest(
                proxy, desiredModel,
                currentModel, proxyClient,
                callbackContext, clientRequestToken
            ))
            .then(progress -> new ReadHandler().handleRequest(
                proxy, request,
                callbackContext,
                proxyClient, logger
            ));
    }

    /**
     * If your resource requires some form of stabilization (e.g. service does not provide strong consistency), you will need to ensure that your code
     * accounts for any potential issues, so that a subsequent read/update requests will not cause any conflicts (e.g. NotFoundException/InvalidRequestException)
     * for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
     * @param proxyClient the aws service client to make the call
     * @param model resource model
     * @param clientRequestToken idempotent token in the request
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnUpdate(
        final UpdateReplicationInfoResponse updateReplicationInfoResponse,
        final ProxyClient<KafkaClient> proxyClient,
        final ResourceModel model,
        final String clientRequestToken) {

        logger.log(String.format("[ClientRequestToken: %s] Stabilizing update operation for replicator %s.",
            clientRequestToken, model.getReplicatorArn()));

        if (model.getReplicatorArn() == null) {
            model.setReplicatorArn(updateReplicationInfoResponse.replicatorArn());
        }

        final String replicatorArn = model.getReplicatorArn();
        final ReplicatorState currentReplicatorState =
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model),
                proxyClient.client()::describeReplicator).replicatorState();

        logger.log(String.format("[ClientRequestToken: %s] Stabilizing replicator %s. Current status is %s",
                clientRequestToken, model.getReplicatorArn(), currentReplicatorState));

        switch (currentReplicatorState) {
            case RUNNING:
                logger.log(String.format("Replicator %s is stabilized, current state is %s", replicatorArn,
                        currentReplicatorState));
                return true;
            case UPDATING:
                logger.log(String.format("Replicator %s is stabilizing, current state is %s", replicatorArn,
                        currentReplicatorState));
                return false;
            default:
                logger.log(String.format("Replicator %s reached unexpected state %s", replicatorArn,
                        currentReplicatorState));
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getReplicatorArn());
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> makeUpdateReplicatorRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceModel desiredModel,
        final ResourceModel currentModel,
        final ProxyClient<KafkaClient> proxyClient,
        final CallbackContext callbackContext,
        final String clientRequestToken) {

        boolean updateReplicationInfoUpdated = UPDATE_REPLICATION_INFO.isUpdated(desiredModel, currentModel, logger);

        final List<Boolean> possibleUpdates = Arrays.asList(updateReplicationInfoUpdated);

        logger.log(String.format("updateReplicationInfoUpdated: %s", updateReplicationInfoUpdated));

        long possibleUpdateCount = possibleUpdates.stream().filter(c -> c != null && c).count();

        List<ReplicationInfo> desiredUpdatedReplicationInfo = getUpdatedReplicationInfos(desiredModel, currentModel);

        if(possibleUpdateCount > 1 || desiredUpdatedReplicationInfo.size() > 1) {
            return ProgressEvent.failed(null, null,
                HandlerErrorCode.InvalidRequest,
                String.format("%s", MULTIPLE_UPDATES_UNSUPPORTED));
        }

        if (possibleUpdateCount == 1) {
            ReplicationInfo desiredReplicationInfo = desiredUpdatedReplicationInfo.get(0);
            return proxy
                .initiate("AWS-MSK-Replicator::UpdateReplicationInfo", proxyClient, desiredModel, callbackContext)
                .translateToServiceRequest(_resourceModel -> Translator.translateToUpdateReplicationInfoRequest(desiredModel, desiredReplicationInfo))
                .backoffDelay(STABILIZATION_DELAY_UPDATE)
                .makeServiceCall((updateReplicationInfoRequest, _proxyClient) -> performUpdateReplicationInfoOperation(updateReplicationInfoRequest, _proxyClient, clientRequestToken))
                .stabilize((updateReplicationInfoRequest, updateReplicationInfoResponse, _proxyClient, _resourceModel, _callbackContext) -> stabilizedOnUpdate(updateReplicationInfoResponse, _proxyClient, desiredModel, clientRequestToken))
                .handleError((updateReplicationInfoRequest, exception, _proxyClient, _resourceModel, _callbackContext) ->
                    handleError(exception, desiredModel, callbackContext, logger, clientRequestToken))
                .progress();
        }

        return ProgressEvent.defaultSuccessHandler(desiredModel);
    }

    /**
     * Handler execute operation to call update replication info api
     *
     * @param updateReplicationInfoRequest the aws service request to update replication info
     * @param proxyClient the aws service client to make the call
     * @param clientRequestToken idempotent token in the request
     * @return UpdateReplicationInfoResponse update replication info response
     */
    private UpdateReplicationInfoResponse performUpdateReplicationInfoOperation(
        final UpdateReplicationInfoRequest updateReplicationInfoRequest,
        final ProxyClient<KafkaClient> proxyClient,
        final String clientRequestToken) {

        logger.log(String.format("[ClientRequestToken: %s] Updating replication info for replicator %s", clientRequestToken,
            updateReplicationInfoRequest.replicatorArn()));

        return proxyClient.injectCredentialsAndInvokeV2(
            updateReplicationInfoRequest, proxyClient.client()::updateReplicationInfo);
    }

    /**
     * tagResource during update
     *
     * Calls the service:TagResource API.
     */
    private ProgressEvent<ResourceModel, CallbackContext> tagResource(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<KafkaClient> serviceClient,
        final ResourceModel resourceModel,
        final ResourceHandlerRequest<ResourceModel> handlerRequest,
        final CallbackContext callbackContext,
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
        final String clientRequestToken,
        final Map<String, String> addedTags) {

        if (addedTags.isEmpty()) {
            return ProgressEvent.progress(resourceModel, progressEvent.getCallbackContext());
        }

        logger.log(String.format("[UPDATE][IN PROGRESS] Going to add tags for MSK Replicator resource: %s with AccountId: %s",
                resourceModel.getReplicatorName(), handlerRequest.getAwsAccountId()));

        return proxy.initiate("AWS-MSK-Replicator::TagResource", serviceClient, resourceModel, callbackContext)
            .translateToServiceRequest(model ->
                Translator.tagResourceRequest(model, addedTags))
            .makeServiceCall((tagResourceRequest, _proxyClient) -> _proxyClient.injectCredentialsAndInvokeV2(
                tagResourceRequest, _proxyClient.client()::tagResource))
            .handleError((tagResourceRequest, exception, _proxyClient, _resourceModel, _callbackContext) ->
                handleError(exception, resourceModel,  callbackContext, logger, clientRequestToken))
            .progress();
    }

    /**
     * untagResource during update
     *
     * Calls the service:UntagResource API.
     */
    private ProgressEvent<ResourceModel, CallbackContext> untagResource(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<KafkaClient> serviceClient,
        final ResourceModel resourceModel,
        final ResourceHandlerRequest<ResourceModel> handlerRequest,
        final CallbackContext callbackContext,
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
        final String clientRequestToken,
        final Set<String> removedTags) {

        if (removedTags.isEmpty()) {
            return ProgressEvent.progress(resourceModel, progressEvent.getCallbackContext());
        }

        logger.log(String.format("[UPDATE][IN PROGRESS] Going to remove tags for MSK Replicator resource: %s with AccountId: %s",
            resourceModel.getReplicatorName(), handlerRequest.getAwsAccountId()));

        return proxy.initiate("AWS-MSK-Replicator::UntagResource", serviceClient, resourceModel, callbackContext)
            .translateToServiceRequest(model ->
                Translator.untagResourceRequest(model, removedTags))
            .makeServiceCall((untagResourceRequest, _proxyClient) -> _proxyClient.injectCredentialsAndInvokeV2(
                untagResourceRequest, _proxyClient.client()::untagResource))
            .handleError((untagResourceRequest, exception, _proxyClient, _resourceModel, _callbackContext) ->
                handleError(exception, resourceModel,  callbackContext, logger, clientRequestToken))
            .progress();
    }

}
