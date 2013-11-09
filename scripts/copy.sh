#/bin/bash
source config.sh

echo "Copying jar to cluster"

while read server; do
	echo "Copying jar to ${server}"
	scp ${JAR_FILE_SOURCE} ${USER_NAME}@${server}:${REMOTE_DIR}/.
done < ${SERVER_LIST}

