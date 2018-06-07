@echo off

REM %~dp0 is the current file directory -> script should always work with current distribution
set THIRDPARTIES=%~dp0\thirdparty
set CAT_CP=%~dp0\jar\cat.jar
REM Apache library for logging
set CAT_CP=%CAT_CP%;%THIRDPARTIES%/apache-log4j-2.7-bin/log4j-api-2.7.jar
REM Apache library for logging
set CAT_CP=%CAT_CP%;%THIRDPARTIES%/apache-log4j-2.7-bin/log4j-core-2.7.jar
REM Apache library for parsing command line options
set CAT_CP=%CAT_CP%;%THIRDPARTIES%/commons-cli-1.3.1/commons-cli-1.3.1.jar
REM GitHub (fuse) library allowing to use ANSI escape sequences to format console output
set CAT_CP=%CAT_CP%;%THIRDPARTIES%/jansi-1.11/jansi-1.11.jar
REM Oracle library for sending mails
set CAT_CP=%CAT_CP%;%THIRDPARTIES%/java-mail-1.4.4/java-mail-1.4.4.jar
REM JDOM.org library for XML parsing
set CAT_CP=%CAT_CP%;%THIRDPARTIES%/jdom-2.0.6/jdom-2.0.6.jar
REM GitHub library (Bertrand Martel) for running network speed tests
set CAT_CP=%CAT_CP%;%THIRDPARTIES%/jspeedtest-1.31.3/jspeedtest-1.31.3.jar
REM GitHub library (Bertrand Martel) for encoding/decoding HTTP
set CAT_CP=%CAT_CP%;%THIRDPARTIES%/http-endec-1.04/http-endec-1.04.jar
REM GitHub (Markus Bernhardt) Library for retrieving network proxies
set CAT_CP=%CAT_CP%;%THIRDPARTIES%/proxy-vole-20131209/proxy-vole_20131209.jar

REM To display special characters
chcp 1252 > NUL
java -cp "%CAT_CP%" -Dlog4j.skipJansi=true cclerc.cat.Cat %*


