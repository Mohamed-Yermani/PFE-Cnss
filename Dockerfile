# Étape 1 : Image de base avec Java 17
FROM eclipse-temurin:17-jdk-jammy

# Étape 2 : Argument du JAR à copier
ARG JAR_FILE=target/*.jar

# Étape 3 : Copier le JAR dans l’image
COPY ${JAR_FILE} userservice.jar

# Étape 4 : Exposer le port utilisé par l’application
EXPOSE 8089

# Étape 5 : Lancer l’application
ENTRYPOINT ["java", "-jar", "/userservice.jar"]
