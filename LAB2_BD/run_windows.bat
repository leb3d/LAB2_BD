@echo off
chcp 1251 >nul
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\Schoolsearch.java
if errorlevel 1 (
    pause
    exit /b 1
)
java -Dfile.encoding=UTF-8 -Dstdout.encoding=windows-1251 -Dstderr.encoding=windows-1251 -cp out Schoolsearch
pause
