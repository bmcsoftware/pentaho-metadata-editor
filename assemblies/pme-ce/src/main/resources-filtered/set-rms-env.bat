
@REM This program is free software; you can redistribute it and/or modify it under the
@REM terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
@REM Foundation.
@REM
@REM You should have received a copy of the GNU Lesser General Public License along with this
@REM program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
@REM or from the Free Software Foundation, Inc.,
@REM 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
@REM
@REM This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
@REM without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
@REM See the GNU Lesser General Public License for more details.
@REM
@REM Copyright (c) 2010 - ${copyright.year} Hitachi Vantara. All rights reserved.
@REM BMC Software, Inc. has modified this file in 2024
rem ---------------------------------------------------------------------------
rem Finds a suitable Java
rem
rem Looks in well-known locations to find a suitable Java then sets two 
rem environment variables for use in other bat files. The two environment
rem variables are:
rem 
rem * _RMS_JAVA_HOME - absolute path to Java home
rem * _RMS_JAVA - absolute path to Java launcher (e.g. java.exe)
rem 
rem The order of the search is as follows:
rem 
rem 1. argument #1 - path to Java home
rem 2. environment variable RMS_JAVA_HOME - path to Java home
rem 3. jre folder at current folder level
rem 4. java folder at current folder level
rem 5. jre folder one level up
rem 6 java folder one level up
rem 7. jre folder two levels up
rem 8. java folder two levels up
rem 9. environment variable JAVA_HOME - path to Java home
rem 10. environment variable JRE_HOME - path to Java home
rem
rem If a suitable Java is found at one of these locations, then
rem _RMS_JAVA_HOME is set to that location and _RMS_JAVA is set to the
rem absolute path of the Java launcher at that location. If none of these
rem locations are suitable, then _RMS_JAVA_HOME is set to empty string and
rem _RMS_JAVA is set to java.exe.
rem
rem Finally, there is one final optional environment variable: RMS_JAVA.
rem If set, this value is used in the construction of _RMS_JAVA. If not
rem set, then the value java.exe is used.
rem ---------------------------------------------------------------------------

if not "%RMS_JAVA%" == "" goto gotPentahoJava
set __LAUNCHER=java.exe
goto checkPentahoJavaHome

:gotPentahoJava
set __LAUNCHER=%RMS_JAVA%
goto checkPentahoJavaHome

:checkPentahoJavaHome
if exist "%~1\bin\%__LAUNCHER%" goto gotValueFromCaller
if not "%RMS_JAVA_HOME%" == "" goto gotPentahoJavaHome
if exist "%~dp0jre\bin\%__LAUNCHER%" goto gotJreCurrentFolder
if exist "%~dp0java\bin\%__LAUNCHER%" goto gotJavaCurrentFolder
if exist "%~dp0..\jre\bin\%__LAUNCHER%" goto gotJreOneFolderUp
if exist "%~dp0..\java\bin\%__LAUNCHER%" goto gotJavaOneFolderUp
if exist "%~dp0..\..\jre\bin\%__LAUNCHER%" goto gotJreTwoFolderUp
if exist "%~dp0..\..\java\bin\%__LAUNCHER%" goto gotJavaTwoFolderUp
if not "%JAVA_HOME%" == "" goto gotJdkHome
if not "%JRE_HOME%" == "" goto gotJreHome
goto gotPath

:gotPentahoJavaHome
echo DEBUG: Using RMS_JAVA_HOME
set _RMS_JAVA_HOME=%RMS_JAVA_HOME%
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotJreCurrentFolder
echo DEBUG: Found JRE at the current folder
set _RMS_JAVA_HOME=%~dp0jre
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotJavaCurrentFolder
echo DEBUG: Found JAVA at the current folder
set _RMS_JAVA_HOME=%~dp0java
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotJreOneFolderUp
echo DEBUG: Found JRE one folder up
set _RMS_JAVA_HOME=%~dp0..\jre
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotJavaOneFolderUp
echo DEBUG: Found JAVA one folder up
set _RMS_JAVA_HOME=%~dp0..\java
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotJreTwoFolderUp
echo DEBUG: Found JRE two folder up
set _RMS_JAVA_HOME=%~dp0..\..\jre
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotJavaTwoFolderUp
echo DEBUG: Found JAVA two folder up
set _RMS_JAVA_HOME=%~dp0..\..\java
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotJdkHome
echo DEBUG: Using JAVA_HOME
set _RMS_JAVA_HOME=%JAVA_HOME%
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotJreHome
echo DEBUG: Using JRE_HOME
set _RMS_JAVA_HOME=%JRE_HOME%
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotValueFromCaller
echo DEBUG: Using value (%~1) from calling script
set _RMS_JAVA_HOME=%~1
set _RMS_JAVA=%_RMS_JAVA_HOME%\bin\%__LAUNCHER%
goto end

:gotPath
echo WARNING: Using java from path
set _RMS_JAVA_HOME=
set _RMS_JAVA=%__LAUNCHER%

goto end

:end

echo DEBUG: _RMS_JAVA_HOME=%_RMS_JAVA_HOME%
echo DEBUG: _RMS_JAVA=%_RMS_JAVA%
