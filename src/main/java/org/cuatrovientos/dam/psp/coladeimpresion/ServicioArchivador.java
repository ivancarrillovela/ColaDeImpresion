package org.cuatrovientos.dam.psp.coladeimpresion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ServicioArchivador {
    // Usamos un grupo específico para que reciba copia de todo
    private static final String GROUP_ID = "grupo-archivado"; 
    private static final String TOPIC = "docs-entrada";

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));
        Gson gson = new Gson();

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    JsonObject json = gson.fromJson(record.value(), JsonObject.class);
                    String sender = json.get("sender").getAsString();
                    
                    // Lógica para guardar en una carpeta
                    File directorio = new File("archivador/" + sender);
                    if (!directorio.exists()) directorio.mkdirs();
                    
                    try (FileWriter writer = new FileWriter(new File(directorio, "doc_" + System.currentTimeMillis() + ".json"))) {
                        writer.write(record.value());
                        System.out.println("Archivo guardado para: " + sender);
                    }
                }
            }
        } finally {
            consumer.close();
        }
    }
}
