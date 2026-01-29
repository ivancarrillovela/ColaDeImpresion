# Sistema de Cola de Impresión Distribuida con Kafka

Este proyecto implementa una arquitectura de mensajería asíncrona para la gestión de colas de impresión. Utiliza **Apache Kafka** para desacoplar la recepción de documentos (Productores) de su procesamiento e impresión física (Consumidores), garantizando escalabilidad y tolerancia a fallos.

## 🏛️ Arquitectura de TOPICS

El sistema se basa en la división de Topics y Particiones para lograr paralelismo eficiente:

| Topic | Particiones | Descripción |
| :--- | :---: | :--- |
| **`docs-entrada`** | 1 | **Cola Global.** Recibe todos los documentos crudos. Al tener 1 partición, garantiza el orden de llegada para el proceso de archivado. |
| **`docs-bn`** | **3** | **Cola de Impresión B/N.** Dimensionada con 3 particiones para permitir que las **3 impresoras B/N** trabajen simultáneamente (paralelismo real). |
| **`docs-color`** | **2** | **Cola de Impresión Color.** Dimensionada con 2 particiones para las **2 impresoras Color**. |

### Estrategia de Enrutamiento
El `ServicioTransformador` utiliza el **Título del Documento** como clave de particionado (Key). Esto garantiza que todas las páginas de un mismo documento vayan siempre a la misma impresora (mismo orden físico), mientras que documentos distintos se reparten entre las impresoras disponibles (balanceo de carga).

---

## 🚀 Manual de Implantación (Despliegue)

Instrucciones para poner en marcha el sistema en un entorno nuevo.

### Prerrequisitos
* **Java JDK 17** o superior.
* **Apache Kafka 4.1.1** (Descomprimido en local).
* **Maven**.

### Pasos de Instalación Automática
El proyecto incluye un script DevOps (`setup_entorno.bat`) que automatiza la configuración de Kafka en modo KRaft (sin Zookeeper).

1.  Abra el archivo `setup_entorno.bat`.
2.  Edite la variable `KAFKA_DIR` (línea 6) con la ruta de instalación de Kafka:
    ```batch
    SET "KAFKA_DIR=C:\kafka\kafka_2.13-4.1.1"
    ```
3.  Ejecute el script `setup_entorno.bat`.
    * El sistema compilará el proyecto.
    * Se formateará el almacenamiento de Kafka (Cluster ID).
    * Se crearán los Topics con las particiones especificadas en la arquitectura.

---

## 💻 Manual del Desarrollador (Ejecución)

Orden de arranque de los componentes Java (desde Eclipse o Terminal):

1.  **`ServicioArchivador`**: Inicia el backup de documentos originales (Grupo: `grupo-archivado`).
2.  **`ServicioTransformador`**: Inicia el procesador y enrutador (Grupo: `grupo-transformacion`).
3.  **`LauncherImpresoras`**: Levanta 5 hilos consumidores que simulan las impresoras físicas.
4.  **`SimuladorEmpleados`**: Ejecutar para generar carga de trabajo simulada.

**Resultados:**
* Los documentos originales se guardan en la carpeta `./archivador`.
* Las impresiones finales se generan en `./impresiones`.

---

## 🔧 Manual de Mantenimiento (Limpieza y Reinicio)

Información crítica para el reinicio del sistema y limpieza de datos.

### Reinicio del Sistema (Cold Restart)
Si Kafka falla o se desea reiniciar el entorno desde cero:

1.  Cierre todas las ventanas de consola (Kafka y Java).
2.  Ejecute de nuevo el script `setup_entorno.bat`.
    * **Advertencia:** El script detectará carpetas de logs antiguas (`/tmp/kafka-logs`, `kraft-combined-logs`) y las **eliminará automáticamente** para evitar inconsistencias de IDs.

### Limpieza de Datos de Aplicación
El script de Kafka limpia la infraestructura (mensajes en cola), pero no los archivos generados por la aplicación Java. Para una limpieza total:

1.  Borre manualmente la carpeta `archivador/` de la raíz del proyecto.
2.  Borre manualmente la carpeta `impresiones/` de la raíz del proyecto.
