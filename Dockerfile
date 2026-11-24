FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY . .

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew --no-daemon clean test bootJar \
    && jar_file="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
    && test -n "$jar_file" \
    && mkdir -p /out \
    && cp "$jar_file" /out/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build --chown=10001:10001 /out/app.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=45.0", "-XX:MaxMetaspaceSize=160m", "-XX:ReservedCodeCacheSize=48m", "-Xss256k", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
