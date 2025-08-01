# 🔍 Search Engine - Локальный поисковый движок
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-%2300f.svg?style=for-the-badge&logo=mysql&logoColor=white)

**Высокопроизводительный поисковый движок** для сканирования и индексации веб-сайтов с морфологическим поиском и ранжированием результатов.

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

## ⚙️ Быстрый старт

### Предварительные требования
1. Установите:
   - JDK 17+
   - MySQL 8+
2. Создайте БД:
   ```sql
   CREATE DATABASE search_engine;
