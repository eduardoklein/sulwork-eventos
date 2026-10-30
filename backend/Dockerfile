# Estágio 1: compila com o JDK. As dependências vêm antes do código, para a camada ficar em cache.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
# Os testes usam o Testcontainers, que precisa do Docker. Eles rodam fora da imagem.
RUN ./mvnw -q -B -DskipTests package && mv target/*.jar app.jar

# Estágio 2: roda só o .jar com o JRE, sem compilador e sem código-fonte.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
