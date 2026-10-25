@echo off
REM Compile the Canteen Order Management System (Windows).
REM Requires: Java 17+ (javac on PATH) and MySQL Connector/J in lib\.
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\mysql-connector-j.jar" -d out @sources.txt
del sources.txt
echo Compiled OK -^> out\
