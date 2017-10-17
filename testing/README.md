Testing
=======

To run a dockerized Telegraf instance that receives InfluxDB line protocol on
http://localhost:8086 and prints it out on stdout run:

```
docker run -p 8086:8086 -v $PWD/telegraf.conf:/etc/telegraf/telegraf.conf:ro telegraf
```

To start a vanilla DropWizard app configured for sending metrics to the Telegraf do:

```
git clone https://github.com/dropwizard/dropwizard
cd dropwizard/dropwizard-example
git apply <<EOF
diff --git a/dropwizard-example/example.yml b/dropwizard-example/example.yml
index 22aa696ee..af3c52679 100644
--- a/dropwizard-example/example.yml
+++ b/dropwizard-example/example.yml
@@ -61,7 +61,7 @@ logging:
 
     # Redirects SQL logs to a separate file
     org.hibernate.SQL:
-      level: DEBUG
+      level: INFO
 
 # Logback's Time Based Rolling Policy - archivedLogFilenamePattern: /tmp/application-%d{yyyy-MM-dd}.log.gz
 # Logback's Size and Time Based Rolling Policy -  archivedLogFilenamePattern: /tmp/application-%d{yyyy-MM-dd}-%i.log.gz
@@ -69,14 +69,14 @@ logging:
 
   appenders:
     - type: console
-    - type: file
-      threshold: INFO
-      logFormat: "%-6level [%d{HH:mm:ss.SSS}] [%t] %logger{5} - %X{code} %msg %n"
-      currentLogFilename: /tmp/application.log
-      archivedLogFilenamePattern: /tmp/application-%d{yyyy-MM-dd}-%i.log.gz
-      archivedFileCount: 7
-      timeZone: UTC
-      maxFileSize: 10MB
+      #- type: file
+      #- threshold: INFO
+      #- logFormat: "%-6level [%d{HH:mm:ss.SSS}] [%t] %logger{5} - %X{code} %msg %n"
+      #- currentLogFilename: /tmp/application.log
+      #- archivedLogFilenamePattern: /tmp/application-%d{yyyy-MM-dd}-%i.log.gz
+      #- archivedFileCount: 7
+      #- timeZone: UTC
+      #- maxFileSize: 10MB
 
 # the key needs to match the suffix of the renderer
 viewRendererConfiguration:
@@ -86,8 +86,9 @@ viewRendererConfiguration:
 
 metrics:
   reporters:
-    - type: graphite
-      host: localhost
-      port: 2003
-      prefix: example
-      frequency: 1m
+    - type: influxdb
+      database: dropwizard
+      port: 8086
+      frequency: 10s
+      rateUnit: SECONDS
+      durationUnit: MILLISECONDS
diff --git a/dropwizard-example/pom.xml b/dropwizard-example/pom.xml
index ef37f6177..aab8dd9c3 100644
--- a/dropwizard-example/pom.xml
+++ b/dropwizard-example/pom.xml
@@ -81,6 +81,11 @@
         <dependency>
             <groupId>io.dropwizard</groupId>
             <artifactId>dropwizard-metrics-graphite</artifactId>
+          </dependency>
+        <dependency>
+            <groupId>com.izettle</groupId>
+            <artifactId>dropwizard-metrics-influxdb</artifactId>
+            <version>1.2.1-SNAPSHOT</version>
         </dependency>
         <dependency>
             <groupId>com.h2database</groupId>
diff --git a/dropwizard-example/src/main/java/com/example/helloworld/resources/PeopleResource.java b/dropwizard-example/src/main/java/com/example/helloworld/resources/PeopleResource.java
index 40ad6d29b..050c675b0 100644
--- a/dropwizard-example/src/main/java/com/example/helloworld/resources/PeopleResource.java
+++ b/dropwizard-example/src/main/java/com/example/helloworld/resources/PeopleResource.java
@@ -1,5 +1,6 @@
 package com.example.helloworld.resources;
 
+import com.codahale.metrics.annotation.Timed;
 import com.example.helloworld.core.Person;
 import com.example.helloworld.db.PersonDAO;
 import io.dropwizard.hibernate.UnitOfWork;
@@ -23,12 +24,14 @@ public class PeopleResource {
 
     @POST
     @UnitOfWork
+    @Timed
     public Person createPerson(Person person) {
         return peopleDAO.create(person);
     }
 
     @GET
     @UnitOfWork
+    @Timed
     public List<Person> listPeople() {
         return peopleDAO.findAll();
     }
EOF

mvn package -DskipTests
java -jar target/dropwizard-example-*-SNAPSHOT.jar db migrate example.yml
java -jar target/dropwizard-example-*-SNAPSHOT.jar server example.yml
```
