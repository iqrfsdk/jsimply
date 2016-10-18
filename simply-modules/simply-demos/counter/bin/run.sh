#!/bin/bash

#cdc
/usr/bin/java -Djava.library.path=natives/armhf/osgi \
	-Dlogback.configurationFile=config/logback/logback.xml \
	-cp counter-0.0.1.jar: \
	com.microrisc.simply.counter.App
#       com.microrisc.simply.counter.App >> counter.log 2>&1
