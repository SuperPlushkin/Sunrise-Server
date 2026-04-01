# 🌅 Sunrise Messenger Server

<div align="center">

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)

**Серверная часть мессенджера Sunrise на Java Spring Boot**

</div>

## 🚀 Быстрый старт

### Предварительные требования
- **Docker** и **Docker Compose** должны быть установлены на вашей системе

### Запуск приложения

1. **Скачайте Docker образ:**

   ```bash
   docker pull superplushkin/sunrise-server
   ```
   
2. **❗ВАЖНО❗Создайте файлы зависимостей:**<br><br>

   * 📁 Файл окружения (.env) <br><br>

    ```
    Расположение: В корне

    Используйте пример: .env-example как шаблон

    Содержит критически важные настройки и пароли для сервисов
    ```
   
    * 📁 Файлы инициализаций бд (❗обязательно только для запуска full-stack приложения❗) <br><br>
   
    ```
    Расположение: В корне требуется сделать папку init-sql

    В ней нужно создать 3 файла:
    1) 01-tables.sql
    2) 02-functions.sql
    3) 03-grants.sql
   
    (Содержит таблицы, функции и гранты для инициализации бд)
    ```
   
   * 🐳 Файл Docker Compose (docker-compose.yml) <br><br>
   
   ```
   Требуется файл: docker-compose.yml

   Файл требуется поместить в корень запуска (самому создавать не нужно, только скопировать)

   Для запуска только Spring Boot приложения: 
   /Sunrise-Server/docker-compose.only-app.yml
   
   Для запуска full-stack приложения: 
   /Sunrise-Server/docker-compose.full-stack.yml
   
   (Содержит скрипт для запуска приложения и зависимостей)
   ```
   
   Перед сборкой проверьте еще раз наличие всех файлов 😊 <br><br>

3. **Запустите приложение:**

   ```bash
   # Запуск только Spring Boot приложения
    docker compose -f docker-compose.only-app.yml -p sunrise-server up -d
   ```
   или
   ```bash
   # Запуск full-stack приложения
   docker compose -f docker-compose.full-stack.yml -p sunrise-server up -d
   ```

4. Используйте! 🎉

## 📁 Полезные файлы

```
Sunrise-Server/
├── .env-example                    # 📋 Пример конфигурации окружения
├── Dockerfile                      # 🐳 Конфигурация Docker образа
├── DocumentationAPI.md             # 📚 Документация API
├── docker-compose.full-stack.yml   # 🐳 Docker-compose скрипт для запуска всего стека приложения
└── docker-compose.only-app.yml     # 🐳 Docker-compose скрипт для запуска только Spring Boot приложения
```

**Приятного переваривания! ✨**
