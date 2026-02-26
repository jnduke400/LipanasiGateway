package com.hybrid9.pg.Lipanasi.route.processor;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import org.apache.camel.Exchange;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RabbitMqAckProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqAckProcessor.class);

    public void acknowledge(Exchange exchange) {
        Channel channel = exchange.getIn().getHeader(SpringRabbitMQConstants.CHANNEL, Channel.class);
        Long deliveryTag = exchange.getIn().getHeader(SpringRabbitMQConstants.DELIVERY_TAG, Long.class);

        if (channel != null && deliveryTag != null) {
            try {
                channel.basicAck(deliveryTag, false);
                logger.debug("Successfully acknowledged message with deliveryTag: {}", deliveryTag);
            } catch (IOException e) {
                logger.error("Failed to acknowledge message with deliveryTag: {}", deliveryTag, e);
                throw new RuntimeException("Failed to acknowledge RabbitMQ message", e);
            } catch (AlreadyClosedException e) {
                logger.error("Channel already closed for deliveryTag: {}", deliveryTag, e);
                // Implement reconnection logic if needed
            }
        }

        /*else {
            logger.warn("[AutoAck-Mode] Cannot acknowledge - missing channel or deliveryTag");
        }*/
    }

    public void rejectWithRequeue(Exchange exchange, boolean requeue) {
        Channel channel = exchange.getIn().getHeader(SpringRabbitMQConstants.CHANNEL, Channel.class);
        Long deliveryTag = exchange.getIn().getHeader(SpringRabbitMQConstants.DELIVERY_TAG, Long.class);

        if (channel != null && deliveryTag != null) {
            try {
                channel.basicNack(deliveryTag, false, requeue);
                logger.debug("Rejected message with deliveryTag: {}, requeue: {}", deliveryTag, requeue);
            } catch (IOException e) {
                logger.error("Failed to reject message with deliveryTag: {}", deliveryTag, e);
                throw new RuntimeException("Failed to reject RabbitMQ message", e);
            }
        }
    }
}
