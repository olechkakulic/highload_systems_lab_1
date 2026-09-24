# Соответствие ЛР №1 требованиям

Источник: [README курса](https://github.com/Discipliny/highload_systems), раздел «Лабораторная работа №1», и предоставленный PDF с темой приютов. Таблица описывает реализованный код, а не утверждает получение согласования преподавателя.

| Требование | Реализация / проверка |
|---|---|
| Монолит Spring Boot | `AnimalShelterApplication`, один исполняемый JAR и один app-контейнер |
| Java/Kotlin, Maven/Gradle/Bazel, стабильные версии | Java 21, Maven Wrapper, фиксированные релизные версии в `pom.xml` |
| Конвенции языка, SOLID/DRY/KISS | Разделение controller/service/repository/model/dto; constructor injection; форматирование google-java-format; общие pagination/error helpers |
| Feature branching, Conventional Commits | Локальная feature-ветка, история Git; процесс продолжения описан в README |
| Модульные и интеграционные тесты, JUnit Jupiter, Testcontainers | `PaginationTest`, `BusinessRulesTest`, `ShelterApiIT`; настоящая PostgreSQL в контейнере |
| Общее покрытие минимум 70% | JaCoCo LINE COVEREDRATIO >= 0.70 на `mvn verify`, без исключений классов из метрики |
| Осмысленный CRUD основных сущностей | REST-контроллеры всех семи сущностей; изменение заявки/участия через допустимые бизнес-действия |
| Правильные HTTP-статусы | 200/201/204/400/404/409, Location при создании; восстановление участия — 200 |
| Spring Data JPA/JDBC | JPA-репозитории, Specifications для каталога животных |
| Валидация контроллеров и Entity | `@Valid` DTO, Bean Validation в Entity, CHECK/FK/UNIQUE в БД |
| Liquibase/Flyway | Flyway `V1__create_shelter_schema.sql`, Hibernate `validate` |
| Интеграционные тесты бизнес-логики | Заявки/передача, запись/отмена, конкурентные запросы, откат транзакции |
| Настройки через переменные окружения | `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`; Compose `.env.example` |
| Сборка Docker, запуск Compose, БД в Docker | Multi-stage Dockerfile, `compose.yaml`, healthchecks и постоянный volume |
| Пагинация каждого findAll, максимум 50 | `Pagination.request`, все коллекционные GET принимают page/size, некорректные размеры — 400 |
| Бесконечная прокрутка без общего числа | `GET /api/shifts`, Spring Data Slice, `hasNext`, без COUNT/total/header |
| Страница с общим числом в HTTP-header | `GET /api/animals`, Page, `X-Total-Count` после фильтрации |
| Минимум две сложные транзакции с объяснением | Передача животного и запись на смену; `docs/architecture.md` |
| Разделение Entity и DTO | `model/*`, предметные пакеты `dto/*`, `DtoMapper`; Entity не являются ответами API |
| Слои и чистая архитектура монолита | API → сервисы → репозитории; предметные сценарии в сервисах |
| Enum в БД строками | `@Enumerated(EnumType.STRING)` и CHECK в SQL |
| Читаемые ошибки | `GlobalExceptionHandler`, Problem Detail, ошибки полей без SQL/stack trace |
| Продумать и согласовать БД | ER-диаграмма и миграция готовы; согласование с преподавателем выполняет команда |
| Many-to-Many | Животные ↔ теги через `animal_tags` |
| One-to-Many / Many-to-One | Приют → животные/смены; животное → заявки; JPA `@ManyToOne` |
| Many-to-Many с дополнительным полем | Пользователи ↔ смены, `ShiftParticipation.status/registeredAt` |
| Общая OpenAPI 3 / Swagger UI | Один `/v3/api-docs`, одна Swagger UI со всеми контроллерами |

Требования ЛР №2–4 не включены в объём первой работы. Целевая ролевая модель из PDF сохранена в профилях и документации; аутентификация и ограничения текущего пользователя появятся в ЛР №3.
