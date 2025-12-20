## Диаграмма контекста системы:
```plantuml
@startuml
!include  https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

LAYOUT_WITH_LEGEND()

Person(user, "User")

System_Boundary(app, "LinkTracker"){
  Container(telegram, "Telegram", "Scala", "Manages settings and receives updates to deliver them to user")
  ContainerDb(database, "Settings database", "Postgres", "Contains link tracking settings")
  Container(scrapper, "Scrapper service", "Scala", "Registers users, tracks links")
}

System_Ext(github, "Github")

Rel_R(user, telegram, "Use")
Rel_D(telegram, scrapper, "Register, track, untrack, get", "REST")
Rel_R(scrapper, database, "Save data", "JDBC")
Rel_D(scrapper, github, "Check update", "REST")
Rel_U(scrapper, telegram, "Send update info", "Kafka")
@enduml
```

## Схема таблицы настроек
| Поле      | Тип       | Атрибуты    |
|-----------|-----------|-------------|
| id        | long      | primary key |
| ownerId   | long      | foreign key |
| link      | string    | not null    |
| updatedAt | timestamp | not null    |