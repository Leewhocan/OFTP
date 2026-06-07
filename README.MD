````markdown
# OTP Authentication Service

Сервис генерации и валидации OTP-кодов с JWT-аутентификацией и ролевой моделью доступа. Поддерживает多渠道 рассылку кодов: Email, SMS, Telegram и сохранение в файл.

## Стек технологий

- Java 21
- Spring Boot 3.3.5 (Web, Security, Data JPA, Mail)
- Spring Security 6.3.4 (JWT-аутентификация, ролевая модель)
- PostgreSQL 17
- JJWT 0.12.6
- Gradle 8.10
- Docker / Docker Compose
- MailDev (эмулятор Email)
- SMPPSim (эмулятор SMS/SMPP)
- WireMock (мок Telegram Bot API)
- Lombok
- Logback (логирование)

## Требования

- JDK 21
- Docker и Docker Compose
- 4 ГБ свободной RAM

## Быстрый старт

### 1. Клонирование проекта

### 2. Запуск инфраструктуры

```bash
docker-compose up -d --build
```
````

Контейнеры:

| Сервис        | Порт                   | Назначение       |
| ------------- | ---------------------- | ---------------- |
| PostgreSQL 17 | 5432                   | База данных      |
| MailDev       | 1080 (UI), 1025 (SMTP) | Эмулятор почты   |
| SMPPSim       | 2775 (SMPP), 8989 (UI) | Эмулятор SMS     |
| WireMock      | 8089                   | Мок Telegram API |

Проверка статуса:

```bash
docker ps
```

### 3. Сборка и запуск приложения

```bash
./gradlew bootRun
```

Приложение запускается на порту 8080.

### 4. Остановка

```bash

docker-compose down


docker-compose down -v
```

## Архитектура приложения

Проект построен по трёхслойной архитектуре:

- **Слой API (Controllers)** — обработка HTTP-запросов. AuthController, UserController, AdminController.
- **Слой сервисов (Services)** — бизнес-логика. JwtService, OtpService, OtpChannelService.
- **Слой доступа к данным (DAO)** — работа с БД. UserRepository, OtpRepository.

Дополнительные компоненты:

- **Security** — JWT-фильтр, конфигурация доступа по ролям.
- **Scheduler** — планировщик просроченных OTP-кодов с интервалом 30 секунд.
- **Channels** — стратегии отправки OTP: Email (MailDev), SMS (SMPPSim), Telegram (WireMock), File.

## Ролевая модель

| Роль       | Доступ                                                  |
| ---------- | ------------------------------------------------------- |
| ROLE_ADMIN | Все эндпоинты, управление пользователями, настройка OTP |
| ROLE_USER  | Генерация и валидация OTP, просмотр профиля             |

Первый зарегистрированный пользователь получает роль ROLE_ADMIN. Все последующие — ROLE_USER. Регистрация второго администратора невозможна.

## API Reference

Все запросы (кроме `/api/auth/**`) требуют заголовок:

```
Authorization: Bearer <jwt_token>
```

### Аутентификация

#### Регистрация

```
POST /api/auth/register
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

Ответ 200:

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ1c2VyMSIsInJvbGUiOiJST0xFX1VTRVIiLCJpYXQiOjE3ODA4MzU5MjEsImV4cCI6MTc4MDkyMjMyMX0.xxx"
}
```

Ответ 400:

```json
{
  "error": "Username already exists"
}
```

#### Логин

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

Ответ 200:

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9..."
}
```

Ответ 401:

```json
{
  "error": "Invalid credentials"
}
```

### Пользовательские эндпоинты (ROLE_USER / ROLE_ADMIN)

#### Профиль текущего пользователя

```
GET /api/user/profile
```

Ответ 200:

```json
{
  "username": "user1",
  "role": "[ROLE_USER]"
}
```

#### Список доступных каналов отправки

```
GET /api/user/otp/channels
```

Ответ 200:

```json
{
  "channels": ["email", "sms", "telegram", "file"]
}
```

#### Генерация OTP-кода

```
POST /api/user/otp/generate?channel={channel}
Content-Type: application/json

