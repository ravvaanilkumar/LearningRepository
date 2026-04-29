@echo off

echo === Building naming-server ===
cd naming-server
call mvnw.cmd spring-boot:build-image "-Dspring-boot.build-image.imageName=naming-server:latest" -DskipTests
cd ..

echo === Building config-server ===
cd config-server
call mvnw.cmd spring-boot:build-image "-Dspring-boot.build-image.imageName=config-server:latest" -DskipTests
cd ..

echo === Building currency-exchange ===
cd currency-exchange
call mvnw.cmd spring-boot:build-image "-Dspring-boot.build-image.imageName=currency-exchange:latest" -DskipTests
cd ..

echo === Building currency-conversion-service ===
cd currency-conversion-service
call mvnw.cmd spring-boot:build-image "-Dspring-boot.build-image.imageName=currency-conversion-service:latest" -DskipTests
cd ..

echo === Building api-gateway ===
cd api-gateway
call mvnw.cmd spring-boot:build-image "-Dspring-boot.build-image.imageName=api-gateway:latest" -DskipTests
cd ..

echo === All images built ===