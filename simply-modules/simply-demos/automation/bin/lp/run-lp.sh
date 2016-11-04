#!/bin/bash

/usr/bin/sudo /usr/bin/java -Djava.library.path=natives/armhf/osgi \
	-Dlogback.configurationFile=config/logback/logback.xml \
	-cp automation-0.0.1.jar: \
	com.microrisc.simply.demos.automation.AppLp >> automation-lp.log 2>&1
