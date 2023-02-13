package software.amazon.msk.replicator;

import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
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

        return proxy.initiate("AWS-MSK-Replicator::Read", proxyClient, model, callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((describeReplicatorRequest, sdkProxyClient) -> readResource(describeReplicatorRequest, sdkProxyClient , clientRequestToken))
            .handleError((describeReplicatorRequest, exception, _proxyClient, _resourceModel, _callbackContext) ->
                handleError(exception, model,  callbackContext, logger, clientRequestToken))
            .done((describeReplicatorRequest, describeReplicatorResponse, proxyInvocation, resourceModel, context) ->
                constructResourceModelFromResponse(describeReplicatorResponse));
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialized with
     * caller credentials, correct region and retry settings
     * @param describeReplicatorRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private DescribeReplicatorResponse readResource(
        final DescribeReplicatorRequest describeReplicatorRequest,
        final ProxyClient<KafkaClient> proxyClient,
        final String clientRequestToken) {

        DescribeReplicatorResponse describeReplicatorResponse =
            proxyClient.injectCredentialsAndInvokeV2(describeReplicatorRequest, proxyClient.client()::describeReplicator);

        logger.log(String.format("[ClientRequestToken: %s] Successfully read Replicator %s", clientRequestToken,
            describeReplicatorRequest.replicatorArn()));

        return describeReplicatorResponse;
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialized with
     * caller credentials, correct region and retry settings
     * @param describeReplicatorResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
        final DescribeReplicatorResponse describeReplicatorResponse) {

        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(describeReplicatorResponse));
    }
}
