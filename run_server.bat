@echo off
chcp 65001 >nul
echo ========================================
echo   启动 HTTP 服务器
echo ========================================
cd /d %~dp0
java -cp out server.HttpServer %1
pause
