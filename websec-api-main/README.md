# websec-api

Spring Boot REST API за websec апликацију.

## Покретање

Исто као на часу:

```bash
./mvnw clean package -DskipTests
java -jar target/web-security.jar
```

Или преко systemd сервиса:

```bash
sudo systemctl start websec-api
```

## Безбедносне измене (грана `vuln/idor`)

### 1. IDOR — Broken Access Control

**Проблем:** Сваки аутентификовани корисник могао је да приступи филму по произвољном ID-у (`GET /movie/{id}`), без обзира чији је филм.

**Решење:**
- `Movie` ентитет добио поље `user_id` (FK ка `User`) — Hibernate аутоматски додаје колону при старту (`ddl-auto=update`).
- `MovieService.getMovieById(id, requestingUserId)` проверава власништво пре него što врати ресурс.
- Ако филм постоји али припада другом кориснику → **403 Forbidden**.
- Ако филм не постоји → **404 Not Found**.
- Филмови без власника (`user_id = NULL`) доступни су свим корисницима (backward compatibility).

Погођени фајлови:
- `persistence/Movie.java`
- `persistence/MovieRepository.java`
- `service/MovieService.java`
- `facade/MovieFacade.java`
- `api/MovieController.java`
- `exception/WebSecForbiddenException.java` *(нов)*
- `exception/GlobalExceptionHandler.java` *(нов)*

---

### 2. Брут-форс / dictionary attack заштита

**Метода:** Ескалирајућа блокада налога (in-memory, по email адреси).

| Узастопних неуспелих покушаја | Блокада |
|-------------------------------|---------|
| 1–2                           | нема    |
| 3                             | 5 секунди |
| 4                             | 15 секунди |
| 5                             | 30 секунди |
| 6+                            | 60 секунди |

Након успешне пријаве бројач се **ресетује**.

API враћа **HTTP 429 Too Many Requests** са хедером `Retry-After` и пољем `retryAfterSeconds` у телу одговора.

UI приказује одбројавање у реалном времену и онемогућава дугме за пријаву током блокаде.

> **Напомена:** Бројачи су чувани in-memory. При рестарту апликације се ресетују.

Погођени фајлови:
- `service/LoginAttemptService.java` *(нов)*
- `facade/AuthenticationFacade.java`
- `exception/WebSecTooManyAttemptsException.java` *(нов)*
- `exception/GlobalExceptionHandler.java` *(нов)*
