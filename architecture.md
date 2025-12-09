## Диаграмма контекста системы:
```plantuml
@startuml
!include  https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

LAYOUT_WITH_LEGEND()

Person(user, "User")

System_Boundary(app, "LinkTracker"){
  Container(front, "Telegram/Mobile app", "Scala/Swift/Kotlin", "Manages settings and receives updates to deliver them to user")
  Container(settings, "Settings Service", "Scala", "Manages link tracking settings")
  ContainerDb(database, "Settings database", "Postgres", "Contains link tracking settings")
  Container(scrapper, "Scrapper service", "Scala", "Checks link updates")
  Container(notifier, "Notification service", "Scala", "Sends notifications with updates")
}

System_Ext(github, "Github")
System_Ext(stackoverflow, "Stackoverflow")
System_Ext(other, "Other")

Rel_R(user, front, "Use")
Rel_R(front, settings, "Save/Read links", "REST")
Rel_D(settings, database, "Save/Read setting", "JDBC")
Rel_U(scrapper, database, "Check for new settings", "JDBC")
Rel_R(scrapper, github, "Check update", "REST")
Rel_R(scrapper, stackoverflow, "Check update", "REST")
Rel_U(scrapper, notifier, "Save update info", "Kafka")
Rel_R(scrapper, other, "Check update", "REST")
Rel_U(notifier, front, "Send notification", "REST/FCM/APNS")
@enduml
```

## Схема таблицы пользователей
| Поле     | Тип    | Атрибуты    |
|----------|--------|-------------|
| id       | long   | primary key |
| login    | string | not null    |
| password | string | not null    |

## Схема таблицы настроек
| Поле    | Тип    | Атрибуты    |
|---------|--------|-------------|
| id      | long   | primary key |
| ownerId | long   | foreign key |
| link    | string | not null    |