{
  "operationId": "string"
}
```

Параметры запроса:

| Параметр | Тип   | По умолчанию | Описание                                        |
| -------- | ----- | ------------ | ----------------------------------------------- |
| channel  | query | all          | Канал отправки: email, sms, telegram, file, all |

Ответ 200:

```json
{
  "operationId": "login-confirm-123",
  "expiresAt": "2026-06-07T20:30:00.894656",
  "channels": ["email", "sms", "telegram", "file"]
}
```

Код не возвращается в ответе API. Он доступен только через выбранный канал отправки.

#### Валидация OTP-кода

```
POST /api/user/otp/validate
Content-Type: application/json

{
  "operationId": "string",
  "code": "string"
}
```

Ответ 200:

```json
{
  "message": "OTP is valid"
}
```

Ответ 400:

```json
{
  "error": "Invalid or expired OTP"
}
```

Код используется однократно. После успешной валидации статус меняется на USED. Просроченные коды получают статус EXPIRED.

### Административные эндпоинты (ROLE_ADMIN)

#### Список пользователей (без администраторов)

```
GET /api/admin/users
```

Ответ 200:

```json
[
  {
    "id": 2,
    "username": "user1",
    "roles": ["ROLE_USER"]
  }
]
```

#### Удаление пользователя

```
DELETE /api/admin/users/{id}
```

Ответ 200:

```json
{
  "message": "User and related OTP codes deleted"
}
```

При удалении пользователя также удаляются все его OTP-коды. Администраторы не могут быть удалены.

#### Конфигурация OTP

```
GET /api/admin/otp/config
```

Ответ 200:

```json
{
  "length": 6,
  "ttlSeconds": 300
}
```

#### Изменение конфигурации OTP

```
PUT /api/admin/otp/config
Content-Type: application/json

{
  "length": 8,
  "ttlSeconds": 600
}
```

Ответ 200:

```json
{
  "message": "OTP config updated",
  "length": 8,
  "ttlSeconds": 600
}
```

| Параметр   | Тип | По умолчанию | Описание                    |
| ---------- | --- | ------------ | --------------------------- |
| length     | int | 6            | Количество цифр в коде      |
| ttlSeconds | int | 300          | Время жизни кода в секундах |

## Каналы отправки OTP

### Email

Используется эмулятор MailDev.

Конфигурация SMTP:

- Хост: localhost
- Порт: 1025
- Аутентификация: отключена

Проверка: интерфейс MailDev доступен по адресу http://localhost:1080.

### SMS

Используется эмулятор SMPPSim.

Конфигурация SMPP:

- Хост: localhost
- Порт: 2775

Проверка: панель управления SMPPSim доступна по адресу http://localhost:8989. В логах приложения отображается строка вида `OTP sent via SMS to user1: 249664`.

### Telegram

Используется WireMock для мокирования Telegram Bot API.

Конфигурация:

- URL API: http://localhost:8089/bot{token}/sendMessage
- Файл моков: wiremock/mappings/telegram-bot.json

Проверка: запросы отображаются по адресу http://localhost:8089/\_\_admin/requests. Код содержится в поле `text` тела запроса.

### File

Код сохраняется в файл `otp_codes.txt` в корне проекта.

Формат записи:

```
2026-06-07T20:21:08.123 | User: user1 | OTP: 249664
```

## Конфигурация приложения

### application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: myuser
    password: mypassword
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  mail:
    host: localhost
    port: 1025

jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration: 86400000

otp:
  length: 6
  ttl-seconds: 300
  scheduler-interval-ms: 30000
```

### docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: mypassword
    ports:
      - "5432:5432"

  maildev:
    image: maildev/maildev
    ports:
      - "1080:1080"
      - "1025:1025"

  smppsim:
    build: ./smppsim
    ports:
      - "2775:2775"
      - "8989:8989"

  wiremock:
    image: wiremock/wiremock:latest
    ports:
      - "8089:8080"
    volumes:
      - ./wiremock:/home/wiremock
```

### smppsim/Dockerfile

```dockerfile
FROM bitsensedev/smpp-sim:latest
RUN sed -i 's/PASSWORDS=pass12345/PASSWORDS=pass123/' /smppsim/conf/smppsim.props \
    && sed -i 's/value="INFO"/value="DEBUG"/' /smppsim/conf/logback.xml \
    && sed -i 's/CAPTURE_SME_DECODED=false/CAPTURE_SME_DECODED=true/' /smppsim/conf/smppsim.props
