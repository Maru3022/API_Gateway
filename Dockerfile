FROM eclipse-temurin:21-jre

# Создаем непривилегированного пользователя и группу
RUN addgroup --system javauser && adduser --system --ingroup javauser javauser

# Копируем JAR файл
COPY target/*.jar /app/app.jar

# Меняем владельца файла и переключаемся на безопасного пользователя
RUN chown javauser:javauser /app/app.jar
USER javauser

# Указываем рабочую директорию
WORKDIR /app

ENTRYPOINT ["java", "-jar", "app.jar"]