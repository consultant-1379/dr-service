######################################################################
# Script to get access token for artifactory  #
######################################################################
# The token generated by this script needs to be regenerated after 12 month before the expire date #
# Token expire date: 22/04/2025 #
# If token has expired, user needs to manually update the token by the following steps
# 1. Login to https://arm.seli.gic.ericsson.se as esoadm100,
# 2. Go to user profile,
# 3. Generate the identity token,
# 4. Replace the content of the <password> in .pypirc file with the newly generated token and execute the script.
# Note: User needs to install yq in order to execute the command.
#   For windows user:
#     choco install yq
#   For linux user:
#     sudo wget -qO /usr/local/bin/yq https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64
#     sudo chmod a+x /usr/local/bin/yq

#!/bin/bash

set -xe

function getToken() {
  token=$(curl --fail -H "Authorization:Bearer $1" -XPOST "https://$2/access/api/v1/tokens" | yq -r .access_token);
  echo ${token}
}

PYPIRC_FILE=".pypirc"
current_token=$(grep -oP 'password:\s*\K.*' "$PYPIRC_FILE")

echo "Updating Access Token for SELI...";
url_path="arm.seli.gic.ericsson.se";
new_token=$(getToken $current_token $url_path);
echo $new_token

sed -i "s/^password:.*/password: $new_token/" "$PYPIRC_FILE"
echo "Token replaced successfully in .pypirc file."