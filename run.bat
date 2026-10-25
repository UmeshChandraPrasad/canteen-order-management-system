@echo off
REM Run the Canteen Order Management System (Windows).
REM Run compile.bat first. The DB must be up (see README.md).
cd /d "%~dp0"
java -cp "out;lib\mysql-connector-j.jar" com.canteen.ui.CanteenApp
