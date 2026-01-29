package org.cuatrovientos.dam.psp.coladeimpresion;

import com.google.gson.Gson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

public class EmpleadoProductor {
    private static final String TOPIC = "docs-entrada";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    public static void main(String[] args) {
        // Configuración del Productor
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        Gson gson = new Gson();

        try {
            // Simulamos el envío de un documento
            Map<String, String> mensaje = new HashMap<>();
            mensaje.put("titulo", "Informe Anual");
            mensaje.put("documento", "Este es un texto muy largo que simula ser un documento...");
            mensaje.put("tipo", "B/N"); // O "Color" si lo quisieramos
            mensaje.put("sender", "Miguel Goyena");

            String json = gson.toJson(mensaje);

            // Enviar mensaje
            producer.send(new ProducerRecord<>(TOPIC, json));
            System.out.println("Documento enviado por empleado: " + json);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            producer.flush();
            producer.close();
        }
    }
}
