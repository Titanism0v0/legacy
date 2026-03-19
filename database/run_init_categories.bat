@echo off
chcp 65001 >nul
set /p MYSQL_USER=MySQL username (default: root): 
if "%MYSQL_USER%"=="" set MYSQL_USER=root
echo Run: mysql -u %MYSQL_USER% -p --default-character-set=utf8mb4 overseas_purchase < "%~dp0init_categories.sql"
mysql -u %MYSQL_USER% -p --default-character-set=utf8mb4 overseas_purchase < "%~dp0init_categories.sql"
pause
