# MedGraph

MedGraph моделирует события эксплуатации медицинского оборудования. Fact Service генерирует события, а Graph Service строит из них цепочки по устройствам и сохраняет историю их изменений.

## Компоненты

| Компонент | Назначение | HTTP | PostgreSQL |
| --- | --- | --- | --- |
| Fact Service | Добавляет и удаляет порции фактов, сохраняет изменения и уведомляет Graph Service | `localhost:8081` | `localhost:15433/facts` |
| Graph Service | Получает изменения, строит цепочки и хранит версии графа и жизненные циклы фактов | `localhost:8082` | `localhost:15434/graphs` |
| pgAdmin | Веб-интерфейс для просмотра обеих баз | `localhost:15051` | — |

Оба сервиса используют Java 21, Spring Boot 3.5, Spring Data JPA с Hibernate и отдельные экземпляры PostgreSQL 17. Схемы версионируются через Flyway. Сервисы обмениваются данными синхронно по HTTP через `RestTemplate`.

## API

Оба приложения предоставляют спецификацию OpenAPI через `GET /v3/api-docs` и статус готовности через `GET /actuator/health`. Интерактивная документация доступна в [Swagger UI Fact Service](http://localhost:8081/swagger-ui/index.html) и [Swagger UI Graph Service](http://localhost:8082/swagger-ui/index.html).

### Fact Service · `http://localhost:8081`

| Метод и путь | Параметры | Результат |
| --- | --- | --- |
| `POST /api/facts/batches` | Обязательный query-параметр `operation` (`ADD` или `DELETE`). Необязательные `size` (от 1 до 100, по умолчанию 3) и `objectId` | `201` и порция изменений после ответа Graph Service. `400` при неверных параметрах, `409`, если для удаления нет подходящих фактов. |
| `GET /internal/facts/batches/{batchId}` | `batchId` — номер порции | Служебный метод. `200` и сохранённая порция изменений либо `404`, если порция отсутствует. |

`objectId` ограничивает операцию одним устройством и принимает от 1 до 64 латинских букв, цифр, дефисов и подчёркиваний. При `DELETE` сервис случайно выбирает до `size` подходящих фактов.

В ответе порции есть `batchId`, время записи `changedAt` и два массива. Элемент `added` содержит `id`, `objectId`, `occurredAt` и `eventType`, а элемент `deleted` содержит `id` и `objectId`.

### Graph Service · `http://localhost:8082`

| Метод и путь | Параметры | Результат |
| --- | --- | --- |
| `GET /api/graph` | — | `200` и список текущих непустых цепочек. |
| `GET /api/graph/objects/{objectId}` | `objectId` — идентификатор устройства. Необязательный query-параметр `date=YYYY-MM-DD` | `200` и текущая цепочка либо её состояние на конец указанного дня UTC. Если цепочки на этот момент нет, массивы узлов и рёбер пустые. |
| `GET /api/graph/objects/{objectId}/history` | `objectId` — идентификатор устройства | `200` и список сохранённых версий цепочки в порядке применения порций. |
| `GET /api/graph/facts/{factId}/lifecycle` | `factId` — UUID факта | `200` и события жизненного цикла факта либо `404`, если факт отсутствует в истории. |
| `POST /internal/graph/batches/{batchId}` | `batchId` — номер порции. Тело запроса не требуется | Служебное уведомление от Fact Service. Graph Service загружает и применяет порцию, затем отвечает `204`. |

Цепочка содержит `objectId`, `batchId`, `changedAt`, `nodes` и `edges`. У узла есть `id`, `occurredAt`, `eventType` и `position`, а у ребра — идентификаторы соседних узлов `from` и `to`. Узлы отсортированы по `occurredAt`, при совпадении — по UUID. Позиции нумеруются с 1. Историческая версия выбирается по времени изменения графа `changedAt`.

События жизненного цикла имеют тип `REGISTERED`, `POSITION_CHANGED` или `DELETED` и содержат время изменения и номер порции. При регистрации заполняется `position`, при смене позиции — `previousPosition` и `position`, при удалении — `previousPosition`.

### Пример

```sh
curl -fsS -X POST 'http://localhost:8081/api/facts/batches?operation=ADD&size=3&objectId=DEVICE-001'
curl -fsS 'http://localhost:8082/api/graph/objects/DEVICE-001/history'
```

## Запуск через Docker Compose

Окружение использует имя Compose-проекта `medgraph-dev`.

```sh
docker compose up --build -d --wait
docker compose ps
curl -fsS --retry 12 --retry-all-errors --retry-delay 2 http://localhost:8081/actuator/health
curl -fsS --retry 12 --retry-all-errors --retry-delay 2 http://localhost:8082/actuator/health
```

Оба сервиса должны вернуть статус `UP` после завершения запуска. Команда `docker compose down` останавливает окружение. Данные PostgreSQL хранятся в отдельных именованных томах.

## Просмотр баз данных через pgAdmin

1. Из корня проекта выполните `docker compose up -d pgadmin`. Compose также запустит обе базы, если они остановлены.
2. Откройте [pgAdmin](http://localhost:15051) и войдите с логином `admin@medgraph.dev` и паролем `medgraph_admin_local`. Для первого запуска эти значения можно задать через `PGADMIN_DEFAULT_EMAIL` и `PGADMIN_DEFAULT_PASSWORD`. Панель доступна на локальном интерфейсе, а её настройки хранятся в отдельном томе.
3. В дереве **Servers → MedGraph** выберите **Fact DB** или **Graph DB**. При подключении PostgreSQL запросит отдельный пароль. Для **Fact DB** используйте `fact_local`, для **Graph DB** — `graph_local`.
4. Откройте **Databases → facts** или **Databases → graphs**, затем **Schemas → public → Tables**. Чтобы посмотреть строки, нажмите на таблицу правой кнопкой и выберите **View/Edit Data → First 100 Rows**.

В базе `facts` основные таблицы — `fact` и `fact_batch`, в базе `graphs` — `graph_snapshot` и `fact_lifecycle_event`.

## Запуск приложений из JAR

Понадобятся JDK 21 и Docker Compose. Проверьте, что `./mvnw -version` показывает Java 21 и что в выбранном JDK есть `javac`. При необходимости укажите путь к JDK через `JAVA_HOME`.

```sh
docker compose up -d --wait fact-db graph-db
./mvnw verify
```

Запустите Graph Service, дождитесь его готовности и запустите Fact Service в другом терминале. Если приложения уже работают в Docker, перед локальным запуском остановите их контейнеры, чтобы освободить порты `8081` и `8082`.

```sh
java -jar graph-service/target/graph-service.jar
```

```sh
java -jar fact-service/target/fact-service.jar
```

Параметры подключения к базам переопределяются через `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` и `SPRING_DATASOURCE_PASSWORD`. Для адресов соседнего сервиса используются `MEDGRAPH_GRAPH_BASE_URL` в Fact Service и `MEDGRAPH_FACT_BASE_URL` в Graph Service. Значения по умолчанию предназначены для локальной разработки.

## Генерация и обмен изменениями

Fact Service случайно выбирает добавление или удаление порции и выдерживает между запусками случайную паузу от 15 до 30 секунд. Если база пуста, он добавляет факты. Каждый факт содержит UUID, время события, идентификатор устройства и тип события. Автоматическая генерация включена по умолчанию. Для локального запуска без неё передайте Fact Service переменную `MEDGRAPH_GENERATOR_ENABLED=false`. При запуске через Compose задайте эту переменную в окружении команды `docker compose up`.

Fact Service в одной транзакции обновляет таблицу `fact` и сохраняет запись в `fact_batch`. После фиксации он отправляет уведомление в Graph Service. Graph Service запрашивает порцию и записывает полную версию цепочки каждого затронутого устройства в `graph_snapshot`, а события жизненного цикла факта — в `fact_lifecycle_event`. При удалении факта позиции пересчитываются и рёбра соединяют оставшихся соседей.

Результаты обоих HTTP-вызовов выводятся в консоль в формате `event=batch_transfer batch_id=… method=… from=… to=… result=… http_status=… added=… deleted=…`. Строки одной порции имеют общий `batch_id`. На уровне `INFO` выводятся успешные вызовы, на уровне `WARN` — ошибки. Прочерк означает, что значение не получено.

```sh
docker compose logs -f fact-service graph-service | grep --line-buffered 'event=batch_transfer'
```
