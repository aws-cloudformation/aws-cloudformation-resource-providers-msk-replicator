package software.amazon.msk.replicator;

import java.time.Duration;
import java.util.stream.Stream;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.awssdk.services.kafka.model.BadRequestException;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorResponse;
import software.amazon.awssdk.services.kafka.model.ForbiddenException;
import software.amazon.awssdk.services.kafka.model.InternalServerErrorException;
import software.amazon.awssdk.services.kafka.model.KafkaException;
import software.amazon.awssdk.services.kafka.model.NotFoundException;
import software.amazon.awssdk.services.kafka.model.ReplicatorState;
import software.amazon.awssdk.services.kafka.model.ServiceUnavailableException;
import software.amazon.awssdk.services.kafka.model.TagResourceRequest;
import software.amazon.awssdk.services.kafka.model.TagResourceResponse;
import software.amazon.awssdk.services.kafka.model.UnauthorizedException;
import software.amazon.awssdk.services.kafka.model.UpdateReplicationInfoRequest;
import software.amazon.awssdk.services.kafka.model.UpdateReplicationInfoResponse;
import software.amazon.awssdk.services.kafka.model.UntagResourceRequest;
import software.amazon.awssdk.services.kafka.model.UntagResourceResponse;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<KafkaClient> proxyClient;

    @Mock
    KafkaClient kafkaClient;

    private UpdateHandler handler;

    private static Stream<Arguments> KafkaErrorToCfnErrorUpdateOperations() {
        return Stream.of(
            arguments(NotFoundException.class, HandlerErrorCode.NotFound),
            arguments(InternalServerErrorException.class, HandlerErrorCode.InternalFailure),
            arguments(ForbiddenException.class, HandlerErrorCode.InvalidRequest),
            arguments(ServiceUnavailableException.class, HandlerErrorCode.ServiceInternalError),
            arguments(UnauthorizedException.class, HandlerErrorCode.InvalidRequest),
            arguments(BadRequestException.class, HandlerErrorCode.InvalidRequest),
            arguments(IllegalArgumentException.class, HandlerErrorCode.InvalidRequest),
            arguments(AwsServiceException.class, HandlerErrorCode.GeneralServiceException));
    }

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        kafkaClient = mock(KafkaClient.class);
        proxyClient = MOCK_PROXY(proxy, kafkaClient);
        handler = new UpdateHandler();
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(kafkaClient);
    }

    @Test
    public void handleRequest_Success_UpdateReplicationInfo() {

        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);
        final DescribeReplicatorResponse describeReplicatorResponseAfter = getReplicator(ReplicatorState.RUNNING).toBuilder()
            .replicationInfoList(UPDATED_REPLICATION_INFO_DESCRIPTION)
            .build();

        final UpdateReplicationInfoResponse updateReplicationInfoResponse = UpdateReplicationInfoResponse.builder()
            .replicatorArn(REPLICATOR_ARN)
            .replicatorState(ReplicatorState.UPDATING)
            .build();

        when(proxyClient.client().updateReplicationInfo(any(UpdateReplicationInfoRequest.class)))
            .thenReturn(updateReplicationInfoResponse);

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenReturn(describeReplicatorResponse, describeReplicatorResponseAfter);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel().toBuilder()
                    .replicationInfoList(UPDATED_REPLICATION_INFOS_MODEL).build())
                .previousResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), atLeast(2)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(proxyClient.client(), times(0)).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(0)).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), atLeastOnce()).updateReplicationInfo(any(UpdateReplicationInfoRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_Success_UpdateReplicationInfoWithTags() {

        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);
        final DescribeReplicatorResponse describeReplicatorResponseAfter = getReplicator(ReplicatorState.RUNNING).toBuilder()
                .replicationInfoList(UPDATED_REPLICATION_INFO_DESCRIPTION)
                .tags(UPDATED_TAGS)
                .build();

        final UpdateReplicationInfoResponse updateReplicationInfoResponse = UpdateReplicationInfoResponse.builder()
                .replicatorArn(REPLICATOR_ARN)
                .replicatorState(ReplicatorState.UPDATING)
                .build();

        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder().build();
        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder().build();
        when(proxyClient.client().tagResource(any(TagResourceRequest.class))).thenReturn(tagResourceResponse);

        when(proxyClient.client().untagResource(any(UntagResourceRequest.class))).thenReturn(untagResourceResponse);

        when(proxyClient.client().updateReplicationInfo(any(UpdateReplicationInfoRequest.class)))
            .thenReturn(updateReplicationInfoResponse);

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
                .thenReturn(describeReplicatorResponse, describeReplicatorResponseAfter);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel().toBuilder()
                    .replicationInfoList(UPDATED_REPLICATION_INFOS_MODEL)
                    .tags(TagHelper.convertToSet(UPDATED_TAGS))
                    .build())
                .previousResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), atLeast(2)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(proxyClient.client(), atLeastOnce()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), atLeastOnce()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), atLeastOnce()).updateReplicationInfo(any(UpdateReplicationInfoRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_shouldReturnSuccess_ForUnchangedReplicationInfo() {

        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
                .thenReturn(describeReplicatorResponse);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel())
                .previousResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), atLeast(1)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_shouldReturnFailure_ForMultipleReplicationInfoUpdate() {

        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenReturn(describeReplicatorResponse);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel().toBuilder()
                    .replicationInfoList(MULTIPLE_UPDATED_REPLICATION_INFOS_MODEL).build())
                .previousResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

        verify(proxyClient.client(), atLeast(1)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @ParameterizedTest
    @MethodSource("KafkaErrorToCfnErrorUpdateOperations")
    public void handleRequest_shouldReturnFailure_UpdateReplicationInfoFailed(
            Class<KafkaException> kafkaException, HandlerErrorCode cfnError) {

        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenReturn(describeReplicatorResponse);

        when(proxyClient.client().updateReplicationInfo(any(UpdateReplicationInfoRequest.class)))
            .thenThrow(kafkaException);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel().toBuilder()
                    .replicationInfoList(UPDATED_REPLICATION_INFOS_MODEL).build())
                .previousResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(cfnError);

        verify(proxyClient.client(), atLeast(1)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(proxyClient.client(), atLeastOnce()).updateReplicationInfo(any(UpdateReplicationInfoRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @ParameterizedTest
    @MethodSource("KafkaErrorToCfnErrorUpdateOperations")
    public void handleRequest_untagRequestThrowsException(
        Class<KafkaException> kafkaException, HandlerErrorCode cfnError) {

        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
                .thenReturn(describeReplicatorResponse);

        when(proxyClient.client().untagResource(any(UntagResourceRequest.class)))
                .thenThrow(kafkaException);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(buildResourceModel())
                .desiredResourceState(
                    buildResourceModel().toBuilder()
                        .tags(TagHelper.convertToSet(REMOVED_TAGS))
                        .build()
                )
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .previousResourceTags(TAGS)
                .desiredResourceTags(REMOVED_TAGS)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(cfnError);

        verify(proxyClient.client()).untagResource(any(UntagResourceRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @ParameterizedTest
    @MethodSource("KafkaErrorToCfnErrorUpdateOperations")
    public void handleRequest_tagRequestThrowsException(
        Class<KafkaException> kafkaException, HandlerErrorCode cfnError) {

        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenReturn(describeReplicatorResponse);

        when(proxyClient.client().tagResource(any(TagResourceRequest.class)))
            .thenThrow(kafkaException);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(buildResourceModel())
                .desiredResourceState(
                    buildResourceModel().toBuilder()
                        .tags(TagHelper.convertToSet(ADD_TAGS))
                        .build()
                )
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .previousResourceTags(TAGS)
                .desiredResourceTags(ADD_TAGS)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(cfnError);

        verify(proxyClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), times(0)).untagResource(any(UntagResourceRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleStabilize_shouldReturnFailure_InvalidReplicatorStatus() {

        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);
        final DescribeReplicatorResponse describeReplicatorResponseDegraded = getReplicator(ReplicatorState.DEGRADED);

        final UpdateReplicationInfoResponse updateReplicationInfoResponse = UpdateReplicationInfoResponse.builder()
            .replicatorArn(REPLICATOR_ARN)
            .replicatorState(ReplicatorState.UPDATING)
            .build();

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenReturn(describeReplicatorResponse, describeReplicatorResponseDegraded);

        when(proxyClient.client().updateReplicationInfo(any(UpdateReplicationInfoRequest.class)))
            .thenReturn(updateReplicationInfoResponse);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel().toBuilder()
                    .replicationInfoList(UPDATED_REPLICATION_INFOS_MODEL).build())
                .previousResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        assertThrows(CfnNotStabilizedException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), atLeast(2)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(proxyClient.client(), atLeastOnce()).updateReplicationInfo(any(UpdateReplicationInfoRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_shouldReturnFailure_ForInitialDescribeReplicatorFailure() {

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenThrow(NotFoundException.class);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel().toBuilder()
                    .replicationInfoList(UPDATED_REPLICATION_INFOS_MODEL).build())
                .previousResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client(), atLeast(1)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_shouldReturnFailure_ForStabilizingDescribeReplicatorFailure() {
        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);

        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenReturn(describeReplicatorResponse)
            .thenThrow(NotFoundException.class);

        final UpdateReplicationInfoResponse updateReplicationInfoResponse = UpdateReplicationInfoResponse.builder()
            .replicatorArn(REPLICATOR_ARN)
            .replicatorState(ReplicatorState.UPDATING)
            .build();

        when(proxyClient.client().updateReplicationInfo(any(UpdateReplicationInfoRequest.class)))
                .thenReturn(updateReplicationInfoResponse);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel().toBuilder()
                    .replicationInfoList(UPDATED_REPLICATION_INFOS_MODEL).build())
                .previousResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client(), atLeast(2)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

}
