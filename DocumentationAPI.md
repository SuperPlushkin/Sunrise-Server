# 🌅 Sunrise API Documentation (УСТАРЕЛА)

Полная документация по REST API для общения с сервером.

## 📋 Обзор API

- **Базовый URL:** `http://95.154.89.8:8888/app`
- **Формат данных:** JSON or Text
- **Аутентификация:** JWT токен (требуется для защищённых endpoints)

### 🚀 Быстрый старт

```http
# 1. Регистрация пользователя
POST /auth/register
{
  "username": "SuperPlushkin",
  "password": "123456Au", // Исправленно специально для Великого Ивана Дунаева
  "name": "Kirill",
  "email": "ya.krutoj@gmail.com"
}

# 2. Авторизация
POST /auth/login
{
  "username": "SuperPlushkin",   // обязательно, 4-30 символов
  "password": "123456Au"    // обязательно, минимум 6 символов
}

# 3. Использование API с токеном (получение первых 10 пользователей с фильтром "kiril")
GET /user/getmany?limited=10&offset=0&filter=kiril
Authorization: Bearer <ваш_токен>

# 4. ГОТОВО!!!
```

## 🔐 Аутентификация

### Регистрация

- **Метод:** POST /auth/register
- **Описание:** Создаёт нового пользователя в системе 

### 🧾 Тело запроса
```Json
{
  "username": "string",
  "password": "string",
  "name": "string",
  "email": "string"
}
```

### 📥 Пример
```Json
{
  "username": "SuperPlushkin",
  "password": "123456",
  "name": "Kirill",
  "email": "ya.krutoj@gmail.com"
}
```

### Ответы

- **✅ 200 OK:** — Успешно
```Text
User registered successfully
```
- **❌ 400 Bad Request:** — Пример ошибки валидации
```Json
{
  "errors": {
    "username": "Username must be between 4 and 30 characters",
    "password": "Password must be at least 6 characters",
    "name": "Name must be between 4 and 30 characters",
    "email": "Not valid email"
  }
}
```

### Авторизация

- **Метод:** POST /auth/login
- **Описание:** Аутентифицирует пользователя и возвращает JWT токен

### 🧾 Тело запроса
```Json
{
  "username": "string",
  "password": "string"
}
```

### 📥 Пример
```Json
{
  "username": "SuperPlushkin",
  "password": "123456"
}
```

### Ответы

- **✅ 200 OK** — JWT токен
```Text
"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```
- **❌ 400 Bad Request** — Неверные учётные данные
```Text
"Invalid credentials"
```
- **❌ 400 Bad Request** — Пример ошибки валидации
```Json
{
  "errors": {
    "username": "Username must be between 4 and 30 characters",
    "password": "Password must be at least 6 characters"
  }
}
```

## 👤 Действия с пользователями

### Получение списка пользователей

- **Метод:** GET /app/user/getmany
- **Описание:** Возвращает отфильтрованный список пользователей с поддержкой offset и текстового фильтра.

### Поля запроса и валидация:
| Поле    | Тип     | Обязательное | Описание                                                                                                     |
|---------|---------|--------------|--------------------------------------------------------------------------------------------------------------|
| limited | integer | ✅ да         | Количество пользователей в ответе. Значение от 1 до 50                                                       |
| offset  | integer | ❌ нет        | Смещение от начала списка (счет идет в страницах по формуле limited * offset). Значение от 0. По умолчанию 0 |
| filter  | string  | ❌ нет        | Фильтр по имени/логину. По умолчанию — пустая строка.                                                        |

### 📥 Пример
```
GET /user/getmany?limited=10&offset=0&filter=kiril
```

### Ответы

- **✅ 200 OK** — Успешный запрос
```Json
{
  "count": 2,
  "users": [
    {
      "id": 1,
      "username": "gone_don",
      "name": "ShaLava"
    },
    {
      "id": 2,
      "username": "johnny_silverhand",
      "name": "Johnny"
    }
  ]
}
```
- **❌ 400 Bad Request** — Пример ошибки валидации
```Json
{
  "errors": {
    "limited": "limited must be at most 50",
    "offset": "offset must be at least 0"
  }
}
```

## 💬 Управление чатами

> Все endpoints в этом разделе требуют JWT аутентификации

### Создание личного чата

- **Метод:** POST /chat/create-personal
- **Описание:** Создает личный чат между текущим пользователем и другим пользователем

### 🧾 Тело запроса
```Json
{
  "otherUserId": "long"
}
```

### 📥 Пример
```Json
{
  "otherUserId": 2
}
```

### Ответы

- **✅ 200 OK** — Чат создан
```Json
{
  "message": "Personal chat created successfully",
  "chat_id": 15
}
```
- **❌ 400 Bad Request** — Ошибка
```Text
"Cannot create personal chat with yourself"
```


### Создание группового чата

- **Метод:** POST /chat/create-group
- **Описание:** Создает групповой чат с указанными участниками

### 🧾 Тело запроса
```Json
{
  "chatName": "string",
  "memberIds": "long[]"
}
```

