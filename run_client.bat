@echo off
chcp 65001 >nul
echo ========================================
echo   启动 HTTP 客户端
echo ========================================
cd /d %~dp0
java -cp out client.HttpClientCLI
pause
