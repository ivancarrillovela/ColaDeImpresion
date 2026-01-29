package org.cuatrovientos.dam.psp.coladeimpresion;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Impresora {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java Impresora <tipo_impresora> <id_impresora>");
            return;
        }
        
        String tipo = args[0]; // "bn" o "color"
        String idImpresora = args[1]; // Por ejemplo "ImpresoraBN_1"
        
        String topic = tipo.equals("color") ? "docs-color" : "docs-bn";
        String groupId = tipo.equals("color") ? "grupo-impresoras-color" : "grupo-impresoras-bn";

        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        System.out.println("Impresora " + idImpresora + " iniciada esperando trabajos en " + topic);

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    // Simulación de impresión guardando las "impresiones"
                    File directorio = new File("impresiones/" + tipo + "/" + idImpresora);
                    if (!directorio.exists()) directorio.mkdirs();
                    
                    try (FileWriter writer = new FileWriter(new File(directorio, "print_" + System.currentTimeMillis() + ".txt"))) {
                        writer.write(record.value());
                        System.out.println(idImpresora + " imprimiendo documento...");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
