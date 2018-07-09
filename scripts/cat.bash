#!/bin/bash

THIRDPARTIES=`dirname $0`/thirdparty
CAT_CP=`dirname $0`/jar/cat.jar
CAT_CP=$CAT_CP:$THIRDPARTIES/apache-log4j-2.7-bin/log4j-api-2.7.jar          # Apache library for logging
CAT_CP=$CAT_CP:$THIRDPARTIES/apache-log4j-2.7-bin/log4j-core-2.7.jar         # Apache library for logging
CAT_CP=$CAT_CP:$THIRDPARTIES/commons-cli-1.3.1/commons-cli-1.3.1.jar         # Apache library for parsing command line options
CAT_CP=$CAT_CP:$THIRDPARTIES/jansi-1.11/jansi-1.11.jar                       # GitHub (fuse) library allowing to use ANSI escape sequences to format console output
CAT_CP=$CAT_CP:$THIRDPARTIES/java-mail-1.4.4/java-mail-1.4.4.jar             # Oracle library for sending mails
CAT_CP=$CAT_CP:$THIRDPARTIES/jdom-2.0.6/jdom-2.0.6.jar                       # JDOM.org library for XML parsing
CAT_CP=$CAT_CP:$THIRDPARTIES/jspeedtest-1.31.3/jspeedtest-1.31.3.jar         # GitHub library (Bertrand Martel) for running network speed tests
CAT_CP=$CAT_CP:$THIRDPARTIES/http-endec-1.04/http-endec-1.04.jar             # GitHub library (Bertrand Martel) for encoding/decoding HTTP
CAT_CP=$CAT_CP:$THIRDPARTIES/proxy-vole-20131209/proxy-vole_20131209.jar     # GitHub (Markus Bernhardt) Library for retrieving network proxies
CAT_CP=$CAT_CP:$THIRDPARTIES/geoip2-2.12.0/lib/geoip2-2.12.0.jar             # GitHub (MaxMind) library for geographic localization
CAT_CP=$CAT_CP:$THIRDPARTIES/geoip2-2.12.0/lib/jackson-annotations-2.9.5.jar # GitHub (MaxMind) library for geographic localization
CAT_CP=$CAT_CP:$THIRDPARTIES/geoip2-2.12.0/lib/jackson-core-2.9.5.jar        # GitHub (MaxMind) library for geographic localization
CAT_CP=$CAT_CP:$THIRDPARTIES/geoip2-2.12.0/lib/jackson-databind-2.9.5.jar    # GitHub (MaxMind) library for geographic localization
CAT_CP=$CAT_CP:$THIRDPARTIES/geoip2-2.12.0/lib/maxmind-db-1.2.2.jar          # GitHub (MaxMind) library for geographic localization

$JAVA_HOME/java -Dlog4j.skipJansi=true -Dprism.forceGPU=true -Dprism.order=sw -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Xms512m -Xmx1g -cp $CAT_CP cclerc.cat.Cat $*
