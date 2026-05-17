FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S cloudpay && adduser -S cloudpay -G cloudpay
USER cloudpay
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8081
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]