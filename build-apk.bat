@echo off
set "JAVA_HOME=D:\huawei\DevEco Studio\jbr"
set "ANDROID_HOME=D:\android-sdk"
set "ANDROID_SDK_ROOT=D:\android-sdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "TEMP_GRADLE_CACHE=C:\Users\kaokao221\.gemini\antigravity\brain\9c9ff90c-78db-4406-a1f1-04dcd322d1ad\scratch\android-gradle-cache"
cd /d "d:\.dev\miniclassisland\android"
call "C:\Users\kaokao221\.gradle\wrapper\dists\gradle-9.3.0-bin\79n14ral3mx1ozqr3csh2u872\gradle-9.3.0\bin\gradle.bat" assembleDebug assembleRelease --project-cache-dir "%TEMP_GRADLE_CACHE%" --no-daemon
if exist "app\build\intermediates" rmdir /s /q "app\build\intermediates"
if exist "app\build\kotlin" rmdir /s /q "app\build\kotlin"
if exist "app\build\tmp" rmdir /s /q "app\build\tmp"
