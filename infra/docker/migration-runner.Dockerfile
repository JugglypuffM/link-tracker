FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.10.11_3.3.5 AS build

WORKDIR /app

COPY build.sbt ./
COPY project/ ./project/

RUN sbt update

COPY modules/migration-runner/ ./modules/migration-runner/

RUN sbt migration-runner/stage

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/modules/migration-runner/target/universal/stage/ /app/

COPY migrations/ /app/migrations/

ENTRYPOINT ["/app/bin/migration-runner"]
