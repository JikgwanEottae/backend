# ====== BUILD STAGE ======
FROM eclipse-temurin:17-jdk as build
WORKDIR /app
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies || true
COPY . .
RUN ./gradlew clean bootJar -x test

# ====== RUNTIME STAGE ======
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*-SNAPSHOT.jar /app/app.jar
EXPOSE 8080

# Timezone & JVM options (TZ is respected by many base images)
ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT:-8080} --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-default}"]
