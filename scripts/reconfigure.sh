#/bin/bash
source config.sh

echo "Reconfigure cluster"

while read server; do
	echo "Copying config to ${server}"
	scp ${PROPERTY_FILE_SOURCE} ${USER_NAME}@${server}:${REMOTE_DIR}/.
	echo "Starting server at ${server}"
	ssh      -n ${USER_NAME}@${server} "sh -c 'cd ${REMOTE_DIR};nohup java ${JAVA_OPTIONS} -Djava.rmi.server.hostname=${server} -jar ${JAR_FILE} ${SERVER_OPTIONS} > /tmp/log.txt 2>&1 &'" 
done < ${SERVER_LIST}

