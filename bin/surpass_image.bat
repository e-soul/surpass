@echo off

set SCRIPT_DIR=%~dp0
if %SCRIPT_DIR:~-1%==\ set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%
REM set VM_OPTIONS="-Dorg.esoul.surpass.persist.datadir=%SCRIPT_DIR%"
"%SCRIPT_DIR%\bin\javaw" %VM_OPTIONS% --enable-native-access=com.formdev.flatlaf,surpass.hello -m surpass.gui/org.esoul.surpass.gui.Main %*
