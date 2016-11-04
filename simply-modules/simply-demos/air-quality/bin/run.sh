#!/bin/bash

#cdc
/usr/bin/java -Djava.library.path=natives/armhf/osgi \
	-Dlogback.configurationFile=config/logback/logback.xml \
	-cp air-quality-0.0.1.jar: \
	com.microrisc.simply.demos.airquality.App
#      com.microrisc.simply.demos.airquality.App >> airquality.log 2>&1