```

### wiremock/mappings/telegram-bot.json

```json
{
  "request": {
    "method": "POST",
    "urlPathPattern": "/bot.*/sendMessage"
  },
  "response": {
    "status": 200,
    "jsonBody": {
      "ok": true,
      "result": {
        "message_id": 1,
        "from": { "id": 123456, "is_bot": true, "first_name": "DemoBot" },
        "chat": { "id": 789012, "first_name": "User" },
        "text": "OTP sent successfully"
      }
    }
  }
}
```

## Логирование

Логи записываются в консоль и в файлы в директории `logs/`:

- `logs/application.log` — текущий лог
- `logs/application-YYYY-MM-DD.log` — ротация по дням, хранится 7 дней

Формат:

```
2026-06-07 20:11:17 [main] INFO  com.example.demo.DemoApplication - Started DemoApplication in 3.147 seconds
2026-06-07 20:21:08 [http-nio-8080-exec-5] INFO  c.e.d.service.otp.EmailOtpChannel - OTP sent via Email to user1
2026-06-07 20:21:08 [http-nio-8080-exec-5] INFO  c.e.d.service.otp.SmsOtpChannel - OTP sent via SMS to user1: 249664
2026-06-07 20:21:08 [http-nio-8080-exec-5] INFO  c.e.d.service.otp.TelegramOtpChannel - OTP sent via Telegram to user1: 249664
2026-06-07 20:21:08 [http-nio-8080-exec-5] INFO  c.e.d.service.otp.FileOtpChannel - OTP saved to file for user user1
2026-06-07 20:21:23 [scheduling-1] INFO  c.e.d.scheduler.OtpExpirationScheduler - Expired 2 OTP codes
```

## Структура проекта

```
Sirnya/
├── build.gradle
├── settings.gradle
├── docker-compose.yml
├── smppsim/
│   └── Dockerfile
├── wiremock/
│   └── mappings/
│       └── telegram-bot.json
├── otp_codes.txt
├── logs/
└── src/
    └── main/
        ├── java/com/example/demo/
        │   ├── DemoApplication.java
        │   ├── config/
        │   │   └── OtpConfig.java
        │   ├── controller/
        │   │   ├── AuthController.java
        │   │   ├── UserController.java
        │   │   └── AdminController.java
        │   ├── dto/
        │   │   ├── AuthRequest.java
        │   │   ├── AuthResponse.java
        │   │   ├── OtpGenerateRequest.java
        │   │   └── OtpValidateRequest.java
        │   ├── model/
        │   │   ├── User.java
        │   │   └── OtpCode.java
        │   ├── repository/
        │   │   ├── UserRepository.java
        │   │   └── OtpRepository.java
        │   ├── scheduler/
        │   │   └── OtpExpirationScheduler.java
        │   ├── security/
        │   │   ├── JwtService.java
        │   │   ├── JwtAuthenticationFilter.java
        │   │   └── SecurityConfig.java
        │   └── service/
        │       ├── OtpService.java
        │       ├── OtpChannelService.java
        │       └── otp/
        │           ├── OtpChannel.java
        │           ├── EmailOtpChannel.java
        │           ├── SmsOtpChannel.java
        │           ├── TelegramOtpChannel.java
        │           └── FileOtpChannel.java
        └── resources/
            ├── application.yml
            └── logback-spring.xml
