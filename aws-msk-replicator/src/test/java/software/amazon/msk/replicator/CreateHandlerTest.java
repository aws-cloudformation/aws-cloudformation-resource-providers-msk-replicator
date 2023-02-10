package software.amazon.msk.replicator;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.awssdk.services.kafka.model.BadRequestException;
import software.amazon.awssdk.services.kafka.model.ConflictException;
import software.amazon.awssdk.services.kafka.model.CreateReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.CreateReplicatorResponse;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorResponse;
import software.amazon.awssdk.services.kafka.model.ForbiddenException;
import software.amazon.awssdk.services.kafka.model.InternalServerErrorException;
import software.amazon.awssdk.services.kafka.model.KafkaException;
import software.amazon.awssdk.services.kafka.model.NotFoundException;
import software.amazon.awssdk.services.kafka.model.ReplicatorState;
import software.amazon.awssdk.services.kafka.model.ServiceUnavailableException;
import software.amazon.awssdk.services.kafka.model.TooManyRequestsException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<KafkaClient> proxyClient;

    @Mock
    KafkaClient kafkaClient;

    private CreateHandler createHandler;

    private static Stream<Arguments> requestKafkaErrorToCfnError() {
        return Stream.of(
            arguments(BadRequestException.class, HandlerErrorCode.InvalidRequest),
            arguments(ForbiddenException.class, HandlerErrorCode.InvalidRequest),
            arguments(NotFoundException.class, HandlerErrorCode.NotFound),
            arguments(TooManyRequestsException.class, HandlerErrorCode.Throttling),
            arguments(InternalServerErrorException.class, HandlerErrorCode.InternalFailure),
            arguments(ServiceUnavailableException.class, HandlerErrorCode.ServiceInternalError),
            arguments(AwsServiceException.class, HandlerErrorCode.GeneralServiceException));
    }

    private static Stream<Arguments> stabilizeKafkaErrorToCfnError() {
        return Stream.of(
            arguments(NotFoundException.class, HandlerErrorCode.NotFound),
            arguments(ServiceUnavailableException.class, HandlerErrorCode.ServiceInternalError),
            arguments(AwsServiceException.class, HandlerErrorCode.GeneralServiceException));
    }



    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        kafkaClient = mock(KafkaClient.class);
        proxyClient = MOCK_PROXY(proxy, kafkaClient);
        createHandler = new CreateHandler();
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(kafkaClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        // Given
        final CreateReplicatorResponse createReplicatorResponse =
            CreateReplicatorResponse.builder().replicatorState(ReplicatorState.CREATING).replicatorArn(REPLICATOR_ARN)
                .replicatorName(REPLICATOR_NAME)
                .build();
        when(proxyClient.client().createReplicator(any(CreateReplicatorRequest.class)))
            .thenReturn(createReplicatorResponse);

        final DescribeReplicatorResponse describeReplicatorResponseInProgress = getReplicator(ReplicatorState.CREATING);
        final DescribeReplicatorResponse describeReplicatorResponseRunning = getReplicator(ReplicatorState.RUNNING);
        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenReturn(describeReplicatorResponseInProgress, describeReplicatorResponseRunning);

        // When
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(buildResourceModel())
            .clientRequestToken(CLIENT_REQUEST_TOKEN)
            .desiredResourceTags(TAGS)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = createHandler.handleRequest(proxy, request,
            new CallbackContext(), proxyClient, logger);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);

        assertThat(response.getResourceModel().getReplicatorName())
                .isEqualTo(request.getDesiredResourceState().getReplicatorName());
        assertThat(response.getResourceModel().getReplicatorArn())
                .isEqualTo(request.getDesiredResourceState().getReplicatorArn());
        assertThat(response.getResourceModel().getKafkaClusters())
          .isEqualTo(request.getDesiredResourceState().getKafkaClusters());
        assertThat(response.getResourceModel().getReplicationInfoList())
                .isEqualTo(request.getDesiredResourceState().getReplicationInfoList());
        assertThat(response.getResourceModel().getServiceExecutionRoleArn())
                .isEqualTo(request.getDesiredResourceState().getServiceExecutionRoleArn());
        assertThat(response.getResourceModel().getTags())
                .isEqualTo(request.getDesiredResourceState().getTags());

        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createReplicator(any(CreateReplicatorRequest.class));
        verify(proxyClient.client(), times(3)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_SimpleSuccess_WithoutArnInModel() {
        // Given
        final CreateReplicatorResponse createReplicatorResponse =
                CreateReplicatorResponse.builder().replicatorState(ReplicatorState.CREATING).replicatorArn(REPLICATOR_ARN)
                        .replicatorName(REPLICATOR_NAME)
                        .build();
        when(proxyClient.client().createReplicator(any(CreateReplicatorRequest.class)))
                .thenReturn(createReplicatorResponse);

        final DescribeReplicatorResponse describeReplicatorResponseInProgress = getReplicator(ReplicatorState.CREATING);
        final DescribeReplicatorResponse describeReplicatorResponseRunning = getReplicator(ReplicatorState.RUNNING);
        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
                .thenReturn(describeReplicatorResponseInProgress, describeReplicatorResponseRunning);

        // When
        ResourceModel replicatorModel = buildResourceModel();
        replicatorModel.setReplicatorArn(null);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(replicatorModel)
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(TAGS)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = createHandler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);

        assertThat(response.getResourceModel().getReplicatorName())
                .isEqualTo(request.getDesiredResourceState().getReplicatorName());
        assertThat(response.getResourceModel().getReplicatorArn())
                .isEqualTo(request.getDesiredResourceState().getReplicatorArn());
        assertThat(response.getResourceModel().getKafkaClusters())
                .isEqualTo(request.getDesiredResourceState().getKafkaClusters());
        assertThat(response.getResourceModel().getReplicationInfoList())
                .isEqualTo(request.getDesiredResourceState().getReplicationInfoList());
        assertThat(response.getResourceModel().getServiceExecutionRoleArn())
                .isEqualTo(request.getDesiredResourceState().getServiceExecutionRoleArn());
        assertThat(response.getResourceModel().getTags())
                .isEqualTo(request.getDesiredResourceState().getTags());

        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createReplicator(any(CreateReplicatorRequest.class));
        verify(proxyClient.client(), times(3)).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @ParameterizedTest
    @MethodSource("requestKafkaErrorToCfnError")
    public void handleRequest_Exception(Class<KafkaException> kafkaException, HandlerErrorCode cfnError) {
        // Given
        when(proxyClient.client().createReplicator(any(CreateReplicatorRequest.class)))
            .thenThrow(kafkaException);

        // When
        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            createHandler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(cfnError);

        verify(proxyClient.client()).createReplicator(any(CreateReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleStabilize_CreateFailed_GeneralFailure() {
        // Given
        when(proxyClient.client().createReplicator(any(CreateReplicatorRequest.class)))
            .thenReturn(CreateReplicatorResponse.builder().build());
        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.FAILED);
        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenReturn(describeReplicatorResponse);

        // When & Then
        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();
        assertThrows(CfnNotStabilizedException.class,
            () -> createHandler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client()).createReplicator(any(CreateReplicatorRequest.class));
        verify(proxyClient.client()).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleCreate_ResourceConflict_AlreadyExistsFailure() {

        when(proxyClient.client().createReplicator(any(CreateReplicatorRequest.class)))
            .thenThrow(ConflictException.class);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        assertThrows(CfnAlreadyExistsException.class,
            () -> createHandler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client()).createReplicator(any(CreateReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @ParameterizedTest
    @MethodSource("stabilizeKafkaErrorToCfnError")
    public void handleStabilize_Exception(
            Class<KafkaException> kafkaException, HandlerErrorCode cfnError) {
        // Given
        when(proxyClient.client().createReplicator(any(CreateReplicatorRequest.class)))
            .thenReturn(CreateReplicatorResponse.builder().build());
        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenThrow(kafkaException);

        // When
        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
            createHandler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(cfnError);

        verify(proxyClient.client()).createReplicator(any(CreateReplicatorRequest.class));
        verify(proxyClient.client()).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }
}
