#!/bin/bash

#spi
#sudo /usr/bin/java -Djava.library.path=natives/armv7l \

#cdc
sudo /usr/bin/java -Djava.library.path=natives/armhf/osgi \
	-Dlogback.configurationFile=config/logback/logback.xml \
	-cp monitoring-0.1.0.jar: \
	com.microrisc.simply.demos.monitoring.App
#	com.microrisc.simply.demos.monitoring.App > monitoring.log 2>&1
