@echo off

set SCRIPT_DIR=%~dp0
if %SCRIPT_DIR:~-1%==\ set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%
REM set VM_OPTIONS=-Dorg.esoul.surpass.persist.datadir="%SCRIPT_DIR%"
"%SCRIPT_DIR%\jre\bin\javaw" %VM_OPTIONS% -Djava.library.path="%SCRIPT_DIR%\jfx" --enable-native-access=javafx.graphics -p "%SCRIPT_DIR%\mods" --add-modules ALL-MODULE-PATH,ALL-SYSTEM -m surpass.gui.jfx/org.esoul.surpass.gui.jfx.Main %*
