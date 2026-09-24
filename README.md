# lab1-animal-shelter

Лабораторная работа №1: платформа приютов для животных. Spring Boot монолит, PostgreSQL, REST API и общая Swagger UI.

- [Условия курса](https://github.com/Discipliny/highload_systems)
- [Соответствие каждому пункту ТЗ](docs/requirements.md)
- [Схема БД, статусы и обоснование транзакций](docs/architecture.md)
- [Порядок демонстрации и распределение работы](docs/defense.md)
- [Результаты выполненных проверок](docs/verification.md)

## Стек

Java 21, Maven Wrapper 3.9.16, Spring Boot 3.5.16, Spring Data JPA, PostgreSQL 17, Flyway, Bean Validation, springdoc-openapi 2.8.17, JUnit Jupiter, Testcontainers 1.21.4, JaCoCo 0.8.15. Версии библиотек, не указанные явно в `pom.xml`, управляются BOM Spring Boot.

Исходники компилируются под Java 21. Docker использует JDK/JRE 21.

## Запуск всего приложения

Нужен работающий Docker с Compose:

```bash
docker compose up --build -d --wait
```

Если Docker установлен без Compose/Buildx, используйте:

```bash
./scripts/compose.sh up --build -d --wait
```

После старта:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI 3: http://localhost:8080/v3/api-docs
- Состояние: http://localhost:8080/actuator/health
- PostgreSQL: `localhost:5433`, БД `shelter`, пользователь `shelter`, пароль `shelter_local`.

Параметры для локальной лабораторной заданы по умолчанию. Чтобы изменить их, скопируйте `.env.example` в `.env` и отредактируйте значения.

```bash
docker compose logs -f app
docker compose stop
```

`docker compose down` удаляет контейнеры и сеть, сохраняя данные. 

## Проверки и покрытие

Нужны Java 21+ и Docker. Maven отдельно устанавливать не нужно.

```bash
./mvnw clean verify
```

На Windows: `mvnw.cmd clean verify`.

На macOS с Colima или другим Docker context можно использовать скрипт, который берёт адрес Docker из текущего контекста:

```bash
./scripts/verify.sh
```

- `*Test` — модульные тесты, выполняются Surefire на фазе `test`.
- `*IT` — интеграционные тесты, выполняются Failsafe на фазах `integration-test` и `verify`.
- Testcontainers поднимает отдельную PostgreSQL 17. Рабочая БД Compose в тестах не используется.
- Все интеграционные тесты обязательны: при недоступном Docker сборка падает, а не пропускает их.
- Отчёт: `target/site/jacoco/index.html`; результаты тестов: `target/surefire-reports` и `target/failsafe-reports`.

Dockerfile собирает исполняемый JAR с пропуском запуска тестов: Testcontainers требует отдельного Docker daemon. Полная проверка выполняется командой выше и в GitHub Actions.

## Быстрая демонстрация

После запуска Compose выполните:

```bash
python3 scripts/demo.py
```

Для Java-процесса из IDEA:

```bash
python3 scripts/demo.py http://localhost:8081
```

Скрипт использует только стандартную библиотеку Python. Он создаёт новый набор демонстрационных данных, проходит оба бизнес-сценария, проверяет ответы и печатает идентификаторы. Повторный запуск создаёт отдельный набор; существующие записи не удаляются.

## Основные адреса API

| Ресурс | Операции |
|---|---|
| `/api/shelters` | POST, GET списка; GET/PUT/DELETE `/{id}` |
| `/api/users` | POST, GET списка; GET/PUT/DELETE `/{id}` |
| `/api/tags` | POST, GET списка; GET/PUT/DELETE `/{id}` |
| `/api/animals` | POST, GET списка с фильтрами; GET/PUT/DELETE `/{id}` |
| `/api/applications` | POST, GET списка; GET/PUT/DELETE `/{id}` |
| `/api/applications/{id}/review` | POST с `{"decision":"APPROVE"}` или `REJECT` |
| `/api/applications/{id}/withdraw` | POST: отозвать активную заявку |
| `/api/applications/{id}/complete` | POST: передать животное по одобренной заявке |
| `/api/shifts` | POST, GET списка; GET/PUT/DELETE `/{id}` |
| `/api/shifts/{id}/cancel` | POST: отменить смену и все активные участия |
| `/api/shifts/{shiftId}/participations` | POST с `{"userId":1}`, GET списка |
| `/api/shifts/{shiftId}/participations/{id}` | GET, DELETE отменённого участия |
| `/api/shifts/{shiftId}/participations/{id}/cancel` | POST: отменить участие |


### Пагинация

У всех списков: `page=0` по умолчанию, `size=20` по умолчанию, допустимый размер `1..50`. Неверные значения возвращают `400`. Сортировка фиксирована по `id ASC`.

```http
GET /api/animals?page=0&size=20&shelterId=1&species=CAT&status=AVAILABLE&tagId=1
```

Тело: `{"items":[...],"page":0,"size":20,"hasNext":true}`. У страниц общее количество **совпавших с фильтром** записей передаётся в `X-Total-Count`.

`GET /api/shifts` возвращает `Slice`: нет общего количества ни в теле, ни в заголовке, и не выполняется SQL COUNT. Для продолжения прокрутки проверяйте `hasNext` и увеличивайте `page`. Фильтры: `shelterId`, `status`. Список заявок фильтруется по `animalId`, `applicantId`.

### Ошибки и статусы

Успешное создание — `201` и `Location`, чтение/изменение — `200`, удаление — `204`, неверные данные — `400`, отсутствующая запись — `404`, конфликт состояния/уникальности/ссылок — `409`.

Повторная запись после отмены восстанавливает существующее участие с тем же id и возвращает `200`. Повторная запись при уже активном участии — `409`.

Ошибки представлены как Problem Detail с `status`, `title`, `detail`; для неверных полей добавляется `errors`. SQL, пароли подключения и stack trace в API не возвращаются.