### 📥 Пример
```Json
{
  "chatName": "Разработчики проекта",
  "memberIds": [2, 3, 4, 5]
}
```

### Ответы

- **✅ 200 OK** — Группа создана
```Json
{
  "message": "Group chat created successfully",
  "chat_id": 16
}
```
- **❌ 400 Bad Request** — Ошибка
```Text
"Group chat must have at least 3 members total"
```

### Очистка истории чата

- **Метод:** POST /chat/{chatId}/clear-history?clearType={clearType}
- **Описание:** Очищает историю сообщений в чате (для всех участников "FOR_ALL" или только для себя "FOR_SELF")

### 📥 Пример
```
POST /chat/1/clear-history?clearType=FOR_SELF // "FOR_ALL" или "FOR_SELF"
```

### Ответы

- **✅ 200 OK** — История очищена
```Json
{
  "message": "Chat history cleared successfully",
  "affected_messages": 25
}
```
- **❌ 400 Bad Request** — Ошибка
```Text
"Only admin can clear chat history for all"
```

### Восстановление истории чата

- **Метод:** POST /chat/restore-history
- **Описание:** Восстанавливает скрытую историю сообщений (только для себя)

### 🧾 Тело запроса
```Json
{
  "chatId": "long"
}
```

### 📥 Пример
```Json
{
  "chatId": 15
}
```

### Ответы

- **✅ 200 OK** — История восстановлена
```Json
{
  "message": "Chat history restored successfully",
  "restored_messages": 10
}
```
- **❌ 400 Bad Request** — Ошибка
```Text
"User is not a member of this chat"
```

### Добавление участника в группу

- **Метод:** POST /chat/{chatId}/add-member
- **Описание:** Добавляет нового участника в групповой чат (только для админов)

### 🧾 Тело запроса
```Json
{
  "newUserId": "long"
}
```

### 📥 Пример
```Json
{
  "newUserId": 15
}
```

### Ответы

- **✅ 200 OK** — Участник добавлен
```Text
"User added to group successfully"
```
- **❌ 400 Bad Request** — Ошибка
```Text
"Only admin can add members to group"
```

### Выход из чата

- **Метод:** POST /chat/{chatId}/leave
- **Описание:** Выход пользователя из чата

### Ответы

- **✅ 200 OK** — Успешный выход
```Text
"Left chat successfully"
```
- **❌ 400 Bad Request** — Ошибка
```Text
"User is not a member of this chat"
```

### Получение сообщений чата

- **Метод:** GET /chat/{chatId}/messages
- **Описание:** Возвращает сообщения чата с поддержкой offset.

### Поля запроса и валидация:
| Поле    | Тип     | Обязательное | Описание                                                                                                     |
|---------|---------|--------------|--------------------------------------------------------------------------------------------------------------|
| limited | integer | ❌ нет        | Количество сообщений (по умолчанию 50)                                                                       |
| offset  | integer | ❌ нет        | Смещение от начала списка (счет идет в страницах по формуле limited * offset). Значение от 0. По умолчанию 0 |

### 📥 Пример
```
GET /chat/15/messages?limit=20&offset=0
```

### Ответы

- **✅ 200 OK** — Сообщения
```Json
{
  "messages": [
    {
      "messageId": 150,
      "senderId": 1,
      "senderUsername": "user1",
      "text": "Привет всем!",
      "sentAt": "2024-01-15T14:30:00",
      "isDeleted": false,
      "readCount": 3,
      "isHiddenByUser": false
    }
  ],
  "count": 1
}
```

### Отметка сообщения как прочитанного

- **Метод:** POST /chat/{chatId}/mark-read/{messageId}
- **Описание:** Отмечает сообщение как прочитанное текущим пользователем

### Ответы

- **✅ 200 OK** — Успешно
```Text
"Successfully marked message as read"
```
- **❌ 400 Bad Request** — Ошибка
```Text
"User is not a member of this chat"
```

### Получение количества видимых сообщений

- **Метод:** GET /chat/{chatId}/message-count
- **Описание:** Возвращает количество сообщений, видимых пользователю

### Ответы

- **✅ 200 OK** — Количество сообщений
```Json
{
  "count": 135
}
```
- **❌ 400 Bad Request** — Ошибка
```Text
"User is not a member of this chat"
```

### Проверка прав администратора

- **Метод:** GET /chat/{chatId}/is-admin
- **Описание:** Проверяет, является ли пользователь администратором чата

### Ответы

- **✅ 200 OK** — Статус админа
```Json
{
  "is_admin": true
}
```
- **❌ 400 Bad Request** — Ошибка
```Text
"User is not a member of this chat"
```

## 💬 Действия с сообщениями

> Все endpoints в этом разделе требуют JWT аутентификации 

> Будут позже

## 🖥️ Статусы сервера

### Общий статус сервера

- **Метод:** GET /actions/status
- **Описание:** Возвращает текущий статус системы

### Ответы
- **✅ 200 OK** — Возврат статуса
```Json
{
  "status": "🟢 Онлайн",
  "version": "1.0",
  "users": "1"
}
```