@echo off

set SCRIPT_DIR=%~dp0
REM set VM_OPTIONS=-Dorg.esoul.surpass.persist.datadir=%SCRIPT_DIR%..
"%SCRIPT_DIR%bin\java" %VM_OPTIONS% -m surpass.gui/org.esoul.surpass.gui.Main %*
