package software.amazon.msk.replicator;

import java.time.Duration;
import org.apache.commons.lang3.StringUtils;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.awssdk.services.kafka.model.BadRequestException;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorResponse;
import software.amazon.awssdk.services.kafka.model.ForbiddenException;
import software.amazon.awssdk.services.kafka.model.InternalServerErrorException;
import software.amazon.awssdk.services.kafka.model.NotFoundException;
import software.amazon.awssdk.services.kafka.model.ServiceUnavailableException;
import software.amazon.awssdk.services.kafka.model.TooManyRequestsException;
import software.amazon.awssdk.services.kafka.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    public static final String MULTIPLE_UPDATES_UNSUPPORTED = "You can't update multiple attributes of the replicator in " +
        "same request. Use a different request for each update.";
    public static final String LOG_MSG_MSK_API_REQUEST_FAILED = "MSK API request failed: %s";
    public static final String LOG_MSG_MSK_INTERNAL_FAILURE = "MSK Internal Failure: %s";
    protected static final String MSK_API_PARAM_NAME_REPLICATOR_ARN = "replicatorArn";
    protected static final String INVALID_PARAMETER_EXCEPTION = "One or more of the parameters are not valid";
    protected static final Constant STABILIZATION_DELAY_CREATE =
        Constant.of().timeout(Duration.ofMinutes(120L)).delay(Duration.ofSeconds(30L)).build();
    protected static final Constant STABILIZATION_DELAY_DELETE =
        Constant.of().timeout(Duration.ofMinutes(75L)).delay(Duration.ofSeconds(30L)).build();
    protected static final Constant STABILIZATION_DELAY_UPDATE =
            Constant.of().timeout(Duration.ofMinutes(720L)).delay(Duration.ofMinutes(1L)).build();

  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final Logger logger) {
      return handleRequest(
          proxy,
          request,
          callbackContext != null ? callbackContext : new CallbackContext(),
          proxy.newProxy(ClientBuilder::getClient),
          logger
      );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<KafkaClient> proxyClient,
      final Logger logger);

  protected ProgressEvent<ResourceModel,
      CallbackContext> handleError(
      final Exception exception,
      final ResourceModel model,
      final CallbackContext callbackContext,
      final Logger logger,
      final String clientRequestToken)
      throws Exception {

      if (exception instanceof IllegalArgumentException) {
          final String exceptionMessage = exception.getMessage();
          logger.log(String.format("[ClientRequestToken: %s] Property validation failure while creating replicator: %s",
              clientRequestToken, exceptionMessage));
          return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, String.format("%s",
              exceptionMessage));
      }

      if (exception instanceof BadRequestException) {
          logger.log(String.format(LOG_MSG_MSK_API_REQUEST_FAILED, exception.getMessage()));
          return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, String.format(
              "[ClientRequestToken: %s] %s '%s'", clientRequestToken, exception.getMessage(),
              ((BadRequestException) exception).invalidParameter()));
      }

      if (exception instanceof ForbiddenException || exception instanceof UnauthorizedException) {
          logger.log(String.format(LOG_MSG_MSK_API_REQUEST_FAILED, exception.getMessage()));
          return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, String.format(
              "[ClientRequestToken: %s] %s", clientRequestToken, exception.getMessage()));
      }

      if (exception instanceof InternalServerErrorException) {
          logger.log(String.format(LOG_MSG_MSK_INTERNAL_FAILURE, exception.getMessage()));
          return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InternalFailure, String.format(
              "[ClientRequestToken: %s] %s", clientRequestToken, exception.getMessage()));
      }

      if (exception instanceof ServiceUnavailableException) {
          logger.log(String.format(LOG_MSG_MSK_INTERNAL_FAILURE, exception.getMessage()));
          return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.ServiceInternalError, String.format(
              "[ClientRequestToken: %s] %s", clientRequestToken, exception.getMessage()));
      }

      if (exception instanceof TooManyRequestsException) {
          logger.log(String.format(LOG_MSG_MSK_API_REQUEST_FAILED, exception.getMessage()));
          return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.Throttling, String.format(
              "[ClientRequestToken: %s] %s", clientRequestToken, exception.getMessage()));
      }

      if (exception instanceof NotFoundException) {
          logger.log(String.format(LOG_MSG_MSK_API_REQUEST_FAILED, exception.getMessage()));
          return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, String.format(
              "[ClientRequestToken: %s] %s", clientRequestToken, exception.getMessage()));
      }

      if (exception instanceof AwsServiceException) {
          boolean isError5XX =
              StringUtils.isNotEmpty(exception.getMessage()) &&
                    exception.getMessage().contains("Status Code: 5");
          logger.log(String.format(LOG_MSG_MSK_API_REQUEST_FAILED, exception.getMessage()));

          return ProgressEvent.failed(model, callbackContext,
              isError5XX ?
                      HandlerErrorCode.ServiceInternalError :
                      HandlerErrorCode.GeneralServiceException,
              String.format("[ClientRequestToken: %s] %s", clientRequestToken, exception.getMessage()));
      }

      logger.log(String.format(LOG_MSG_MSK_API_REQUEST_FAILED, exception.getMessage()));
      throw exception;
  }

  protected ProgressEvent < ResourceModel, CallbackContext > describeReplicator(
      final AmazonWebServicesClientProxy proxy,
      final ProxyClient < KafkaClient > proxyClient,
      final ResourceModel model,
      final CallbackContext context,
      final String clientRequestToken,
      final Logger logger) {
      return proxy
          .initiate("AWS-MSK-Replicator::GetReplicatorDetails", proxyClient, model, context)
          .translateToServiceRequest(Translator::translateToReadRequest)
          .makeServiceCall((describeReplicatorRequest, _proxyClient) ->
              getReplicatorDetails(describeReplicatorRequest, _proxyClient, logger))
          .handleError((describeReplicatorRequest, exception, _proxyClient, _resourceModel, _callbackContext) ->
              handleError(exception, model, context, logger, clientRequestToken))
          .done(describeReplicatorResponse -> ProgressEvent.defaultSuccessHandler(
              Translator.translateFromReadResponse(describeReplicatorResponse)));
  }

  private DescribeReplicatorResponse getReplicatorDetails(
      final DescribeReplicatorRequest describeReplicatorRequest,
      final ProxyClient < KafkaClient > proxyClient,
      final Logger logger) {

      logger.log(String.format("Fetching replicator details of resource %s.", describeReplicatorRequest.replicatorArn()));

      return proxyClient.injectCredentialsAndInvokeV2(describeReplicatorRequest, proxyClient.client()::describeReplicator);
  }
}
