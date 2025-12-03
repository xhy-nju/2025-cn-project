@echo off
chcp 65001 >nul
echo ========================================
echo   编译 Simple HTTP Server 项目
echo ========================================

cd /d %~dp0

if not exist out mkdir out

echo 正在编译Java文件...
javac -encoding UTF-8 -d out ^
    src\common\*.java ^
    src\model\*.java ^
    src\server\request\*.java ^
    src\server\response\*.java ^
    src\server\router\*.java ^
    src\server\handler\*.java ^
    src\server\mime\*.java ^
    src\server\*.java ^
    src\client\*.java

if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo 编译成功！
echo.
echo 使用以下命令启动:
echo   启动服务器: run_server.bat
echo   启动客户端: run_client.bat
pause
