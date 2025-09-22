@startuml

!define RECTANGLE class



actor "Usuario" as User



node "Frontend (Cliente Web)" {

&nbsp;   \[Browser] as Browser

}



node "API Layer (FastAPI)" {

&nbsp;   \[API Layer] as API

}



node "Backend (Workers y Servicios)" {

&nbsp;   node "Workers Estáticos" {

&nbsp;       \[StaticWorker1] as StaticWorker1

&nbsp;       \[StaticWorker2] as StaticWorker2

&nbsp;       \[StaticWorker3] as StaticWorker3

&nbsp;   }



&nbsp;   node "Workers Dinámicos" {

&nbsp;       \[DynamicWorker1] as DynamicWorker1

&nbsp;       \[DynamicWorker2] as DynamicWorker2

&nbsp;   }



&nbsp;   node "Almacenamiento" {

&nbsp;       \[ObjectStore (S3/MinIO)] as ObjectStore

&nbsp;       \[MetadataDB] as MetadataDB

&nbsp;   }



&nbsp;   node "Servicios" {

&nbsp;       \[UploadService] as UploadService

&nbsp;       \[JobQueue (RabbitMQ/Redis)] as JobQueue

&nbsp;       \[StaticAnalyzer] as StaticAnalyzer

&nbsp;       \[FeatureBuilder] as FeatureBuilder

&nbsp;       \[ModelService] as ModelService

&nbsp;       \[DynamicRunner] as DynamicRunner

&nbsp;       \[ReportService] as ReportService

&nbsp;   }

}



User --> Browser : Interactúa con la aplicación

Browser --> API : Solicita análisis y resultados

API --> UploadService : Sube APK

UploadService --> JobQueue : Encola trabajo

JobQueue --> StaticWorker1 : Asigna trabajo estático

JobQueue --> StaticWorker2 : Asigna trabajo estático

JobQueue --> StaticWorker3 : Asigna trabajo estático



StaticWorker1 --> StaticAnalyzer : Ejecuta análisis estático

StaticWorker2 --> StaticAnalyzer : Ejecuta análisis estático

StaticWorker3 --> StaticAnalyzer : Ejecuta análisis estático

StaticAnalyzer --> FeatureBuilder : Extrae características

FeatureBuilder --> ModelService : Clasificación

ModelService --> MetadataDB : Almacena metadatos



API --> JobQueue : Encola trabajos de análisis

DynamicWorker1 --> DynamicRunner : Ejecuta análisis dinámico

DynamicWorker2 --> DynamicRunner : Ejecuta análisis dinámico

DynamicRunner --> MetadataDB : Almacena resultados dinámicos

DynamicRunner --> ObjectStore : Almacena artefactos



ReportService --> ObjectStore : Recupera artefactos

ReportService --> MetadataDB : Genera reporte

ReportService --> API : Retorna resultados



@enduml



