# SearchEngine
Поисковой движок для сайта

*Интрукция к запуску:* 
- Добавить settings.xml и заполнить Private-Token (Пример settingsExample.xml)
- Установить и запустить MySql сервер. Создать на сервере базу данных с именем search_engine
- Установить целевые целевые сайты в application.yml (поля indexing-settings.sites)
- Установить креды пользователя для БД в application.yml (поля spring.datasource)
- Запустить приложение (mvn spring-boot:run)
- Локальный адрес http://localhost:8080/

*Метод добавления псевдонима для лемы:*
- curl --location 'localhost:8080/api/addAlias?word=samsung&alias=самсунг'