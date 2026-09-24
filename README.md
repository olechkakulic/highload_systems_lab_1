# lab1-animal-shelter

Лабораторная работа №1: платформа приютов для животных. Один Spring Boot монолит, PostgreSQL, REST API и общая Swagger UI.

Реализованы карточки животных и теги, приюты, профили пользователей, заявки на передачу животных, волонтёрские смены и участие в них. Исходная тема, сценарии и распределение работы взяты из предоставленного документа «лаб 1 то что есть.pdf».

- [Условия курса](https://github.com/Discipliny/highload_systems)
- [Соответствие каждому пункту ТЗ](docs/requirements.md)
- [Схема БД, статусы и обоснование транзакций](docs/architecture.md)
- [Порядок демонстрации и распределение работы](docs/defense.md)
- [Результаты выполненных проверок](docs/verification.md)

## Стек

Java 21, Maven Wrapper 3.9.16, Spring Boot 3.5.16, Spring Data JPA, PostgreSQL 17, Flyway, Bean Validation, springdoc-openapi 2.8.17, JUnit Jupiter, Testcontainers 1.21.4, JaCoCo 0.8.15. Версии библиотек, не указанные явно в `pom.xml`, управляются BOM Spring Boot.

Исходники компилируются под Java 21. Для разработки подойдёт установленный JDK 21 или 25; Docker использует JDK/JRE 21.

## Запуск всего приложения

Нужен работающий Docker с Compose:

```bash
docker compose up --build -d --wait
```

Если Docker установлен без Compose/Buildx (как в текущем окружении с Colima), используйте:

```bash
./scripts/compose.sh up --build -d --wait
```

Этот запускатель использует установленные плагины, а при их отсутствии загружает официальные Compose 5.5.1 и Buildx 0.37.1 в `.tools/` внутри проекта с проверкой SHA256. Глобальные настройки Docker не изменяются. Требуется Python 3. Для остальных команд заменяйте `docker compose` на `./scripts/compose.sh`, например `./scripts/compose.sh logs -f app`.

После старта:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI 3: http://localhost:8080/v3/api-docs
- Состояние: http://localhost:8080/actuator/health
- PostgreSQL: `localhost:5433`, БД `shelter`, пользователь `shelter`, пароль `shelter_local`.

Параметры для локальной лабораторной заданы по умолчанию. Чтобы изменить их, скопируйте `.env.example` в `.env` и отредактируйте значения. `.env` не хранится в Git. Данные PostgreSQL сохраняются в именованном Docker volume.

```bash
docker compose logs -f app
docker compose stop
```

`docker compose down` удаляет контейнеры и сеть, сохраняя данные. Команда с `--volumes` дополнительно удалит БД; она нужна только для намеренного сброса.

## Открытие и запуск в IntelliJ IDEA

1. Откройте папку проекта или `pom.xml` через **File → Open** и импортируйте Maven-проект.
2. В **Project Structure → Project SDK** выберите JDK 21 или установленный JDK 25. Language level — 21.
3. Для запуска Java-процесса из IDEA поднимите БД: `docker compose up -d db`.
4. Запустите сохранённую конфигурацию **Shelter API (IDE, 8081)**. Она использует порт `8081`, поэтому может работать одновременно с Docker-приложением на `8080`.
5. Откройте http://localhost:8081/swagger-ui/index.html.

Если конфигурация ещё не появилась после импорта, запустите `AnimalShelterApplication.main()` с переменной `SERVER_PORT=8081`. В проекте также есть конфигурация **Verify laboratory** для Maven `clean verify`.

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
- JaCoCo объединяет результаты обоих видов тестов. Общий порог покрытия строк **70%** проверяется на фазе `verify`, без исключения бизнес-классов из отчёта.
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

DTO и типы полей доступны в Swagger. Даты смен передаются в ISO 8601 с часовым поясом, например `2027-01-01T10:00:00Z`.

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

### Границы лабораторной №1

Профили имеют роли `APPLICANT`, `VOLUNTEER`, `EMPLOYEE`, `SUPERVISOR`; роль задаётся при создании и пока не меняется. Сотрудник связан с приютом. Проверки роли профиля в бизнес-сценариях определяют, кого можно записать на смену или указать заявителем. Они не проверяют личность вызывающего API.

Роли на use case диаграмме описывают также будущую лабораторную №3. В текущем API нет аутентификации, паролей и проверки прав текущего пользователя. JWT, создание пользователей только супервайзером и ограничения «только свои записи» предстоит добавить в ЛР №3. Микросервисы относятся к ЛР №2, брокеры сообщений и файловый сервис — к ЛР №4.

## Работа в Git

Разработка выполнена в локальной feature-ветке `feature/shelter-monolith`. История использует Conventional Commits. Для продолжения создавайте отдельную ветку под функцию, например `feature/animal-search`, и отправляйте её на взаимную проверку перед слиянием.

Удалённый репозиторий не настроен. Согласование схемы с преподавателем и реальную проверку вторым участником выполняет команда; наличие готового проекта не заменяет эти шаги.
