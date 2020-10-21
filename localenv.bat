set BUILD_LIB=%~dp0build-lib

md %BUILD_LIB% 2> NUL

set REPO_URL=https://repo1.maven.org/maven2

set DEPS=junit/junit/4.12/junit-4.12.jar ^
         org/junit/jupiter/junit-jupiter-api/5.6.2/junit-jupiter-api-5.6.2.jar ^
         org/junit/platform/junit-platform-commons/1.6.2/junit-platform-commons-1.6.2.jar ^
         org/apiguardian/apiguardian-api/1.1.0/apiguardian-api-1.1.0.jar ^
         org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar

for %%i in (%DEPS%) do (
    set LOCAL_DEP=%BUILD_LIB%\%%~nxi
    if not exist !LOCAL_DEP! powershell -Command "& { Invoke-WebRequest -OutFile !LOCAL_DEP! %REPO_URL%/%%i }"
)

for /f %%i in ('dir /b %BUILD_LIB%') do set THIRD_PARTY_DEPS=!THIRD_PARTY_DEPS!;%BUILD_LIB%\%%i
