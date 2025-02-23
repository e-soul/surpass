@echo off

set SCRIPT_DIR=%~dp0
if %SCRIPT_DIR:~-1%==\ set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%
REM set VM_OPTIONS=-Dorg.esoul.surpass.persist.datadir="%SCRIPT_DIR%"
REM set VM_OPTIONS=%VM_OPTIONS% -Dorg.esoul.surpass.laf=com.formdev.flatlaf.FlatLightLaf
"%SCRIPT_DIR%\jre\bin\javaw" %VM_OPTIONS% -p "%SCRIPT_DIR%\mods" --add-modules ALL-MODULE-PATH,ALL-SYSTEM -m surpass.gui/org.esoul.surpass.gui.Main %*
