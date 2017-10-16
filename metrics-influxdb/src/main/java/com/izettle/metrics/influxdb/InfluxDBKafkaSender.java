package com.izettle.metrics.influxdb;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;

public class InfluxDBKafkaSender extends InfluxDbBaseSender {
    private static final String KAFKA_CLIENT_ID = "metrics_influxdb_reporter";
    private final KafkaProducer<byte[], byte[]> kafkaProducer;
    private final String topic;

    public InfluxDBKafkaSender(String database, TimeUnit timePrecision, String measurementPrefix) {
        super(database, timePrecision, measurementPrefix);
        int idx = database.indexOf("@");
        String hosts;
        if (idx != -1) {
            topic = database.substring(0, idx);
            hosts = database.substring(idx + 1);
        } else {
            throw new IllegalArgumentException("invalid database format: " + database +", expected: topic@host1,host2...");
        }
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, hosts);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, KAFKA_CLIENT_ID);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        kafkaProducer = new KafkaProducer<>(props);
    }

    @Override
    protected int writeData(byte[] line) throws Exception {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, null, line);
        kafkaProducer.send(record);
        return 0;
    }
}
