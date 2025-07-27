@echo off
REM Wrapper to execute Allure CLI from standard Scoop or Chocolatey paths

REM  Checks Allure installed via Scoop:
set "ALLURE_PATH=%USERPROFILE%\scoop\apps\allure\current\bin\allure.bat"
if exist "%ALLURE_PATH%" (
call "%ALLURE_PATH%" %*
exit /b %ERRORLEVEL%
)

REM  Checks Allure installed via Chocolatey:
set "ALLURE_PATH=%ProgramData%\chocolatey\bin\allure.bat"
if exist "%ALLURE_PATH%" (
call "%ALLURE_PATH%" %*
exit /b %ERRORLEVEL%
)

REM  Checks Allure available in system PATH:
where allure >nul 2>&1
if %ERRORLEVEL%==0 (
allure %*
exit /b %ERRORLEVEL%
)

REM  If not found, gives friendly error:
echo.
echo  Allure CLI not found. Please install Allure via Scoop or Chocolatey or add it to your PATH.
exit /b 1