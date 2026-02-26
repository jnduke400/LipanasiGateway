package com.hybrid9.pg.Lipanasi.configs;



import com.hybrid9.pg.Lipanasi.component.gsonresolver.LocalDateTimeAdapter;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
//import net.minidev.json.JSONUtil;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class CamelConfiguration {

    @Value("${rabbitmq.host}")
    private String rabbitHost;
    @Value("${rabbitmq.port}")
    private int rabbitPort;
    @Value("${rabbitmq.username}")
    private String rabbitUsername;
    @Value("${rabbitmq.password}")
    private String rabbitPassword;


    // Queue names
    public static final String QUEUE_SMSC = "queue.smsc";
    public static final String QUEUE_HALOPESA = "queue.halopesa";
    public static final String QUEUE_HALOPESA_INITS = "queue.halopesa_inits";
    public static final String QUEUE_HALOPESA_PAY_BILL = "queue.halopesa_pay_bill";
    public static final String QUEUE_MPESA_PAY_BILL = "queue.mpesa_pay_bill";
    public static final String QUEUE_AIRTEL_MONEY_PAY_BILL = "queue.airtelmoney_pay_bill";
    public static final String QUEUE_TIGOPESA_PAY_BILL = "queue.tigopesa_pay_bill";
    public static final String QUEUE_MPESA = "queue.mpesa";
    public static final String QUEUE_MPESA_INIT = "queue.mpesa_init";
    public static final String QUEUE_TIGOPESA_INITS = "queue.tigopesa_inits";
    public static final String QUEUE_TIGOPESA = "queue.tigopesa";
    public static final String QUEUE_AIRTEL_MONEY = "queue.airtelmoney";
    public static final String QUEUE_AIRTEL_MONEY_INIT = "queue.airtelmoney_init";
    public static final String QUEUE_MPESA_CONGO = "queue.mpesa-congo";
    public static final String QUEUE_MPESA_CONGO_INIT = "queue.mpesa-congo_init";
    public static final String QUEUE_AIRTEL_MONEY_CONGO = "queue.airtelmoney-congo";
    public static final String QUEUE_AIRTEL_MONEY_CONGO_INIT = "queue.airtelmoney-congo_init";
    public static final String QUEUE_ORANGE_MONEY_CONGO = "queue.orangemoney-congo";
    public static final String QUEUE_ORANGE_MONEY_CONGO_INIT = "queue.orangemoney-congo_init";
    public static final String QUEUE_HALOPESA_PAY_BILL_VALIDATION = "queue.halopesa_pay_bill_validation";
    public static final String QUEUE_MPESA_PAY_BILL_VALIDATION = "queue.mpesa_pay_bill_validation";
    public static final String QUEUE_AIRTEL_MONEY_PAY_BILL_VALIDATION = "queue.airtelmoney_pay_bill_validation";
    public static final String QUEUE_TIGOPESA_PAY_BILL_VALIDATION = "queue.tigopesa_pay_bill_validation";
    public static final String QUEUE_MPESA_PAY_BILL_CALLBACK = "queue.mpesa_pay_bill_callback";
    public static final String QUEUE_VAT = "queue.vat";
    public static final String QUEUE_CRDB = "queue.crdb";
    public static final String QUEUE_VENDOR_CALLBACK = "queue.vendor_callback";
    public static final String QUEUE_FAILED_DEPOSITS = "queue.failed-deposits";



    // Exchange names
    public static final String EXCHANGE_SMSC = "smsc.exchange";
    public static final String EXCHANGE_HALOPESA = "halopesa.exchange";
    public static final String EXCHANGE_HALOPESA_INITS = "halopesa_inits.exchange";
    public static final String EXCHANGE_HALOPESA_PAY_BILL = "halopesa_pay_bill.exchange";
    public static final String EXCHANGE_MPESA_PAY_BILL = "mpesa_pay_bill.exchange";
    public static final String EXCHANGE_AIRTEL_MONEY_PAY_BILL = "airtelmoney_pay_bill.exchange";
    public static final String EXCHANGE_TIGOPESA_PAY_BILL = "tigopesa_pay_bill.exchange";
    public static final String EXCHANGE_MPESA = "mpesa.exchange";
    public static final String EXCHANGE_MPESA_INIT = "mpesa_init.exchange";
    public static final String EXCHANGE_TIGOPESA = "tigopesa.exchange";
    public static final String EXCHANGE_TIGOPESA_INITS = "tigopesa_inits.exchange";
    public static final String EXCHANGE_AIRTEL_MONEY = "airtelmoney.exchange";
    public static final String EXCHANGE_AIRTEL_MONEY_INIT = "airtelmoney_init.exchange";
    public static final String EXCHANGE_MPESA_CONGO = "mpesa-congo.exchange";
    public static final String EXCHANGE_MPESA_CONGO_INIT = "mpesa-congo_init.exchange";
    public static final String EXCHANGE_AIRTEL_MONEY_CONGO = "airtelmoney-congo.exchange";
    public static final String EXCHANGE_AIRTEL_MONEY_CONGO_INIT = "airtelmoney-congo_init.exchange";
    public static final String EXCHANGE_ORANGE_MONEY_CONGO = "orangemoney-congo.exchange";
    public static final String EXCHANGE_ORANGE_MONEY_CONGO_INIT = "orangemoney-congo_init.exchange";
    public static final String EXCHANGE_HALOPESA_PAY_BILL_VALIDATION = "halopesa_pay_bill_validation.exchange";
    public static final String EXCHANGE_MPESA_PAY_BILL_VALIDATION = "mpesa_pay_bill_validation.exchange";
    public static final String EXCHANGE_AIRTEL_MONEY_PAY_BILL_VALIDATION = "airtelmoney_pay_bill_validation.exchange";
    public static final String EXCHANGE_TIGOPESA_PAY_BILL_VALIDATION = "tigopesa_pay_bill_validation.exchange";
    public static final String EXCHANGE_MPESA_PAY_BILL_CALLBACK = "mpesa_pay_bill_callback.exchange";
    public static final String EXCHANGE_VAT = "vat.exchange";
    public static final String EXCHANGE_CRDB = "crdb.exchange";
    public static final String EXCHANGE_VENDOR_CALLBACK = "vendor_callback.exchange";
    public static final String EXCHANGE_FAILED_DEPOSITS = "failed-deposits.exchange";


    // Routing keys
    public static final String ROUTING_KEY_SMSC = "routing.key.smsc";
    public static final String ROUTING_KEY_HALOPESA = "routing.key.halopesa";
    public static final String ROUTING_KEY_HALOPESA_INITS = "routing.key.halopesa_inits";
    public static final String ROUTING_KEY_HALOPESA_PAY_BILL = "routing.key.halopesa_pay_bill";
    public static final String ROUTING_KEY_MPESA_PAY_BILL = "routing.key.mpesa_pay_bill";
    public static final String ROUTING_KEY_AIRTEL_MONEY_PAY_BILL = "routing.key.airtelmoney_pay_bill";
    public static final String ROUTING_KEY_TIGOPESA_PAY_BILL = "routing.key.tigopesa_pay_bill";
    public static final String ROUTING_KEY_MPESA = "routing.key.mpesa";
    public static final String ROUTING_KEY_MPESA_INIT = "routing.key.mpesa_init";
    public static final String ROUTING_KEY_TIGOPESA = "routing.key.tigopesa";
    public static final String ROUTING_KEY_TIGOPESA_INITS = "routing.key.tigopesa_inits";
    public static final String ROUTING_KEY_AIRTEL_MONEY = "routing.key.airtelmoney";
    public static final String ROUTING_KEY_AIRTEL_MONEY_INIT = "routing.key.airtelmoney_init";
    public static final String ROUTING_KEY_MPESA_CONGO = "routing.key.mpesa-congo";
    public static final String ROUTING_KEY_MPESA_CONGO_INIT = "routing.key.mpesa-congo_init";
    public static final String ROUTING_KEY_AIRTEL_MONEY_CONGO = "routing.key.airtelmoney-congo";
    public static final String ROUTING_KEY_AIRTEL_MONEY_CONGO_INIT = "routing.key.airtelmoney-congo_init";
    public static final String ROUTING_KEY_ORANGE_MONEY_CONGO = "routing.key.orangemoney-congo";
    public static final String ROUTING_KEY_ORANGE_MONEY_CONGO_INIT = "routing.key.orangemoney-congo_init";
    public static final String ROUTING_KEY_HALOPESA_PAY_BILL_VALIDATION = "routing.key.halopesa_pay_bill_validation";
    public static final String ROUTING_KEY_MPESA_PAY_BILL_VALIDATION = "routing.key.mpesa_pay_bill_validation";
    public static final String ROUTING_KEY_AIRTEL_MONEY_PAY_BILL_VALIDATION = "routing.key.airtelmoney_pay_bill_validation";
    public static final String ROUTING_KEY_TIGOPESA_PAY_BILL_VALIDATION = "routing.key.tigopesa_pay_bill_validation";
    public static final String ROUTING_KEY_MPESA_PAY_BILL_CALLBACK = "routing.key.mpesa_pay_bill_callback";
    public static final String ROUTING_KEY_VAT = "routing.key.vat";
    public static final String ROUTING_KEY_CRDB = "routing.key.crdb";
    public static final String ROUTING_KEY_VENDOR_CALLBACK = "routing.key.vendor_callback";
    public static final String ROUTING_KEY_FAILED_DEPOSITS = "routing.key.failed-deposits";

    // Producer URIs
    public static final String RABBIT_PRODUCER_SMSC_URI =
            "spring-rabbitmq:" + EXCHANGE_SMSC + "?routingKey=" + ROUTING_KEY_SMSC;
    public static final String RABBIT_PRODUCER_HALOPESA_URI =
            "spring-rabbitmq:" + EXCHANGE_HALOPESA + "?routingKey=" + ROUTING_KEY_HALOPESA;

    public static final String RABBIT_PRODUCER_HALOPESA_INITS_URI =
            "spring-rabbitmq:" + EXCHANGE_HALOPESA_INITS + "?routingKey=" + ROUTING_KEY_HALOPESA_INITS;

    public static final String RABBIT_PRODUCER_HALOPESA_PAY_BILL_URI =
            "spring-rabbitmq:" + EXCHANGE_HALOPESA_PAY_BILL + "?routingKey=" + ROUTING_KEY_HALOPESA_PAY_BILL;

    public static final String RABBIT_PRODUCER_MPESA_PAY_BILL_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_PAY_BILL + "?routingKey=" + ROUTING_KEY_MPESA_PAY_BILL;

    public static final String RABBIT_PRODUCER_AIRTEL_MONEY_PAY_BILL_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_PAY_BILL + "?routingKey=" + ROUTING_KEY_AIRTEL_MONEY_PAY_BILL;

    public static final String RABBIT_PRODUCER_TIGOPESA_PAY_BILL_URI =
            "spring-rabbitmq:" + EXCHANGE_TIGOPESA_PAY_BILL + "?routingKey=" + ROUTING_KEY_TIGOPESA_PAY_BILL;

    public static final String RABBIT_PRODUCER_MPESA_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA + "?routingKey=" + ROUTING_KEY_MPESA;

    public static final String RABBIT_PRODUCER_MPESA_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_INIT + "?routingKey=" + ROUTING_KEY_MPESA_INIT;
    public static final String RABBIT_PRODUCER_TIGOPESA_URI =
            "spring-rabbitmq:" + EXCHANGE_TIGOPESA + "?routingKey=" + ROUTING_KEY_TIGOPESA;
    public static final String RABBIT_PRODUCER_TIGOPESA_INITS_URI =
            "spring-rabbitmq:" + EXCHANGE_TIGOPESA_INITS + "?routingKey=" + ROUTING_KEY_TIGOPESA_INITS;
    public static final String RABBIT_PRODUCER_AIRTEL_MONEY_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY + "?routingKey=" + ROUTING_KEY_AIRTEL_MONEY;
    public static final String RABBIT_PRODUCER_AIRTEL_MONEY_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_INIT + "?routingKey=" + ROUTING_KEY_AIRTEL_MONEY_INIT;
    public static final String RABBIT_PRODUCER_MPESA_CONGO_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_CONGO + "?routingKey=" + ROUTING_KEY_MPESA_CONGO;
    public static final String RABBIT_PRODUCER_MPESA_CONGO_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_CONGO_INIT + "?routingKey=" + ROUTING_KEY_MPESA_CONGO_INIT;
    public static final String RABBIT_PRODUCER_AIRTEL_MONEY_CONGO_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_CONGO + "?routingKey=" + ROUTING_KEY_AIRTEL_MONEY_CONGO;
    public static final String RABBIT_PRODUCER_AIRTEL_MONEY_CONGO_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_CONGO_INIT + "?routingKey=" + ROUTING_KEY_AIRTEL_MONEY_CONGO_INIT;
    public static final String RABBIT_PRODUCER_ORANGE_MONEY_CONGO_URI =
            "spring-rabbitmq:" + EXCHANGE_ORANGE_MONEY_CONGO + "?routingKey=" + ROUTING_KEY_ORANGE_MONEY_CONGO;
    public static final String RABBIT_PRODUCER_ORANGE_MONEY_CONGO_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_ORANGE_MONEY_CONGO_INIT + "?routingKey=" + ROUTING_KEY_ORANGE_MONEY_CONGO_INIT;
    public static final String RABBIT_PRODUCER_HALOPESA_PAY_BILL_VALIDATION_URI =
            "spring-rabbitmq:" + EXCHANGE_HALOPESA_PAY_BILL_VALIDATION + "?routingKey=" + ROUTING_KEY_HALOPESA_PAY_BILL_VALIDATION;
    public static final String RABBIT_PRODUCER_MPESA_PAY_BILL_VALIDATION_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_PAY_BILL_VALIDATION + "?routingKey=" + ROUTING_KEY_MPESA_PAY_BILL_VALIDATION;
    public static final String RABBIT_PRODUCER_AIRTEL_MONEY_PAY_BILL_VALIDATION_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_PAY_BILL_VALIDATION + "?routingKey=" + ROUTING_KEY_AIRTEL_MONEY_PAY_BILL_VALIDATION;
    public static final String RABBIT_PRODUCER_TIGOPESA_PAY_BILL_VALIDATION_URI =
            "spring-rabbitmq:" + EXCHANGE_TIGOPESA_PAY_BILL_VALIDATION + "?routingKey=" + ROUTING_KEY_TIGOPESA_PAY_BILL_VALIDATION;
    public static final String RABBIT_PRODUCER_MPESA_PAY_BILL_CALLBACK_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_PAY_BILL_CALLBACK + "?routingKey=" + ROUTING_KEY_MPESA_PAY_BILL_CALLBACK;
    public static final String RABBIT_PRODUCER_VAT_URI =
            "spring-rabbitmq:" + EXCHANGE_VAT + "?routingKey=" + ROUTING_KEY_VAT;
    public static final String RABBIT_PRODUCER_CRDB_URI =
            "spring-rabbitmq:" + EXCHANGE_CRDB + "?routingKey=" + ROUTING_KEY_CRDB;
    public static final String RABBIT_PRODUCER_VENDOR_CALLBACK_URI =
            "spring-rabbitmq:" + EXCHANGE_VENDOR_CALLBACK + "?routingKey=" + ROUTING_KEY_VENDOR_CALLBACK;
    public static final String RABBIT_PRODUCER_FAILED_DEPOSITS_URI =
            "spring-rabbitmq:" + EXCHANGE_FAILED_DEPOSITS + "?routingKey=" + ROUTING_KEY_FAILED_DEPOSITS;


    // Consumer URIs
    public static final String RABBIT_CONSUMER_SMSC_URI =
            "spring-rabbitmq:" + EXCHANGE_SMSC + "?queues=" + QUEUE_SMSC +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_HALOPESA_URI =
            "spring-rabbitmq:" + EXCHANGE_HALOPESA + "?queues=" + QUEUE_HALOPESA +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_HALOPESA_INITS_URI =
            "spring-rabbitmq:" + EXCHANGE_HALOPESA_INITS + "?queues=" + QUEUE_HALOPESA_INITS +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_HALOPESA_PAY_BILL_URI =
            "spring-rabbitmq:" + EXCHANGE_HALOPESA_PAY_BILL + "?queues=" + QUEUE_HALOPESA_PAY_BILL +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_MPESA_PAY_BILL_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_PAY_BILL + "?queues=" + QUEUE_MPESA_PAY_BILL +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_AIRTEL_MONEY_PAY_BILL_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_PAY_BILL + "?queues=" + QUEUE_AIRTEL_MONEY_PAY_BILL +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_TIGOPESA_PAY_BILL_URI =
            "spring-rabbitmq:" + EXCHANGE_TIGOPESA_PAY_BILL + "?queues=" + QUEUE_TIGOPESA_PAY_BILL +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_MPESA_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA + "?queues=" + QUEUE_MPESA +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_MPESA_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_INIT + "?queues=" + QUEUE_MPESA_INIT +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_TIGOPESA_URI =
            "spring-rabbitmq:" + EXCHANGE_TIGOPESA + "?queues=" + QUEUE_TIGOPESA +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_TIGOPESA_INITS_URI =
            "spring-rabbitmq:" + EXCHANGE_TIGOPESA_INITS + "?queues=" + QUEUE_TIGOPESA_INITS +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_AIRTEL_MONEY_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY + "?queues=" + QUEUE_AIRTEL_MONEY +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_AIRTEL_MONEY_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_INIT + "?queues=" + QUEUE_AIRTEL_MONEY_INIT +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_MPESA_CONGO_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_CONGO + "?queues=" + QUEUE_MPESA_CONGO +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_MPESA_CONGO_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_CONGO_INIT + "?queues=" + QUEUE_MPESA_CONGO_INIT +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_AIRTEL_MONEY_CONGO_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_CONGO + "?queues=" + QUEUE_AIRTEL_MONEY_CONGO +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_AIRTEL_MONEY_CONGO_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_CONGO_INIT + "?queues=" + QUEUE_AIRTEL_MONEY_CONGO_INIT +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_ORANGE_MONEY_CONGO_URI =
            "spring-rabbitmq:" + EXCHANGE_ORANGE_MONEY_CONGO + "?queues=" + QUEUE_ORANGE_MONEY_CONGO +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_ORANGE_MONEY_CONGO_INIT_URI =
            "spring-rabbitmq:" + EXCHANGE_ORANGE_MONEY_CONGO_INIT + "?queues=" + QUEUE_ORANGE_MONEY_CONGO_INIT +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_HALOPESA_PAY_BILL_VALIDATION_URI =
            "spring-rabbitmq:" + EXCHANGE_HALOPESA_PAY_BILL_VALIDATION + "?queues=" + QUEUE_HALOPESA_PAY_BILL_VALIDATION +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_MPESA_PAY_BILL_VALIDATION_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_PAY_BILL_VALIDATION + "?queues=" + QUEUE_MPESA_PAY_BILL_VALIDATION +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_AIRTEL_MONEY_PAY_BILL_VALIDATION_URI =
            "spring-rabbitmq:" + EXCHANGE_AIRTEL_MONEY_PAY_BILL_VALIDATION + "?queues=" + QUEUE_AIRTEL_MONEY_PAY_BILL_VALIDATION +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_TIGOPESA_PAY_BILL_VALIDATION_URI =
            "spring-rabbitmq:" + EXCHANGE_TIGOPESA_PAY_BILL_VALIDATION + "?queues=" + QUEUE_TIGOPESA_PAY_BILL_VALIDATION +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_MPESA_PAY_BILL_CALLBACK_URI =
            "spring-rabbitmq:" + EXCHANGE_MPESA_PAY_BILL_CALLBACK + "?queues=" + QUEUE_MPESA_PAY_BILL_CALLBACK +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_VAT_URI =
            "spring-rabbitmq:" + EXCHANGE_VAT + "?queues=" + QUEUE_VAT +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_CRDB_URI =
            "spring-rabbitmq:" + EXCHANGE_CRDB + "?queues=" + QUEUE_CRDB +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_VENDOR_CALLBACK_URI =
            "spring-rabbitmq:" + EXCHANGE_VENDOR_CALLBACK + "?queues=" + QUEUE_VENDOR_CALLBACK +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    public static final String RABBIT_CONSUMER_FAILED_DEPOSITS_URI =
            "spring-rabbitmq:" + EXCHANGE_FAILED_DEPOSITS + "?queues=" + QUEUE_FAILED_DEPOSITS +
                    "&concurrentConsumers=4&maxConcurrentConsumers=6&prefetchCount=20&autoDeclare=false";
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitHost);
        connectionFactory.setPort(rabbitPort);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        return connectionFactory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(4);  // Start with lower concurrency
        factory.setMaxConcurrentConsumers(6);
        factory.setPrefetchCount(20);  // Lower prefetch count
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);

        // Set receive timeout
        factory.setReceiveTimeout(5000L); // 5 seconds

        // Set idle event interval
        factory.setIdleEventInterval(60000L); // 1 minute

        return factory;
    }

   /* @Bean
    public Declarables rabbitMqBindings() {
        return new Declarables(
                new DirectExchange("mpesa.dlx"),
                new Queue("mpesa.dlq"),
                new Binding("mpesa.dlq",
                        Binding.DestinationType.QUEUE,
                        "mpesa.dlx",
                        "mpesa.failed",
                        null)
        );
    }*/


    /*@Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate((org.springframework.amqp.rabbit.connection.ConnectionFactory) connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange("api.exchange");
        template.setRoutingKey("api.routing.key");
        return template;
    }*/

    // Queue definitions
    @Bean
    public Queue smscQueue() {
        return QueueBuilder.durable(QUEUE_SMSC)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean(name = "halopesaQueue")
    public Queue halopesaQueue() {
        return QueueBuilder.durable(QUEUE_HALOPESA)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean(name = "halopesaInitsQueue")
    public Queue halopesaInitsQueue() {
        return QueueBuilder.durable(QUEUE_HALOPESA_INITS)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue halopesaPayBillQueue() {
        return QueueBuilder.durable(QUEUE_HALOPESA_PAY_BILL)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue mpesaPayBillQueue() {
        return QueueBuilder.durable(QUEUE_MPESA_PAY_BILL)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue airtelMoneyPayBillQueue() {
        return QueueBuilder.durable(QUEUE_AIRTEL_MONEY_PAY_BILL)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue tigopesaPayBillQueue() {
        return QueueBuilder.durable(QUEUE_TIGOPESA_PAY_BILL)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean(name = "mpesaQueue")
    public Queue mpesaQueue() {
        return QueueBuilder.durable(QUEUE_MPESA)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean(name = "mpesaInitQueue")
    public Queue mpesaInitQueue() {
        return QueueBuilder.durable(QUEUE_MPESA_INIT)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean(name = "tigopesaQueue")
    public Queue tigopesaQueue() {
        return QueueBuilder.durable(QUEUE_TIGOPESA)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean(name = "tigopesaInitsQueue")
    public Queue tigopesaInitsQueue() {
        return QueueBuilder.durable(QUEUE_TIGOPESA_INITS)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean(name = "airtelMoneyQueue")
    public Queue airtelMoneyQueue() {
        return QueueBuilder.durable(QUEUE_AIRTEL_MONEY)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean(name = "airtelMoneyInitQueue")
    public Queue airtelMoneyInitQueue() {
        return QueueBuilder.durable(QUEUE_AIRTEL_MONEY_INIT)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue mpesaCongoQueue() {
        return QueueBuilder.durable(QUEUE_MPESA_CONGO)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean
    public Queue mpesaCongoInitQueue() {
        return QueueBuilder.durable(QUEUE_MPESA_CONGO_INIT)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean
    public Queue airtelMoneyCongoQueue() {
        return QueueBuilder.durable(QUEUE_AIRTEL_MONEY_CONGO)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean
    public Queue airtelMoneyCongoInitQueue() {
        return QueueBuilder.durable(QUEUE_AIRTEL_MONEY_CONGO_INIT)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue orangeMoneyCongoQueue() {
        return QueueBuilder.durable(QUEUE_ORANGE_MONEY_CONGO)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean
    public Queue orangeMoneyCongoInitQueue() {
        return QueueBuilder.durable(QUEUE_ORANGE_MONEY_CONGO_INIT)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue halopesaPayBillValidationQueue() {
        return QueueBuilder.durable(QUEUE_HALOPESA_PAY_BILL_VALIDATION)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue mpesaPayBillValidationQueue() {
        return QueueBuilder.durable(QUEUE_MPESA_PAY_BILL_VALIDATION)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue airtelMoneyPayBillValidationQueue() {
        return QueueBuilder.durable(QUEUE_AIRTEL_MONEY_PAY_BILL_VALIDATION)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue tigopesaPayBillValidationQueue() {
        return QueueBuilder.durable(QUEUE_TIGOPESA_PAY_BILL_VALIDATION)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue mpesaPayBillCallbackQueue() {
        return QueueBuilder.durable(QUEUE_MPESA_PAY_BILL_CALLBACK)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue vatQueue() {
        return QueueBuilder.durable(QUEUE_VAT)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue crdbQueue() {
        return QueueBuilder.durable(QUEUE_CRDB)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean
    public Queue vendorCallbackQueue() {
        return QueueBuilder.durable(QUEUE_VENDOR_CALLBACK)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    @Bean
    public Queue failedDepositsQueue() {
        return QueueBuilder.durable(QUEUE_FAILED_DEPOSITS)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    // Exchange definitions
    @Bean
    public DirectExchange smscExchange() {
        return new DirectExchange(EXCHANGE_SMSC);
    }

    @Bean
    public DirectExchange halopesaExchange() {
        return new DirectExchange(EXCHANGE_HALOPESA);
    }

    @Bean
    public DirectExchange halopesaInitsExchange() {
        return new DirectExchange(EXCHANGE_HALOPESA_INITS);
    }

    @Bean
    public DirectExchange halopesaPayBillExchange() {
        return new DirectExchange(EXCHANGE_HALOPESA_PAY_BILL);
    }

    @Bean
    public DirectExchange mpesaPayBillExchange() {
        return new DirectExchange(EXCHANGE_MPESA_PAY_BILL);
    }

    @Bean
    public DirectExchange airtelMoneyPayBillExchange() {
        return new DirectExchange(EXCHANGE_AIRTEL_MONEY_PAY_BILL);
    }

    @Bean
    public DirectExchange tigopesaPayBillExchange() {
        return new DirectExchange(EXCHANGE_TIGOPESA_PAY_BILL);
    }

    @Bean
    public DirectExchange mpesaExchange() {
        return new DirectExchange(EXCHANGE_MPESA);
    }

    @Bean
    public DirectExchange mpesaInitExchange() {
        return new DirectExchange(EXCHANGE_MPESA_INIT);
    }
    @Bean
    public DirectExchange tigopesaExchange() {
        return new DirectExchange(EXCHANGE_TIGOPESA);
    }
    @Bean
    public DirectExchange tigopesaInitsExchange() {
        return new DirectExchange(EXCHANGE_TIGOPESA_INITS);
    }

    @Bean
    public DirectExchange airtelMoneyExchange() {
        return new DirectExchange(EXCHANGE_AIRTEL_MONEY);
    }
    @Bean
    public DirectExchange airtelMoneyInitExchange() {
        return new DirectExchange(EXCHANGE_AIRTEL_MONEY_INIT);
    }
    @Bean
    public DirectExchange mpesaCongoExchange() {
        return new DirectExchange(EXCHANGE_MPESA_CONGO);
    }
    @Bean
    public DirectExchange mpesaCongoInitExchange() {
        return new DirectExchange(EXCHANGE_MPESA_CONGO_INIT);
    }
    @Bean
    public DirectExchange airtelMoneyCongoExchange() {
        return new DirectExchange(EXCHANGE_AIRTEL_MONEY_CONGO);
    }
    @Bean
    public DirectExchange airtelMoneyCongoInitExchange() {
        return new DirectExchange(EXCHANGE_AIRTEL_MONEY_CONGO_INIT);
    }

    @Bean
    public DirectExchange orangeMoneyCongoExchange() {
        return new DirectExchange(EXCHANGE_ORANGE_MONEY_CONGO);
    }
    @Bean
    public DirectExchange orangeMoneyCongoInitExchange() {
        return new DirectExchange(EXCHANGE_ORANGE_MONEY_CONGO_INIT);
    }

    @Bean
    public DirectExchange halopesaPayBillValidationExchange() {
        return new DirectExchange(EXCHANGE_HALOPESA_PAY_BILL_VALIDATION);
    }

    @Bean
    public DirectExchange mpesaPayBillValidationExchange() {
        return new DirectExchange(EXCHANGE_MPESA_PAY_BILL_VALIDATION);
    }

    @Bean
    public DirectExchange airtelMoneyPayBillValidationExchange() {
        return new DirectExchange(EXCHANGE_AIRTEL_MONEY_PAY_BILL_VALIDATION);
    }

    @Bean
    public DirectExchange tigopesaPayBillValidationExchange() {
        return new DirectExchange(EXCHANGE_TIGOPESA_PAY_BILL_VALIDATION);
    }

    @Bean
    public DirectExchange mpesaPayBillCallbackExchange() {
        return new DirectExchange(EXCHANGE_MPESA_PAY_BILL_CALLBACK);
    }

    @Bean
    public DirectExchange vatExchange() {
        return new DirectExchange(EXCHANGE_VAT);
    }

    @Bean
    public DirectExchange crdbExchange() {
        return new DirectExchange(EXCHANGE_CRDB);
    }

    @Bean
    public DirectExchange vendorCallbackExchange() {
        return new DirectExchange(EXCHANGE_VENDOR_CALLBACK);
    }
    @Bean
    public DirectExchange failedDepositsExchange() {
        return new DirectExchange(EXCHANGE_FAILED_DEPOSITS);
    }


    // Bindings
    @Bean
    public Binding smscBinding(Queue smscQueue, DirectExchange smscExchange) {
        return BindingBuilder.bind(smscQueue)
                .to(smscExchange)
                .with(ROUTING_KEY_SMSC);
    }

    @Bean
    public Binding halopesaBinding(Queue halopesaQueue, DirectExchange halopesaExchange) {
        return BindingBuilder.bind(halopesaQueue)
                .to(halopesaExchange)
                .with(ROUTING_KEY_HALOPESA);
    }

    @Bean
    public Binding halopesaInitsBinding(Queue halopesaInitsQueue, DirectExchange halopesaInitsExchange) {
        return BindingBuilder.bind(halopesaInitsQueue)
                .to(halopesaInitsExchange)
                .with(ROUTING_KEY_HALOPESA_INITS);
    }

    @Bean
    public Binding halopesaPayBillBinding(Queue halopesaPayBillQueue, DirectExchange halopesaPayBillExchange) {
        return BindingBuilder.bind(halopesaPayBillQueue)
                .to(halopesaPayBillExchange)
                .with(ROUTING_KEY_HALOPESA_PAY_BILL);
    }

    @Bean
    public Binding mpesaPayBillBinding(Queue mpesaPayBillQueue, DirectExchange mpesaPayBillExchange) {
        return BindingBuilder.bind(mpesaPayBillQueue)
                .to(mpesaPayBillExchange)
                .with(ROUTING_KEY_MPESA_PAY_BILL);
    }

    @Bean
    public Binding airtelMoneyPayBillBinding(Queue airtelMoneyPayBillQueue, DirectExchange airtelMoneyPayBillExchange) {
        return BindingBuilder.bind(airtelMoneyPayBillQueue)
                .to(airtelMoneyPayBillExchange)
                .with(ROUTING_KEY_AIRTEL_MONEY_PAY_BILL);
    }

    @Bean
    public Binding tigopesaPayBillBinding(Queue tigopesaPayBillQueue, DirectExchange tigopesaPayBillExchange) {
        return BindingBuilder.bind(tigopesaPayBillQueue)
                .to(tigopesaPayBillExchange)
                .with(ROUTING_KEY_TIGOPESA_PAY_BILL);
    }

    @Bean
    public Binding vodacomBinding(Queue mpesaQueue, DirectExchange mpesaExchange) {
        return BindingBuilder.bind(mpesaQueue)
                .to(mpesaExchange)
                .with(ROUTING_KEY_MPESA);
    }

    @Bean
    public Binding vodacomInitBinding(Queue mpesaInitQueue, DirectExchange mpesaInitExchange) {
        return BindingBuilder.bind(mpesaInitQueue)
                .to(mpesaInitExchange)
                .with(ROUTING_KEY_MPESA_INIT);
    }
    @Bean
    public Binding tigopesaBinding(Queue tigopesaQueue, DirectExchange tigopesaExchange) {
        return BindingBuilder.bind(tigopesaQueue)
                .to(tigopesaExchange)
                .with(ROUTING_KEY_TIGOPESA);
    }
    @Bean
    public Binding tigopesaInitsBinding(Queue tigopesaInitsQueue, DirectExchange tigopesaInitsExchange) {
        return BindingBuilder.bind(tigopesaInitsQueue)
                .to(tigopesaInitsExchange)
                .with(ROUTING_KEY_TIGOPESA_INITS);
    }
    @Bean
    public Binding airtelMoneyBinding(Queue airtelMoneyQueue, DirectExchange airtelMoneyExchange) {
        return BindingBuilder.bind(airtelMoneyQueue)
                .to(airtelMoneyExchange)
                .with(ROUTING_KEY_AIRTEL_MONEY);
    }
    @Bean
    public Binding airtelMoneyInitBinding(Queue airtelMoneyInitQueue, DirectExchange airtelMoneyInitExchange) {
        return BindingBuilder.bind(airtelMoneyInitQueue)
                .to(airtelMoneyInitExchange)
                .with(ROUTING_KEY_AIRTEL_MONEY_INIT);
    }

    @Bean
    public Binding mpesaCongoBinding(Queue mpesaCongoQueue, DirectExchange mpesaCongoExchange) {
        return BindingBuilder.bind(mpesaCongoQueue)
                .to(mpesaCongoExchange)
                .with(ROUTING_KEY_MPESA_CONGO);
    }
    @Bean
    public Binding mpesaCongoInitBinding(Queue mpesaCongoInitQueue, DirectExchange mpesaCongoInitExchange) {
        return BindingBuilder.bind(mpesaCongoInitQueue)
                .to(mpesaCongoInitExchange)
                .with(ROUTING_KEY_MPESA_CONGO_INIT);
    }
    @Bean
    public Binding airtelMoneyCongoBinding(Queue airtelMoneyCongoQueue, DirectExchange airtelMoneyCongoExchange) {
        return BindingBuilder.bind(airtelMoneyCongoQueue)
                .to(airtelMoneyCongoExchange)
                .with(ROUTING_KEY_AIRTEL_MONEY_CONGO);
    }
    @Bean
    public Binding airtelMoneyCongoInitBinding(Queue airtelMoneyCongoInitQueue, DirectExchange airtelMoneyCongoInitExchange) {
        return BindingBuilder.bind(airtelMoneyCongoInitQueue)
                .to(airtelMoneyCongoInitExchange)
                .with(ROUTING_KEY_AIRTEL_MONEY_CONGO_INIT);
    }

    @Bean
    public Binding orangeMoneyCongoBinding(Queue orangeMoneyCongoQueue, DirectExchange orangeMoneyCongoExchange) {
        return BindingBuilder.bind(orangeMoneyCongoQueue)
                .to(orangeMoneyCongoExchange)
                .with(ROUTING_KEY_ORANGE_MONEY_CONGO);
    }
    @Bean
    public Binding orangeMoneyCongoInitBinding(Queue orangeMoneyCongoInitQueue, DirectExchange orangeMoneyCongoInitExchange) {
        return BindingBuilder.bind(orangeMoneyCongoInitQueue)
                .to(orangeMoneyCongoInitExchange)
                .with(ROUTING_KEY_ORANGE_MONEY_CONGO_INIT);
    }

    @Bean
    public Binding halopesaPayBillValidationBinding(Queue halopesaPayBillValidationQueue, DirectExchange halopesaPayBillValidationExchange) {
        return BindingBuilder.bind(halopesaPayBillValidationQueue)
                .to(halopesaPayBillValidationExchange)
                .with(ROUTING_KEY_HALOPESA_PAY_BILL_VALIDATION);
    }

    @Bean
    public Binding mpesaPayBillValidationBinding(Queue mpesaPayBillValidationQueue, DirectExchange mpesaPayBillValidationExchange) {
        return BindingBuilder.bind(mpesaPayBillValidationQueue)
                .to(mpesaPayBillValidationExchange)
                .with(ROUTING_KEY_MPESA_PAY_BILL_VALIDATION);
    }

    @Bean
    public Binding airtelMoneyPayBillValidationBinding(Queue airtelMoneyPayBillValidationQueue, DirectExchange airtelMoneyPayBillValidationExchange) {
        return BindingBuilder.bind(airtelMoneyPayBillValidationQueue)
                .to(airtelMoneyPayBillValidationExchange)
                .with(ROUTING_KEY_AIRTEL_MONEY_PAY_BILL_VALIDATION);
    }

    @Bean
    public Binding tigopesaPayBillValidationBinding(Queue tigopesaPayBillValidationQueue, DirectExchange tigopesaPayBillValidationExchange) {
        return BindingBuilder.bind(tigopesaPayBillValidationQueue)
                .to(tigopesaPayBillValidationExchange)
                .with(ROUTING_KEY_TIGOPESA_PAY_BILL_VALIDATION);
    }

    @Bean
    public Binding mpesaPayBillCallbackBinding(Queue mpesaPayBillCallbackQueue, DirectExchange mpesaPayBillCallbackExchange) {
        return BindingBuilder.bind(mpesaPayBillCallbackQueue)
                .to(mpesaPayBillCallbackExchange)
                .with(ROUTING_KEY_MPESA_PAY_BILL_CALLBACK);
    }

    @Bean
    public Binding vatBinding(Queue vatQueue, DirectExchange vatExchange) {
        return BindingBuilder.bind(vatQueue)
                .to(vatExchange)
                .with(ROUTING_KEY_VAT);
    }

    @Bean
    public Binding crdbBinding(Queue crdbQueue, DirectExchange crdbExchange) {
        return BindingBuilder.bind(crdbQueue)
                .to(crdbExchange)
                .with(ROUTING_KEY_CRDB);
    }

    @Bean
    public Binding vendorCallbackBinding(Queue vendorCallbackQueue, DirectExchange vendorCallbackExchange) {
        return BindingBuilder.bind(vendorCallbackQueue)
                .to(vendorCallbackExchange)
                .with(ROUTING_KEY_VENDOR_CALLBACK);
    }

    @Bean
    public Binding failedDepositsBinding(Queue failedDepositsQueue, DirectExchange failedDepositsExchange) {
        return BindingBuilder.bind(failedDepositsQueue)
                .to(failedDepositsExchange)
                .with(ROUTING_KEY_FAILED_DEPOSITS);
    }

    // RabbitTemplate for Payment Gateway
    @Bean
    public RabbitTemplate smscRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_SMSC);
        template.setRoutingKey(ROUTING_KEY_SMSC);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    // RabbitTemplate for API
    @Bean
    public RabbitTemplate halopesaRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_HALOPESA);
        template.setRoutingKey(ROUTING_KEY_HALOPESA);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    // RabbitTemplate for Halopesa Inits
    @Bean
    public RabbitTemplate halopesaInitsRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_HALOPESA_INITS);
        template.setRoutingKey(ROUTING_KEY_HALOPESA_INITS);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate halopesaPayBillRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_HALOPESA_PAY_BILL);
        template.setRoutingKey(ROUTING_KEY_HALOPESA_PAY_BILL);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate mpesaPayBillRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_MPESA_PAY_BILL);
        template.setRoutingKey(ROUTING_KEY_MPESA_PAY_BILL);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate airtelMoneyPayBillRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_AIRTEL_MONEY_PAY_BILL);
        template.setRoutingKey(ROUTING_KEY_AIRTEL_MONEY_PAY_BILL);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate tigopesaPayBillRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_TIGOPESA_PAY_BILL);
        template.setRoutingKey(ROUTING_KEY_TIGOPESA_PAY_BILL);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    // RabbitTemplate for Vodacom
    @Bean
    public RabbitTemplate vodacomRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_MPESA);
        template.setRoutingKey(ROUTING_KEY_MPESA);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate vodacomInitRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_MPESA_INIT);
        template.setRoutingKey(ROUTING_KEY_MPESA_INIT);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate tigopesaRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_TIGOPESA);
        template.setRoutingKey(ROUTING_KEY_TIGOPESA);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate tigopesaInitsRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_TIGOPESA_INITS);
        template.setRoutingKey(ROUTING_KEY_TIGOPESA_INITS);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate airtelMoneyRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_AIRTEL_MONEY);
        template.setRoutingKey(ROUTING_KEY_AIRTEL_MONEY);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate airtelMoneyInitRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_AIRTEL_MONEY_INIT);
        template.setRoutingKey(ROUTING_KEY_AIRTEL_MONEY_INIT);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate mpesaCongoRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(EXCHANGE_MPESA_CONGO);
        template.setRoutingKey(ROUTING_KEY_MPESA_CONGO);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate mpesaCongoInitRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(EXCHANGE_MPESA_CONGO_INIT);
        template.setRoutingKey(ROUTING_KEY_MPESA_CONGO_INIT);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate airtelMoneyCongoRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_AIRTEL_MONEY_CONGO);
        template.setRoutingKey(ROUTING_KEY_AIRTEL_MONEY_CONGO);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate airtelMoneyCongoInitRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_AIRTEL_MONEY_CONGO_INIT);
        template.setRoutingKey(ROUTING_KEY_AIRTEL_MONEY_CONGO_INIT);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate orangeMoneyCongoRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_ORANGE_MONEY_CONGO);
        template.setRoutingKey(ROUTING_KEY_ORANGE_MONEY_CONGO);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate orangeMoneyCongoInitRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_ORANGE_MONEY_CONGO_INIT);
        template.setRoutingKey(ROUTING_KEY_ORANGE_MONEY_CONGO_INIT);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate halopesaPayBillValidationTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setExchange(EXCHANGE_HALOPESA_PAY_BILL_VALIDATION);
        template.setRoutingKey(ROUTING_KEY_HALOPESA_PAY_BILL_VALIDATION);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate mpesaPayBillValidationTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setExchange(EXCHANGE_MPESA_PAY_BILL_VALIDATION);
        template.setRoutingKey(ROUTING_KEY_MPESA_PAY_BILL_VALIDATION);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate airtelMoneyPayBillValidationTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setExchange(EXCHANGE_AIRTEL_MONEY_PAY_BILL_VALIDATION);
        template.setRoutingKey(ROUTING_KEY_AIRTEL_MONEY_PAY_BILL_VALIDATION);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate tigopesaPayBillValidationTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setExchange(EXCHANGE_TIGOPESA_PAY_BILL_VALIDATION);
        template.setRoutingKey(ROUTING_KEY_TIGOPESA_PAY_BILL_VALIDATION);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate rabbitTemplateMpesaPayBillCallback() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setExchange(EXCHANGE_MPESA_PAY_BILL_CALLBACK);
        template.setRoutingKey(ROUTING_KEY_MPESA_PAY_BILL_CALLBACK);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate vatRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(EXCHANGE_VAT);
        template.setRoutingKey(ROUTING_KEY_VAT);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate crdbRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_CRDB);
        template.setRoutingKey(ROUTING_KEY_CRDB);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate vendorCallbackRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_VENDOR_CALLBACK);
        template.setRoutingKey(ROUTING_KEY_VENDOR_CALLBACK);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitTemplate failedDepositsRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(EXCHANGE_FAILED_DEPOSITS);
        template.setRoutingKey(ROUTING_KEY_FAILED_DEPOSITS);

        // Set reply timeout
        template.setReplyTimeout(60000); // 60 seconds

        // Set mandatory flag to ensure messages are routed
        template.setMandatory(true);
        return template;
    }

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

}
