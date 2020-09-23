#!/bin/sh

echo "********************************************************"
echo "Waiting for the batch server to start on address $BATCH_HOST:$ALIVE_PORT"
echo "********************************************************"
while ! `nc -z $BATCH_HOST $ALIVE_PORT `; do sleep 3; done
echo ">>>>>>>>>>>> Batch Server has started"

java -Dspring.profiles.active=product,daoproduct -Dspring.jpa.hibernate.ddl-auto=none -Drmi.host=$BATCH_HOST -jar /app.jar
