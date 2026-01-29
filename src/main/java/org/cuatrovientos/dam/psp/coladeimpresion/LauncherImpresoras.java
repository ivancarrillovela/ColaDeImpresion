package org.cuatrovientos.dam.psp.coladeimpresion;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherImpresoras {

    public static void main(String[] args) {
        // Creamos una serie de hilos para gestionar las impresoras, 3 en B/N y 2 de Color = 5 hilos
        ExecutorService executor = Executors.newFixedThreadPool(5);

        System.out.println(">>> INICIANDO SISTEMA DE IMPRESIÓN <<<");

        // Lanzamos 3 Impresoras B/N
        executor.submit(new TareaImpresora("BN", "ImpresoraBN_1"));
        executor.submit(new TareaImpresora("BN", "ImpresoraBN_2"));
        executor.submit(new TareaImpresora("BN", "ImpresoraBN_3"));

        // Lanzamos 2 Impresoras Color
        executor.submit(new TareaImpresora("Color", "ImpresoraColor_1"));
        executor.submit(new TareaImpresora("Color", "ImpresoraColor_2"));
    }

    // Esta clase interna define lo que hace cada hilo (cada impresora)
    public static class TareaImpresora implements Runnable {
        private final String tipo;
        private final String id;

        public TareaImpresora(String tipo, String id) {
            this.tipo = tipo;
            this.id = id;
        }

        @Override
        public void run() {
            String topic = tipo.equals("Color") ? "docs-color" : "docs-bn";
            // Es importante que usemos el mismo GroupID para impresoras del mismo tipo (reparto de carga)
            String groupId = tipo.equals("Color") ? "grupo-impresoras-color" : "grupo-impresoras-bn";

            Properties props = new Properties();
            props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(topic));

            System.out.println("[" + id + "] Lista y esperando en topic: " + topic);

            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        // Simulamos el tiempo de impresión
                        Thread.sleep(500); 
                        
                        // Guardamos en disco (simulación física)
                        File directorio = new File("impresiones/" + tipo + "/" + id);
                        if (!directorio.exists()) directorio.mkdirs();

                        String fileName = "print_" + System.currentTimeMillis() + "_" + record.offset() + ".txt";
                        try (FileWriter writer = new FileWriter(new File(directorio, fileName))) {
                            writer.write(record.value());
                            System.out.println("🖨️ [" + id + "] Imprimió el documento: " + record.offset());
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
}
