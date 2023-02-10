package software.amazon.msk.replicator;

import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<KafkaClient> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final String clientRequestToken = request.getClientRequestToken();

        return proxy
            .initiate("AWS-MSK-Replicator::List", proxyClient, model, callbackContext)
            .translateToServiceRequest(
                _resourceModel -> Translator.translateToListRequest(request.getNextToken()))
            .makeServiceCall(
                (listReplicatorsRequest, _proxyClient) ->
                    _proxyClient.injectCredentialsAndInvokeV2(
                        listReplicatorsRequest, _proxyClient.client()::listReplicators))
            .handleError((listReplicatorsRequest, exception, _proxyClient, _resourceModel, _callbackContext) ->
                handleError(exception, model,  callbackContext, logger, clientRequestToken))
            .done((listReplicatorsRequest, listReplicatorsResponse, proxyInvocation, resourceModel, context) ->
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Translator.translateFromListResponse(listReplicatorsResponse))
                    .status(OperationStatus.SUCCESS)
                    .nextToken(listReplicatorsResponse.nextToken()).build());
    }
}