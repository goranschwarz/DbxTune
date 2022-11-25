rem @echo off
setlocal

set STARTDIR=%~dp0

call "%STARTDIR%\dbxtune.bat" tailw %*
