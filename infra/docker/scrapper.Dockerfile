FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.10.11_3.3.5 AS build

WORKDIR /app

COPY build.sbt ./
COPY project/ ./project/

RUN sbt update

COPY modules/common/ ./modules/common/
COPY modules/scrapper/ ./modules/scrapper/

RUN sbt scrapper/stage

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/modules/scrapper/target/universal/stage/ /app/

ENTRYPOINT ["/app/bin/scrapper"]
