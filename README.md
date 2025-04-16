# Bidirectional-ClickHouse-Flat-File-Data-Ingestion-Tool
to Start this project foolow these steps:- 
1) Start your docker desktop
2) run this command:- docker run -d -p 8123:8123 -p 9000:9000 --name clickhouse-server yandex/clickhouse-server
->> above will run clickhouse in detached mode
3) cd backend
4) mvn clean package
5) cd ..
6) docker-compose down
7) docker-compose up --build 
the above command will start your backend
8) go to google and type: - http://localhost:80 your website will start
to close backend press ctrl+c 3 times
 