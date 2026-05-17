## Диаграмма контекста системы:
```plantuml
@startuml
!include  https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

LAYOUT_WITH_LEGEND()

Person(user, "User")

System_Boundary(app, "LinkTracker") { 
  Container(bot, "Telegram", "Scala", "Manages settings and receives updates to deliver them to user") {
    Component(bot_server, "Main server")
    Component(bot_publisher, "Outbox publisher")
  }
  ContainerDb(bot_db, "Bot database", "Postgres", "Contains notification outbox letters")
  Container(scrapper, "Scrapper service", "Scala", "Registers users, tracks links") {
    Component(scrapper_server, "Main server")
    Component(scrapper_publisher, "Outbox publisher")
  }
  ContainerDb(scrapper_db, "Scrapper database", "Postgres", "Contains link tracking settings and notification outbox letters")
}

System_Ext(github, "Github")

Rel_R(user, bot, "Use")
Rel_D(bot_server, scrapper_server, "Register, track, untrack, get", "REST")
Rel_R(scrapper_server, scrapper_db, "Save data", "JDBC")
Rel_D(scrapper_server, github, "Check update", "REST")
Rel_R(scrapper_server, scrapper_db, "Save notification to outbox", "JDBC")
Rel_R(scrapper_publisher, scrapper_db, "Read notifications from outbox", "JDBC")
Rel_U(scrapper_publisher, bot_server, "Send update info", "Kafka")
Rel_R(bot_server, bot_db, "Save notification to outbox", "JDBC")
Rel_R(bot_publisher, bot_db, "Read notifications from outbox", "JDBC")
Rel_U(bot_publisher, user, "Send update notification", "REST")
@enduml
```

## Схема таблицы настроек
| Поле      | Тип       | Атрибуты    |
|-----------|-----------|-------------|
| id        | long      | primary key |
| ownerId   | long      | foreign key |
| link      | string    | not null    |
| updatedAt | timestamp | not null    |