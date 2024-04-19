@echo off

REM ©Copyright 2024 BMC Software. Inc.

echo Opening up BMC Reporting Metadata Studio

REM this will always output the the system console using the debug mode
set CONSOLE=1

REM This starts MetaData Editor(Admin module) with a console output with the following options:

SetLocal EnableDelayedExpansion

set TENANT_ID=
set TENANT_URL=
set AR_HOST=
set AR_RPC_PORT=
set REST_ENDPOINT=
if exist bmcauthconfiguration\\env.properties (
    For /F "tokens=1* delims==" %%A IN (bmcauthconfiguration\\env.properties) DO (
        IF "%%A"=="TENANT_ID" set TENANT_ID=%%B
        IF "%%A"=="TENANT_URL" set TENANT_URL=%%B 
        IF "%%A"=="AR_HOST" set AR_HOST=%%B 
        IF "%%A"=="AR_RPC_PORT" set AR_RPC_PORT=%%B
        IF "%%A"=="REST_ENDPOINT" set REST_ENDPOINT=%%B
    )
) else (
    echo bmcauthconfiguration\\env.properties file doesn't exist
    timeout 2 > NUL
    EXIT
)

CALL :TRIM TENANT_ID
CALL :TRIM TENANT_URL
CALL :TRIM AR_HOST
CALL :TRIM AR_RPC_PORT
CALL :TRIM REST_ENDPOINT

IF "%TENANT_URL%" NEQ "" (
    IF "%AR_RPC_PORT%" NEQ "" (
        IF "%AR_HOST%" NEQ "" (
            IF "%TENANT_ID%" NEQ "" (
                 IF "%REST_ENDPOINT%" NEQ "" (
                     echo "All env variables are populated"
                 ) ELSE (
	             echo REST_ENDPOINT empty
	             timeout 2 > NUL
	             EXIT
                 )
	    ) ELSE (
	        echo TENANT_ID empty
	        timeout 2 > NUL
	        EXIT
	    )
        ) ELSE (
            echo AR_HOST empty
            timeout 2 > NUL
            EXIT
        )
    ) ELSE (
        echo RPC port empty
        timeout 2 > NUL
        EXIT
    )
) ELSE (
    echo Tenant url empty
    timeout 2 > NUL
    EXIT
)

set access_key=
set secret=
if exist bmcauthconfiguration\\profile.properties (
   For /F "tokens=1* delims==" %%A IN (bmcauthconfiguration\\profile.properties) DO (
      IF "%%A"=="KEY" set access_key=%%B
      IF "%%A"=="SECRET" set secret=%%B 
   )
) else (
   echo global profile file doesn't exist
)

set REDIRECT=1
set PAUSE=
set DEBUG_OPTIONS=/level:Debug

CALL :TRIM access_key
CALL :TRIM secret

IF "%access_key%" NEQ "" (
    IF "%secret%" NEQ "" (
        echo Using global credential information from bmcauthconfiguration\\profile.properties
        set USERKEY=%access_key%
	    set USERSECRET=%secret%
    ) ELSE (
        echo bmcauthconfiguration\\profile.properties has Secret as null to cannot open editor
        timeout 2 > NUL
	    EXIT
    )
) ELSE (
    IF "%secret%" == "" (
	    echo "Both access key and secret are null in bmcauthconfiguration\\profile.properties"
    ) ELSE (
        echo bmcauthconfiguration\\profile.properties has Access key as null, so cannot open editor
        timeout 2 > NUL
        EXIT
    )
) 

IF "%access_key%" == "" IF "%secret%" == "" (
    echo No global credential information found, so prompting for credentials
    echo -
    
    set /P USERKEY=Enter your access_key:
    set /P USERSECRET=Enter your access_secret_key:
)
  
@echo off

cd /D %~dp0

if "%CONSOLE%"=="1" set RMS_JAVA=java
if not "%CONSOLE%"=="1" set RMS_JAVA=javaw
set IS64BITJAVA=0

call "%~dp0set-rms-env.bat"

REM **************************************************
REM   Platform Specific SWT       **
REM **************************************************

