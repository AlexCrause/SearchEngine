# 🔍 Search Engine - Локальный поисковый движок
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-%2300f.svg?style=for-the-badge&logo=mysql&logoColor=white)

Привет! Это мой финальный проект курса Java-разработки — полноценный поисковый движок, который умеет сканировать сайты, анализировать контент и находить нужную информацию. Работает локально, под капотом Spring Boot и MySQL.
## ✨ Ключевые возможности
- 🕷️ Многопоточный обход сайтов
- 📚 Индексация HTML-контента
- 🔍 Морфологический поиск (лемматизация)
- 📊 Ранжирование по релевантности
- ⚡️ REST API для управления
- 📈 Статистика индексации

## 🛠 Технологический стек
- **Java 17** - основной язык разработки
- **Spring Boot** - фреймворк приложения
- **MySQL** - хранение индекса и данных
- **Jsoup** - парсинг HTML
- **Apache Lucene Morphology** - морфологический анализ
- **Maven** - управление зависимостями


### Предварительные требования
1. Установите:
   - JDK 17+
   - MySQL 8+
2. Создайте БД:
   ```sql
   CREATE DATABASE search_engine;


### Настройка и запуск
1. Настройте подключение к БД в application.yaml:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/search_engine
    username: user
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver;
```


2. Добавьте сайты для индексации:

```yaml
indexing-settings:
  sites:
    - url: https://example.com
      name: Example Site
```
      
3. Соберите и запустите:

```bash
mvn clean package
java -jar target/search_engine.jar
```



### 🌐 Интерфейс и API
Приложение доступно по адресу:
http://localhost:8080

#### Основные API-методы:
|Метод|Путь|Действие|
|-----|----|--------|
|GET|/api/startIndexing|Запуск индексации|
|GET|/api/stopIndexing|Остановка индексации|
|POST|/api/indexPage|Индексация отдельной страницы|
|GET|/api/statistics|Получение статистики|
|GET|/api/search|Выполнение поискового запроса|


### 📂 Структура проекта
```text
search_engine/
├── config/         - Конфигурационные классы
├── controllers/    - Веб-контроллеры
├── dto/            - Data Transfer Objects
├── model/          - Сущности БД (JPA)
├── repositories/   - Репозитории Spring Data
├── services/       - Бизнес-логика
└── Application.java- Точка входа
```


### 🧠 Алгоритм работы
1. Сканирование сайтов:
   - Многопоточный обход страниц
   - Сохранение HTML-контента
   - Обнаружение и обработка ссылок

2. Индексация:
   - Очистка текста от HTML-тегов
   - Морфологический анализ (лемматизация)
   - Подсчет частоты слов
   - Сохранение в БД (MySQL)

3. Поиск:
   - Обработка поискового запроса
   - Приведение слов к базовой форме
   - Ранжирование по релевантности
   - Формирование сниппетов



### 📊 Примеры запросов
```bash
# Запуск полной индексации
curl http://localhost:8080/api/startIndexing

# Поиск по всем сайтам
curl "http://localhost:8080/api/search?query=программирование"

# Поиск на конкретном сайте
curl "http://localhost:8080/api/search?query=java&site=https://example.com"
```


