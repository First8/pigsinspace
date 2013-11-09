#/bin/bash
source config.sh

echo "Stopping cluster"

while read server; do
	echo "Stopping ${server}"
	ssh -n ${USER_NAME}@${server} "pidof java|xargs kill "
done < ${SERVER_LIST}

