package org.cuatrovientos.dam.psp.coladeimpresion;

import com.google.gson.Gson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class SimuladorEmpleados {
    private static final String TOPIC = "docs-entrada";

    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        Gson gson = new Gson();
        Random random = new Random();

        String[] empleados = {"Miguel", "Ana", "Carlos", "Lucía"};
        String[] tipos = {"B/N", "Color"};
        
        System.out.println(">>> GENERANDO TRABAJO DE OFICINA <<<");

        // Generamos 20 documentos aleatorios
        for (int i = 0; i < 20; i++) {
            String empleado = empleados[random.nextInt(empleados.length)];
            String tipo = tipos[random.nextInt(tipos.length)];
            String texto = generarTextoAleatorio(200 + random.nextInt(800)); // Entre 200 y 1000 chars

            Map<String, String> mensaje = new HashMap<>();
            mensaje.put("titulo", "Doc_" + i);
            mensaje.put("documento", texto);
            mensaje.put("tipo", tipo);
            mensaje.put("sender", empleado);

            String json = gson.toJson(mensaje);
            producer.send(new ProducerRecord<>(TOPIC, json));

            System.out.println("📤 Enviado: Doc_" + i + " [" + tipo + "] por " + empleado);
            
            // Pausa pequeña para dar realismo al funcionamiento
            Thread.sleep(1000);
        }

        producer.close();
        System.out.println(">>> SIMULACIÓN DE ENVÍO FINALIZADA <<<");
    }

    private static String generarTextoAleatorio(int longitud) {
        String base = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ";
        StringBuilder sb = new StringBuilder();
        while (sb.length() < longitud) {
            sb.append(base);
        }
        return sb.substring(0, longitud);
    }
}