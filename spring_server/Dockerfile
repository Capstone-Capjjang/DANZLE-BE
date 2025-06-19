FROM gradle:8.5.0-jdk21 AS builder  
WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon        

COPY src src
RUN gradle bootJar --no-daemon             


FROM openjdk:21-slim AS runtime
USER root

RUN apt-get update && apt-get install -y --no-install-recommends \
        redis \
        sudo \
        curl \
        libopencv-dev \
        libavcodec-dev \
        libavformat-dev \
        libavutil-dev \
        libswscale-dev \
        libx264-dev \
        libopenblas-dev \
        libgtk2.0-dev \
        libv4l-dev \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
