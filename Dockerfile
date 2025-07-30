
# Используем образ с Java Development Kit (JDK)
FROM maven:3.8.5-openjdk-17-slim AS build

# Копируем файлы с исходным кодом приложения в контейнер
COPY ./ /app

# Задаем рабочую директорию внутри контейнера
WORKDIR /app

# Кэшируем зависимости
RUN mvn dependency:go-offline

# Выполняем сборку приложения с помощью Maven
RUN mvn clean package -DskipTests

# Создаем новый образ, используя JRE
FROM openjdk:17-jdk-slim

# Копируем собранный JAR файл из предыдущего образа в новый образ
COPY --from=build /app/target/*.jar /app/app.jar

# Задаем точку входа для запуска приложения
ENTRYPOINT ["java","-jar","/app/app.jar"]