REM The following line is predicated on the 64-bit Sun
REM java output from -version which
REM looks like this (at the time of this writing):
REM
REM java version OpenJDK "17.0.5"
REM Java(TM) SE Runtime Environment (build 17.0.5+8)
REM Java HotSpot(TM) 64-Bit Server VM (build 17.0.5+8, mixed mode)
REM
REM Below is a logic to find the directory where java can found. We will
REM temporarily change the directory to that folder where we can run java there
pushd "%_RMS_JAVA_HOME%"
if exist java.exe goto USEJAVAFROMRMSJAVAHOME
cd bin
if exist java.exe goto USEJAVAFROMRMSJAVAHOME
popd
pushd "%_RMS_JAVA_HOME%\jre\bin"
if exist java.exe goto USEJAVAFROMPATH
goto USEJAVAFROMPATH
:USEJAVAFROMRMSJAVAHOME
FOR /F %%a IN ('.\java.exe -version 2^>^&1^|%windir%\system32\find /C "64-Bit"') DO (SET /a IS64BITJAVA=%%a)
FOR /F %%a IN ('.\java.exe -version 2^>^&1^|%windir%\system32\find /C "version ""17"') DO (SET /a ISJAVAOPENJDK=%%a)
GOTO CHECKJAVAVERSIONCOMPATIBILITY

:USEJAVAFROMPATH
FOR /F %%a IN ('java -version 2^>^&1^|find /C "64-Bit"') DO (SET /a IS64BITJAVA=%%a)
FOR /F %%a IN ('java -version 2^>^&1^|%windir%\system32\find /C "version ""17"') DO (SET /a ISJAVAOPENJDK=%%a)
GOTO CHECKJAVAVERSIONCOMPATIBILITY

:CHECKJAVAVERSIONCOMPATIBILITY
if "%ISJAVAOPENJDK%" =="0" (
echo This application is only compatible with open java 17.0 version.
echo your's java version is  :: %_RMS_JAVA_HOME%
echo exiting .....
GOTO :EOF
)
:CHECK32VS64BITJAVA

IF %IS64BITJAVA% == 1 GOTO :USE64

:USE32
REM ===========================================
REM Using 32bit Java, so include 32bit SWT Jar
REM ===========================================
set LIBSPATH=libswt\win32
GOTO :CONTINUE
:USE64
REM ===========================================
REM Using 64bit java, so include 64bit SWT Jar
REM ===========================================
set LIBSPATH=libswt\win64
set SWTJAR=..\libswt\win64
:CONTINUE
popd

REM **********************
REM   Collect arguments
REM **********************

set _cmdline=
:TopArg
if %1!==! goto EndArg
set _cmdline=%_cmdline% %1
shift
goto TopArg
:EndArg

REM ******************************************************************
REM ** Set java runtime options                                     **
REM ** Change 2048m to higher values in case you run out of memory  **
REM ** or set the RMS_JAVA_OPTIONS environment variable         **
REM ******************************************************************

if "%RMS_JAVA_OPTIONS%"=="" set RMS_JAVA_OPTIONS="-Xms1024m" "-Xmx4096m"
set OPT=%RMS_JAVA_OPTIONS% "-Djava.library.path=%LIBSPATH%"

IF %ISJAVAOPENJDK% == 1 GOTO :SKIPLOCALE
set OPT=%OPT% "-Djava.locale.providers=COMPAT,SPI"

:SKIPLOCALE
rem **** USE THIS LINE IF REMOTE DEBUGGING (port 5105) IS REQUIRED***
REM set OPT=%OPT% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5105

REM ***************
REM ** Run...    **
REM ***************
set OPT=%OPT% "--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"

if not "CONSOLE%"=="1" set START_OPTION=start "Reporting Metadata Studio"

set COMMAND="%_RMS_JAVA%" %OPT% -jar launcher\pentaho-application-launcher.jar -lib ..\%LIBSPATH% %_cmdline%

echo Launch BMC reporting metadata studio with the command: %COMMAND% -key=hidden-key -secret=hidden-secret>>"%~dp0reportingmetadatastudio.log"
@echo off
%COMMAND% -key=%USERKEY% -secret=%USERSECRET%>>"%~dp0reportingmetadatastudio.log"

if "%PAUSE%"=="1" pause

:TRIM
SetLocal EnableDelayedExpansion
 Call :TRIMSUB %%%1%%
  EndLocal & set %1=%tempvar%
  GOTO :EOF
  
 :TRIMSUB
  set tempvar=%*
  GOTO :EOF  