FROM openjdk:17-jdk-alpine
ARG JAR_FILE=target/drivers-1.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]