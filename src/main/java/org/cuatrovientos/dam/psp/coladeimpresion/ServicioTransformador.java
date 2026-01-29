package org.cuatrovientos.dam.psp.coladeimpresion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.*;

public class ServicioTransformador {
    private static final String INPUT_TOPIC = "docs-entrada";
    // Grupo distinto al archivador para permitir que funcionen en paralelo
    private static final String GROUP_ID = "grupo-transformacion";
    
    public static void main(String[] args) {
        // Configuración del Consumidor
        Properties consumerProps = new Properties();
        consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);

        // Configuración del Productor
        Properties producerProps = new Properties();
        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
        consumer.subscribe(Collections.singletonList(INPUT_TOPIC));
        Gson gson = new Gson();

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    JsonObject originalJson = gson.fromJson(record.value(), JsonObject.class);
                    
                    // Eliminar sender y dividir documento (transformación)
                    String tipo = originalJson.get("tipo").getAsString();
                    String contenido = originalJson.get("documento").getAsString();
                    originalJson.remove("sender"); // Requisito: sender no relevante

                    // Dividir en páginas de 400 carácteres
                    List<String> paginas = dividirEnPaginas(contenido, 400);
                    
                    String targetTopic = tipo.equals("Color") ? "docs-color" : "docs-bn";

                    for (int i = 0; i < paginas.size(); i++) {
                        JsonObject paginaJson = originalJson.deepCopy();
                        paginaJson.addProperty("documento", paginas.get(i));
                        paginaJson.addProperty("pagina", (i + 1) + "/" + paginas.size());
                        
                        producer.send(new ProducerRecord<>(targetTopic, gson.toJson(paginaJson)));
                    }
                    System.out.println("Documento transformado y enviado a " + targetTopic);
                }
            }
        } finally {
            consumer.close();
            producer.close();
        }
    }

    private static List<String> dividirEnPaginas(String texto, int size) {
        List<String> paginas = new ArrayList<>();
        for (int i = 0; i < texto.length(); i += size) {
            paginas.add(texto.substring(i, Math.min(texto.length(), i + size)));
        }
        return paginas;
    }
}
