@echo off
setlocal EnableDelayedExpansion

rem The following is a trick to output a message without a new line.
set <NUL /p=Surpass build started...

call %~dp0localenv.bat

set JAVAC=javac
set JAVA=java
set JAR=jar
set JLINK=jlink

set CLASSES=build-output\classes
set JARS=build-output\jars
set TEST_JARS=build-output\test-jars
set DIST=build-output\dist
set LOG=build-output\log.txt

set MODS=mods

rd /q /s %CLASSES% %JARS% %TEST_JARS% %DIST% 2> NUL
del %LOG% 2> NUL
md %CLASSES% %JARS% %TEST_JARS% %DIST%

for /f %%i in ('dir /b /s %MODS%\*.java') do set SRC=!SRC! %%i

%JAVAC% --module-path %JUNIT_DEPS% --module-source-path %MODS% -d %CLASSES% %SRC% >> %LOG% 2>&1 || (
    echo compilation failed
    goto :eof
)

%JAR% --create --file=%JARS%\surpass.api.jar -C %CLASSES%\surpass.api .
%JAR% --create --file=%JARS%\surpass.core.jar -C %CLASSES%\surpass.core .
%JAR% --create --file=%JARS%\surpass.persist.jar -C %CLASSES%\surpass.persist .
%JAR% --create --file=%JARS%\surpass.gui.jar --main-class=org.esoul.surpass.gui.Main -C %CLASSES%\surpass.gui .

%JAR% --create --file=%TEST_JARS%\surpass.core.test.jar -C %CLASSES%\surpass.core.test .
%JAR% --create --file=%TEST_JARS%\surpass.persist.test.jar -C %CLASSES%\surpass.persist.test .

%JAVA% --class-path %JARS%\surpass.api.jar;%JARS%\surpass.core.jar;%JARS%\surpass.persist.jar;%TEST_JARS%\surpass.core.test.jar;%TEST_JARS%\surpass.persist.test.jar;%JUNIT_DEPS% org.junit.runner.JUnitCore ^
      org.esoul.surpass.core.test.DataTableTest ^
      org.esoul.surpass.core.test.SimpleCipherTest ^
      org.esoul.surpass.persist.test.LocalFileSystemPersistenceServiceTest >> %LOG% 2>&1 || (
    echo tests failed
    goto :eof
)

%JLINK% --module-path "%JARS%;%JAVA_HOME%\jmods" --add-modules surpass.api,surpass.core,surpass.persist,surpass.gui --output %DIST%\surpass --launcher surpass=surpass.gui/org.esoul.surpass.gui.Main ^
      --strip-debug --compress 2 --no-header-files --no-man-pages >> %LOG% 2>&1 || (
    echo jlink failed
    goto :eof
)

echo done