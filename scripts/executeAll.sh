#/bin/bash
source config.sh

echo "Starting cluster"

while read server; do
	echo ssh -n ${USER_NAME}@${server} "sh -c '${1}'" 
	ssh -n ${USER_NAME}@${server} "sh -c '${1}'" 
done < ${SERVER_LIST}

