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
set DIST=build-output\dist
set LOG=build-output\log.txt

set MODS=mods

rd /q /s %CLASSES% %DIST% 2> NUL
del %LOG% 2> NUL
md %CLASSES% %DIST%

for /f %%i in ('dir /b /s %MODS%\*.java') do set SRC=!SRC! %%i
%JAVAC% --module-path %THIRD_PARTY_DEPS% --module-source-path %MODS% -d %CLASSES% %SRC% >> %LOG% 2>&1 || (
    echo compilation failed
    goto :eof
)

rem Uncomment when ConsoleLauncher finally works with JPMS
rem %JAVA% --module-path %CLASSES%;%3RD_PARTY_DEPS% --module org.junit.platform.console/org.junit.platform.console.ConsoleLauncher --scan-modules --disable-banner --details=none

%JLINK% --module-path "%CLASSES%;%JAVA_HOME%\jmods" --add-modules surpass.api,surpass.core,surpass.persist,surpass.gui --output %DIST%\surpass --launcher surpass=surpass.gui/org.esoul.surpass.gui.Main ^
      --strip-debug --compress 2 --no-header-files --no-man-pages >> %LOG% 2>&1 || (
    echo jlink failed
    goto :eof
)

echo done