```

## Сценарии тестирования

### Сценарий 1: Регистрация администратора и пользователя

1. Регистрация первого пользователя:

```
POST /api/auth/register
{"username": "admin1", "password": "admin123"}
```

Ожидаемый результат: ответ 200, в JWT токене `role: ROLE_ADMIN`.

2. Регистрация второго пользователя:

```
POST /api/auth/register
{"username": "user1", "password": "123456"}
```

Ожидаемый результат: ответ 200, в JWT токене `role: ROLE_USER`.

3. Попытка повторной регистрации:

```
POST /api/auth/register
{"username": "user1", "password": "123456"}
```

Ожидаемый результат: ответ 400, `{"error": "Username already exists"}`.

### Сценарий 2: Ролевая модель доступа

1. Запрос списка пользователей с токеном администратора:

```
GET /api/admin/users
Authorization: Bearer <admin_token>
```

Ожидаемый результат: ответ 200, список пользователей.

2. Запрос списка пользователей с токеном обычного пользователя:

```
GET /api/admin/users
Authorization: Bearer <user_token>
```

Ожидаемый результат: ответ 403 Forbidden.

### Сценарий 3: Генерация OTP по всем каналам

1. Отправка OTP во все каналы:

```
POST /api/user/otp/generate?channel=all
Authorization: Bearer <user_token>
{"operationId": "test-all-channels"}
```

Ожидаемый результат: ответ 200.

2. Проверка каналов:

| Канал    | Метод проверки                           | Ожидаемый результат                        |
| -------- | ---------------------------------------- | ------------------------------------------ |
| Email    | http://localhost:1080                    | Письмо с кодом                             |
| SMS      | Логи приложения                          | Строка `OTP sent via SMS to user1: XXXXXX` |
| Telegram | http://localhost:8089/\_\_admin/requests | POST запрос с кодом в теле                 |
| File     | Файл `otp_codes.txt`                     | Запись с кодом                             |

### Сценарий 4: Валидация OTP

1. Успешная валидация:

```
POST /api/user/otp/validate
Authorization: Bearer <user_token>
{"operationId": "test-all-channels", "code": "249664"}
```

Ожидаемый результат: ответ 200, `{"message": "OTP is valid"}`.

2. Повторная валидация того же кода:
   Ожидаемый результат: ответ 400, `{"error": "Invalid or expired OTP"}`.

3. Валидация с неверным кодом:

```
POST /api/user/otp/validate
{"operationId": "test-all-channels", "code": "000000"}
```

Ожидаемый результат: ответ 400, `{"error": "Invalid or expired OTP"}`.

### Сценарий 5: Управление конфигурацией OTP

1. Просмотр текущей конфигурации:

```
GET /api/admin/otp/config
Authorization: Bearer <admin_token>
```

Ожидаемый результат: ответ 200, `{"length": 6, "ttlSeconds": 300}`.

2. Изменение конфигурации:

```
PUT /api/admin/otp/config
Authorization: Bearer <admin_token>
{"length": 8, "ttlSeconds": 600}
```

Ожидаемый результат: ответ 200.

3. Генерация нового OTP с обновлёнными параметрами:

```
POST /api/user/otp/generate?channel=file
Authorization: Bearer <user_token>
{"operationId": "test-8digits"}
```

Ожидаемый результат: в файле `otp_codes.txt` код из 8 цифр.

### Сценарий 6: Удаление пользователя

1. Удаление пользователя:

```
DELETE /api/admin/users/{user_id}
Authorization: Bearer <admin_token>
```

Ожидаемый результат: ответ 200.

2. Проверка списка пользователей после удаления:

```
GET /api/admin/users
Authorization: Bearer <admin_token>
```

Ожидаемый результат: удалённый пользователь отсутствует в списке.

3. Попытка логина удалённого пользователя:

```
POST /api/auth/login
{"username": "user1", "password": "123456"}
```

Ожидаемый результат: ответ 401.

### Сценарий 7: Просроченные OTP

1. Дождаться истечения срока действия кода (30 секунд по умолчанию или изменить TTL через админ-конфиг).
2. Проверить логи приложения на наличие строки `Expired X OTP codes`.
3. Валидация просроченного кода:

```
POST /api/user/otp/validate
{"operationId": "...", "code": "..."}
```

Ожидаемый результат: ответ 400.

## Устранение неполадок

### Приложение не запускается

Проверить статус Docker-контейнеров:

```bash
docker ps
```

Должны быть 4 контейнера в статусе Up.

Проверить версию Java:

```bash
java -version
```

Требуется Java 21.

Пересобрать проект:

```bash
./gradlew clean compileJava
```

### 403 Forbidden на все запросы

Проверить наличие заголовка `Authorization: Bearer <token>`.
Проверить срок действия токена (24 часа).
Получить новый токен через `/api/auth/login`.

### Ошибка подключения к БД

```bash
docker-compose down -v
docker-compose up -d
```

### Письма не отображаются в MailDev

Проверить, что порт 1025 указан в `application.yml` (`spring.mail.port: 1025`).
Проверить логи приложения на наличие ошибок отправки.

### WireMock не показывает запросы

Проверить наличие файла `wiremock/mappings/telegram-bot.json`.
Перезапустить WireMock:

```bash
docker-compose restart wiremock
```
