package br.com.opin.mopclient.retry.infrastructure.inbound;

import br.com.opin.mopclient.retry.application.ClientRetryReplayService;
import br.com.opin.mopclient.retry.application.MopServerAvailabilityProbe;
import br.com.opin.mopclient.retry.domain.ClientRetryMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClientRetryReplaySchedulerTest {

    private static final String QUEUE = "mop.client.retry.queue";
    private static final int MAX_PER_TICK = 5;

    private RabbitTemplate rabbitTemplate;
    private ObjectMapper objectMapper;
    private ClientRetryReplayService replayService;
    private MopServerAvailabilityProbe availabilityProbe;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private CircuitBreaker circuitBreaker;
    private Channel channel;

    private ClientRetryReplayScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        rabbitTemplate = mock(RabbitTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        replayService = mock(ClientRetryReplayService.class);
        availabilityProbe = mock(MopServerAvailabilityProbe.class);
        circuitBreakerRegistry = mock(CircuitBreakerRegistry.class);
        circuitBreaker = mock(CircuitBreaker.class);
        channel = mock(Channel.class);

        when(circuitBreakerRegistry.circuitBreaker("mopProcessEndpoint")).thenReturn(circuitBreaker);
        when(availabilityProbe.isServerAvailable()).thenReturn(true);
        when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        when(rabbitTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.amqp.rabbit.core.ChannelCallback<?> callback =
                    invocation.getArgument(0, org.springframework.amqp.rabbit.core.ChannelCallback.class);
            return callback.doInRabbit(channel);
        });

        scheduler = new ClientRetryReplayScheduler(
                rabbitTemplate,
                objectMapper,
                replayService,
                availabilityProbe,
                circuitBreakerRegistry,
                QUEUE,
                MAX_PER_TICK);
    }

    @Test
    void shouldNotDrainWhenProbeReportsUnavailable() throws Exception {
        when(availabilityProbe.isServerAvailable()).thenReturn(false);

        scheduler.drainRetryQueueWhenMopAvailable();

        verify(rabbitTemplate, never()).execute(any());
        verifyNoInteractions(replayService);
        verify(channel, never()).basicGet(any(), anyBoolean());
    }

    @Test
    void shouldNotDrainWhenCircuitBreakerIsOpen() throws Exception {
        when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);

        scheduler.drainRetryQueueWhenMopAvailable();

        verify(rabbitTemplate, never()).execute(any());
        verifyNoInteractions(replayService);
        verify(channel, never()).basicGet(any(), anyBoolean());
    }

    @Test
    void shouldAckOnSuccessfulReplay() throws Exception {
        enqueueRabbitMessages(1L);
        ClientRetryMessage parsed = new ClientRetryMessage();
        when(objectMapper.readValue(any(byte[].class), eq(ClientRetryMessage.class))).thenReturn(parsed);

        scheduler.drainRetryQueueWhenMopAvailable();

        verify(replayService, times(1)).replay(parsed);
        verify(channel, times(1)).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void shouldDiscardWhenMessageIsUnparseable() throws Exception {
        enqueueRabbitMessages(2L);
        when(objectMapper.readValue(any(byte[].class), eq(ClientRetryMessage.class)))
                .thenThrow(new JsonProcessingException("corrupt json") {});

        scheduler.drainRetryQueueWhenMopAvailable();

        verify(replayService, never()).replay(any());
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel, times(1)).basicNack(2L, false, false);
    }

    @Test
    void shouldRequeueAndStopTickOnReplayFailure() throws Exception {
        enqueueRabbitMessages(10L, 11L, 12L);
        ClientRetryMessage parsed = new ClientRetryMessage();
        when(objectMapper.readValue(any(byte[].class), eq(ClientRetryMessage.class))).thenReturn(parsed);
        doThrow(new RuntimeException("MOP 401")).when(replayService).replay(parsed);

        scheduler.drainRetryQueueWhenMopAvailable();

        verify(replayService, times(1)).replay(parsed);
        verify(channel, times(1)).basicNack(10L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel, times(1)).basicGet(QUEUE, false);
    }

    @Test
    void shouldStopTickEarlyWhenBreakerOpensMidDrain() throws Exception {
        enqueueRabbitMessages(100L, 101L, 102L, 103L);
        ClientRetryMessage parsed = new ClientRetryMessage();
        when(objectMapper.readValue(any(byte[].class), eq(ClientRetryMessage.class))).thenReturn(parsed);

        when(circuitBreaker.getState())
                .thenReturn(CircuitBreaker.State.CLOSED)   // initial guard
                .thenReturn(CircuitBreaker.State.CLOSED)   // before msg #1
                .thenReturn(CircuitBreaker.State.OPEN);    // before msg #2 -> stop

        scheduler.drainRetryQueueWhenMopAvailable();

        verify(replayService, times(1)).replay(parsed);
        verify(channel, times(1)).basicAck(100L, false);
        verify(channel, times(1)).basicGet(QUEUE, false);
    }

    @Test
    void shouldStopTickWhenQueueIsEmpty() throws Exception {
        when(channel.basicGet(QUEUE, false)).thenReturn(null);

        scheduler.drainRetryQueueWhenMopAvailable();

        verify(channel, times(1)).basicGet(QUEUE, false);
        verifyNoInteractions(replayService);
    }

    private void enqueueRabbitMessages(long... deliveryTags) throws Exception {
        Deque<GetResponse> responses = new ArrayDeque<>();
        for (long tag : deliveryTags) {
            responses.add(buildGetResponse(tag));
        }
        doAnswer(invocation -> responses.isEmpty() ? null : responses.poll())
                .when(channel).basicGet(QUEUE, false);
    }

    private static GetResponse buildGetResponse(long deliveryTag) {
        Envelope envelope = new Envelope(deliveryTag, false, "", QUEUE);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().build();
        return new GetResponse(envelope, props, "{}".getBytes(), 0);
    }
}
