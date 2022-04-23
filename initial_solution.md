# Services

## Tasks tracking
Запрос на "Заасанить задачи" должен тригерить ReassignTaskAction для каждой октрытой задачи. ReassignTaskAction отправляется в топик "tasks" в MQ, c KEY_SHARED подпиской, где ключ - id задачи. Это означает что все сообщения для конкретной задачи будут становиться в очередь и обрабатываться по порядку. Это снимает необходимость использования распределенного pessimistic lock.
Запрос на завершение задачи должен тригерить CompleteTaskAction который также отправляется в топик "tasks" в MQ. Это сообщение будет попадать в ту же очередь что и ReassignTaskAction для конкретной задачи.
Если у нас будет ситуация что первым пришел запрос на закрытие задачи, после него на реассайн, то реассайн выполнен не будет и наоборот.

Успешный реассайн задачи должен тригерить TaskAssignedEvent ивент который будет отправляться в топик "profile" MQ.
Успешное завершение задачи должно тригерить TaskCompletedEvent.

## Accounting
Подписывается на TaskAssignedEvent, TaskCompletedEvent. При обработке вычисляется сумма которая дожна быть списана или зачислена на баланс, добавляется запись в аудит лог сотрудника и обновляется баланс.
Scheduler раз в день (можно сразу после завершения текущего дня) для каждого профиля вычисляет заработанную за день сумму (складывая все списания и зачисления) и отправляет в MQ PayoutEvent.
Обработка PayoutEvent добавляет запись в audit log, обновляет баланс и отправляет email.

Успешное изменение audit log отправляет AccountOperationEvent в топик "account" в MQ

## Analytics
Подписывается на AccountOperationEvent и просто сохраняет каждую операцию.
Для определения заработанной суммы, кол-ва сотрудников ушедших в минус и самой дорогой таски можно просто делать соответствующие запросы.

# Диаграмма взаимодействия сервисов
![service diagram](https://user-images.githubusercontent.com/104152538/164933588-80927768-34cf-4748-abc7-5e58df470d87.png)
Синхронные вызовы показаны сплошной линией, асинхронные - пунктирной.

# Messages

```
ReassignTaskAction
  UID taskId;

CompleteTaskAction
  UID taskId;

TaskAssignedEvent
  UID employeeId;
  string taskDetails;
  timestamp assignedAt;

TaskCompletedEvent
  UID taskId;
  UID employeeId;
  timestamp completedAt;

PayoutEvent
  UID employeeId;
  double amount;

AccountOperationEvent
  AccountOperation accountOperation;
  UID employeeId;
  UID taskId;
  timestamp operationAt;
```

# Topics:
```
/tasks - KEY_SHARED, key is taskId
/profile - KEY_SHARED, key is profileId
/account - SHARED
```

```
KEY_SHARED - same consumer always consumes messages with the same id in order
SHARED - any consumer may consume any message 
```

# Entities:

```
Task
  string description;
  TaskStatus status; {OPEN, COMPLETED}
  UID assigneeId;

Employee 
  List<UID> assignedTasks;

Account
  UID employeeId;
  List<AccountOperation> auditLog;
  double balance;

AccountOperation
  double amount;
  OperationType operationType; {DEBIT, CREDIT, PAYOUT}
```

# Отказоусточивость
Если возникает проблема при обработке сообщения из MQ (проблема с сетью, компонент обрабатывающий сообщения упал) то не будет отправлен acknowledge на это сообщение и оно будет оставаться в MQ пока не обработается (пока не появится сеть или не поднимется компонент). Для этого необходимо по возможности сделать операции обрабатывающие сообщения идемпотентными, чтобы ретраи не приводили к багам.
Таким образом мы перекладываем сложность обработки ошибок на MQ который должен давать определенные гарантии по обработке сообщений (acknowledge, сохранение сообщений пока они не обработаются, сохранение сообщений даже если упадет сам MQ)

# Сложности
Пока не понятно как реализовать авторизацию и аутентификацию. Не понятно где хранить профили и их роли, т.к. они нужны для всех сервисов и авторизации.
