@startuml

actor "Usuario" as User



node "Cliente Web (Frontend)" {

&nbsp; \[Navegador] as Browser

}



node "API Layer (FastAPI)" {

&nbsp; \[API Layer] as API

}



node "Backend (Workers y Servicios)" {

&nbsp; \[UploadService] as UploadService

&nbsp; \[JobQueue (RabbitMQ)] as JobQueue

&nbsp; \[StaticWorker 1] as StaticWorker1

&nbsp; \[StaticWorker 2] as StaticWorker2

&nbsp; \[DynamicWorker 1] as DynamicWorker1

&nbsp; \[DynamicWorker 2] as DynamicWorker2

&nbsp; \[ModelService] as ModelService

&nbsp; \[ReportService] as ReportService

&nbsp; \[ObjectStore (S3/MinIO)] as ObjectStore

&nbsp; \[MetadataDB] as MetadataDB

}



User --> Browser : Interactúa con la aplicación

Browser --> API : Solicita análisis y resultados

API --> UploadService : Sube APK

UploadService --> JobQueue : Encola trabajo

JobQueue --> StaticWorker1 : Asigna trabajo

JobQueue --> StaticWorker2 : Asigna trabajo



StaticWorker1 --> ModelService : Ejecuta análisis estático

StaticWorker2 --> ModelService : Ejecuta análisis estático



API --> JobQueue : Encola trabajo de análisis dinámico

JobQueue --> DynamicWorker1 : Asigna trabajo dinámico

JobQueue --> DynamicWorker2 : Asigna trabajo dinámico



DynamicWorker1 --> ModelService : Ejecuta análisis dinámico

DynamicWorker2 --> ModelService : Ejecuta análisis dinámico



ModelService --> ObjectStore : Almacena artefactos

ModelService --> MetadataDB : Almacena resultados



ReportService --> ObjectStore : Recupera artefactos

ReportService --> MetadataDB : Genera reporte

ReportService --> API : Retorna resultados



@enduml



