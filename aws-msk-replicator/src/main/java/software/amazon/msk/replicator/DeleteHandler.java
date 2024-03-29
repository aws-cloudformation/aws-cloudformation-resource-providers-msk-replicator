package software.amazon.msk.replicator;

import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.awssdk.services.kafka.model.BadRequestException;
import software.amazon.awssdk.services.kafka.model.DeleteReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.DeleteReplicatorResponse;
import software.amazon.awssdk.services.kafka.model.NotFoundException;
import software.amazon.awssdk.services.kafka.model.ReplicatorState;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.function.BiFunction;
import java.util.function.Function;

public class DeleteHandler extends BaseHandlerStd {
    private static final BiFunction<ResourceModel, ProxyClient<KafkaClient>, ResourceModel> EMPTY_CALL = (model,
        proxyClient) -> model;

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

        return ProgressEvent.progress(model, callbackContext)
            .then(progress -> proxy
                .initiate("AWS-MSK-Replicator::PreDeleteStateCheck", proxyClient, progress.getResourceModel(),
                    progress.getCallbackContext())
                .translateToServiceRequest(Function.identity())
                // We use CREATE delay, since the resource might be in CREATING state.
                .backoffDelay(STABILIZATION_DELAY_CREATE)
                .makeServiceCall(EMPTY_CALL)
                .stabilize(this::stabilizePreDeleteStateCheck)
                .handleError((emptyRequest, exception, _proxyClient, _resourceModel,
                    _callbackContext) -> handleError(exception, model, callbackContext, logger,
                        clientRequestToken))
                .progress())
            .then(progress -> proxy.initiate("AWS-MSK-Replicator::Delete", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .backoffDelay(STABILIZATION_DELAY_DELETE)
                .makeServiceCall(this::deleteResource)
                .stabilize(this::stabilizedOnDelete)
                .handleError((deleteReplicatorRequest, exception, _proxyClient, _resourceModel,
                    _callbackContext) -> handleError(exception, model, callbackContext, logger,
                        clientRequestToken))
                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .build()));
    }

    /**
     * Checks if the Replicator resource can be deleted or not. We check for
     * RUNNING or FAILED states.
     * 
     * @param emptyRequest
     * @param emptyResponse
     * @param proxyClient
     * @param model
     * @param callbackContext
     * @return
     */
    private boolean stabilizePreDeleteStateCheck(
        final ResourceModel emptyRequest,
        final ResourceModel emptyResponse,
        final ProxyClient<KafkaClient> proxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {
        try {
            ReplicatorState replicatorState = proxyClient
                .injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model),
                    proxyClient.client()::describeReplicator)
                .replicatorState();
            return replicatorState == ReplicatorState.RUNNING
                || replicatorState == ReplicatorState.FAILED;
        } catch (NotFoundException e) {
            logger.log(String.format("Replicator with arn: %s is already deleted.", model.getReplicatorArn()));
            return true;
        } catch (BadRequestException e) {
            if (MSK_API_PARAM_NAME_REPLICATOR_ARN.equals(e.invalidParameter()) && e.getMessage() != null
                && e.getMessage().contains(INVALID_PARAMETER_EXCEPTION)) {
                return true;
            } else {
                throw new CfnInvalidRequestException(e);
            }
        }
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialized with
     * caller credentials, correct region and retry settings
     * @param deleteReplicatorRequest the aws service request to delete a resource
     * @param kafkaClient the aws service client to make the call
     * @return delete resource response
     */
    private DeleteReplicatorResponse deleteResource(
        final DeleteReplicatorRequest deleteReplicatorRequest,
        final ProxyClient<KafkaClient> kafkaClient) {
        final String replicatorArn = deleteReplicatorRequest.replicatorArn();
        try {
            return kafkaClient.injectCredentialsAndInvokeV2(deleteReplicatorRequest,
                kafkaClient.client()::deleteReplicator);
        } catch (NotFoundException e) {
            logger.log(String.format("MSK API request for replicator deletion failed with message: %s, because the " +
                "replicator %s does not exist", e.getMessage(), replicatorArn));
            throw new CfnNotFoundException(e);
        } catch (BadRequestException e) {
            if (MSK_API_PARAM_NAME_REPLICATOR_ARN.equals(e.invalidParameter()) && e.getMessage() != null
                && e.getMessage().contains(INVALID_PARAMETER_EXCEPTION)) {
                logger.log(
                    String.format("MSK API request for replicator deletion failed due to invalid replicatorArn %s " +
                        "with Exception %s", replicatorArn, e.getMessage()));
                throw new CfnNotFoundException(e);
            } else {
                // During the cases when the BadRequestException is occurring because of any invalid parameter
                // other than an invalid ReplicatorArn, we retain the regular behaviour for handling BadRequestException
                logger.log(
                    String.format("MSK API request for replicator deletion failed due to invalid replicatorArn %s " +
                        "with Exception %s", replicatorArn, e.getMessage()));
                throw new CfnInvalidRequestException(e);
            }
        }
    }

    /**
     * If deletion of your resource requires some form of stabilization (e.g. propagation delay)
     * for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
     * @param deleteReplicatorRequest the aws service request to delete a resource
     * @param deleteReplicatorResponse the aws service response to delete a resource
     * @param proxyClient the aws service client to make the call
     * @param model resource model
     * @param callbackContext callback context
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnDelete(
        final DeleteReplicatorRequest deleteReplicatorRequest,
        final DeleteReplicatorResponse deleteReplicatorResponse,
        final ProxyClient<KafkaClient> proxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {

        final String replicatorArn = deleteReplicatorRequest.replicatorArn();

        try {
            ReplicatorState currentReplicatorState =
                proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model),
                    proxyClient.client()::describeReplicator).replicatorState();
            switch (currentReplicatorState) {
                case DELETING:
                    logger.log(String.format("Replicator %s is deleting, current state is %s", replicatorArn,
                        currentReplicatorState));
                    return false;
                default:
                    logger.log(String.format("Replicator %s reached unexpected state %s", replicatorArn,
                        currentReplicatorState));
                    throw new CfnNotStabilizedException(
                        ResourceModel.TYPE_NAME, model.getReplicatorArn());
            }
        } catch (NotFoundException e) {
            logger.log(String.format("Replicator %s is deleted", replicatorArn));
            return true;
        } catch (BadRequestException e) {
            if (MSK_API_PARAM_NAME_REPLICATOR_ARN.equals(e.invalidParameter()) && e.getMessage() != null
                && e.getMessage().contains(INVALID_PARAMETER_EXCEPTION)) {
                return true;
            } else {
                throw new CfnInvalidRequestException(e);
            }
        }
    }
}
