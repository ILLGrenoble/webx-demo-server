FROM openjdk:21-jdk AS builder

ARG MAVEN_OPTS

WORKDIR /app

COPY . .

RUN ./mvnw clean package -B -DskipTests=true $MAVEN_OPTS

FROM amazoncorretto:21-alpine

WORKDIR /app

# copy built application
COPY --from=builder /app/target/webx-demo.jar /app

CMD ["java", "-jar", "/app/webx-demo.jar"]

EXPOSE 8080
