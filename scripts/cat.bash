#!/bin/bash

THIRDPARTIES=`dirname $0`/thirdparty
CAT_CP=`dirname $0`/Cat/jar/cat.jar
CAT_CP=$CAT_CP:$THIRDPARTIES/apache-log4j-2.7-bin/log4j-api-2.7.jar
CAT_CP=$CAT_CP:$THIRDPARTIES/apache-log4j-2.7-bin/log4j-core-2.7.jar
CAT_CP=$CAT_CP:$THIRDPARTIES/commons-cli-1.3.1/commons-cli-1.3.1.jar
CAT_CP=$CAT_CP:$THIRDPARTIES/jansi-1.11/jansi-1.11.jar
CAT_CP=$CAT_CP:$THIRDPARTIES/java-mail-1.4.4/java-mail-1.4.4.jar
CAT_CP=$CAT_CP:$THIRDPARTIES/jdom-2.0.6/jdom-2.0.6.jar
CAT_CP=$CAT_CP:$THIRDPARTIES/jspeedtest-1.25/jspeedtest-1.25.jar
CAT_CP=$CAT_CP:$THIRDPARTIES/proxy-vole-20131209/proxy-vole_20131209.jar

$JAVA_HOME/java -Dlog4j.skipJansi=true -Dprism.forceGPU=true -Dprism.order=sw -cp $CAT_CP cclerc.cat.Cat $*
