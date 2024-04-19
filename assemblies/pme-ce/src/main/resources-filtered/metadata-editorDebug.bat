@echo off

REM This program is free software; you can redistribute it and/or modify it under the
REM terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
REM Foundation.
REM
REM You should have received a copy of the GNU Lesser General Public License along with this
REM program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
REM or from the Free Software Foundation, Inc.,
REM 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
REM
REM This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
REM without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
REM See the GNU Lesser General Public License for more details.
REM
REM Copyright (c) 2013 - 2022 Hitachi Vantara. All rights reserved.

REM MetaDataDebug is to support you in finding unusual errors and start problems.
echo Opening up BMC reporting admin console with debug mode 'ON'

REM this will always output the the system console using the debug mode
set CONSOLE=1

REM This starts MetaData Editor(Admin module) with a console output with the following options:

set access_key=
set secret=
if exist env.properties (
    if exist profile.properties (
        For /F "tokens=1* delims==" %%A IN (profile.properties) DO (
            IF "%%A"=="KEY" set access_key=%%B
            IF "%%A"=="SECRET" set secret=%%B 
        )
    ) else (
        echo global profile file doesn't exist
    )
) else (
    echo env file doesn't exist
    timeout 5 > NUL
    EXIT
)

set REDIRECT=1
set PAUSE=
set DEBUG_OPTIONS=/level:Debug

CALL :TRIM access_key
CALL :TRIM secret

IF "%access_key%" NEQ "" IF "%secret%" NEQ ""   (
    echo Using global credential information from profile.properties

    set USERKEY=%access_key%
    set USERSECRET=%secret%
    
    if not "%REDIRECT%"=="1" "%~dp0metadata-editor.bat" %OPTIONS%
    if "%REDIRECT%"=="1" echo Console output gets redirected to "%~dp0metaDataEditorDebug.txt"
    if "%REDIRECT%"=="1" "%~dp0metadata-editor.bat" %OPTIONS% >>"%~dp0metaDataEditorDebug.txt"
) 

IF "%access_key%" == "" IF "%secret%" == "" (
    echo No global credential information found, so prompting for credentials
    echo -
    
    set /P USERKEY=Enter your access_key:
    set /P USERSECRET=Enter your access_secret_key:

    if not "%REDIRECT%"=="1" "%~dp0metadata-editor.bat" %OPTIONS%
    if "%REDIRECT%"=="1" echo Console output gets redirected to "%~dp0metaDataEditorDebug.txt"
    if "%REDIRECT%"=="1" "%~dp0metadata-editor.bat" %OPTIONS% >>"%~dp0metaDataEditorDebug.txt"
)


:TRIM
SetLocal EnableDelayedExpansion
 Call :TRIMSUB %%%1%%
  EndLocal & set %1=%tempvar%
  GOTO :EOF
  
 :TRIMSUB
  set tempvar=%*
  GOTO :EOF  