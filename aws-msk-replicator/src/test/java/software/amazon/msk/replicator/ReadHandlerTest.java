package software.amazon.msk.replicator;

import java.time.Duration;
import java.util.stream.Stream;
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
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorRequest;
import software.amazon.awssdk.services.kafka.model.DescribeReplicatorResponse;
import software.amazon.awssdk.services.kafka.model.KafkaException;
import software.amazon.awssdk.services.kafka.model.NotFoundException;
import software.amazon.awssdk.services.kafka.model.ReplicatorState;
import software.amazon.awssdk.services.kafka.model.ServiceUnavailableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<KafkaClient> proxyClient;

    @Mock
    KafkaClient kafkaClient;

    private ReadHandler handler;

    private static Stream<Arguments> KafkaErrorToCfnError() {
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
        handler = new ReadHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        // Given
        final DescribeReplicatorResponse describeReplicatorResponse = getReplicator(ReplicatorState.RUNNING);
        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenReturn(describeReplicatorResponse);

        // When
        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(buildResourceModel())
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

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

        verify(proxyClient.client(), atLeastOnce()).describeReplicator(any(DescribeReplicatorRequest.class));
        verify(kafkaClient, atLeastOnce()).serviceName();
    }

    @ParameterizedTest
    @MethodSource("KafkaErrorToCfnError")
    public void handleRequest_Exception(Class<KafkaException> kafkaException, HandlerErrorCode cfnError) {
        // Given
        when(proxyClient.client().describeReplicator(any(DescribeReplicatorRequest.class)))
            .thenThrow(kafkaException);

        final ResourceModel model = ResourceModel.builder().build();

        // When
        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model)
                .clientRequestToken(CLIENT_REQUEST_TOKEN).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(cfnError);

        verify(proxyClient.client()).describeReplicator(any(DescribeReplicatorRequest.class));
    }
}
