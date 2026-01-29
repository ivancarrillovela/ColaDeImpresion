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

        System.out.println(">>> TRANSFORMADOR (DISTRIBUCIÓN POR DOCUMENTO) INICIADO <<<");

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    JsonObject originalJson = gson.fromJson(record.value(), JsonObject.class);
                    
                    String tipo = originalJson.get("tipo").getAsString();
                    String contenido = originalJson.get("documento").getAsString();
                    // Usamos el titulo como CLAVE DE PARTICIONADO para agrupar las páginas
                    String titulo = originalJson.get("titulo").getAsString(); 
                    
                    originalJson.remove("sender"); 

                    // Dividimos en páginas
                    List<String> paginas = dividirEnPaginas(contenido, 400);
                    String targetTopic = tipo.equals("Color") ? "docs-color" : "docs-bn";

                    System.out.println("Procesando '" + titulo + "' (" + paginas.size() + " paginas) -> Enviado a " + targetTopic);

                    for (int i = 0; i < paginas.size(); i++) {
                        JsonObject paginaJson = originalJson.deepCopy();
                        paginaJson.addProperty("documento", paginas.get(i));
                        paginaJson.addProperty("pagina", (i + 1) + "/" + paginas.size());
                        
                        String jsonSalida = gson.toJson(paginaJson);

                        // La CLAVE es el TITULO, asi Kafka garantiza que todos los mensajes con la misma clave van a la misma impresora
                        producer.send(new ProducerRecord<>(targetTopic, titulo, jsonSalida));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
            producer.close();
        }
    }

    private static List<String> dividirEnPaginas(String texto, int size) {
        List<String> paginas = new ArrayList<>();
        if (texto == null || texto.isEmpty()) return paginas;
        for (int i = 0; i < texto.length(); i += size) {
            paginas.add(texto.substring(i, Math.min(texto.length(), i + size)));
        }
        return paginas;
    }
}