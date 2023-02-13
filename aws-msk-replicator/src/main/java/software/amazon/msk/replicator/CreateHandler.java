package software.amazon.msk.replicator;

import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.awssdk.services.kafka.model.ConflictException;
import software.amazon.awssdk.services.kafka.model.CreateReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.CreateReplicatorResponse;
import software.amazon.awssdk.services.kafka.model.ReplicatorState;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<KafkaClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        final String clientRequestToken = request.getClientRequestToken();

        logger.log( String.format("[Request: %s] Handling create operation, resource model: %s", clientRequestToken,
                model));

        return ProgressEvent.progress(model, callbackContext)
            .then(progress ->
                proxy.initiate("AWS-MSK-Replicator::Create", proxyClient, model, callbackContext)
                    .translateToServiceRequest(Translator::translateToCreateRequest)
                    .backoffDelay(STABILIZATION_DELAY_CREATE)
                    .makeServiceCall(this::createResource)
                    .stabilize(this::stabilizedOnCreate)
                    .handleError((createReplicatorRequest, exception, _proxyClient, _resourceModel, _callbackContext) ->
                        handleError(exception, model,  callbackContext, logger, clientRequestToken))
                    .progress())
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Handler execute operation to call create replicator api
     * @param createReplicatorRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateReplicatorResponse createResource(
        final CreateReplicatorRequest createReplicatorRequest,
        final ProxyClient<KafkaClient> proxyClient) {
        try {
            return proxyClient
                .injectCredentialsAndInvokeV2(createReplicatorRequest,proxyClient.client()::createReplicator);
        } catch (final ConflictException e) {
            logger.log(String.format("Replicator with name %s already exists: %s ", createReplicatorRequest.replicatorName(),
                e.getMessage()));
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, createReplicatorRequest.replicatorName(), e);
        }
    }

    /**
     * Handler stabilize operation to wait till resource reaches terminal state by calling DescribeReplicator api
     * @param createReplicatorRequest the aws service request to create a resource
     * @param createReplicatorResponse the aws service response to create a resource
     * @param proxyClient the aws service client to make the call
     * @param model resource model
     * @param callbackContext callback context
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnCreate(
        final CreateReplicatorRequest createReplicatorRequest,
        final CreateReplicatorResponse createReplicatorResponse,
        final ProxyClient<KafkaClient> proxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {

        if (model.getReplicatorArn() == null) {
            model.setReplicatorArn(createReplicatorResponse.replicatorArn());
        }

        final String replicatorArn = model.getReplicatorArn();
        final ReplicatorState currentReplicatorState =
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model),
                proxyClient.client()::describeReplicator).replicatorState();

        switch (currentReplicatorState) {
            case RUNNING:
                logger.log(String.format("Replicator %s is stabilized, current state is %s", replicatorArn,
                        currentReplicatorState));
                return true;
            case CREATING:
                logger.log(String.format("Replicator %s is stabilizing, current state is %s", replicatorArn,
                        currentReplicatorState));
                return false;
            default:
                logger.log(String.format("Replicator %s reached unexpected state %s", replicatorArn,
                        currentReplicatorState));
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getReplicatorArn());
        }
    }
}
