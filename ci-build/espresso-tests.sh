#!/bin/bash
# Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
# The Okta software accompanied by this notice is provided pursuant to the Apache License, Version 2.0 (the "License.")
#
# You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the License for the specific language governing permissions and limitations under the License.

display_usage() {
  echo
  echo "Usage: $0"
  echo " -h,    --help              Instructions"
  echo " -n,    --name              Set username that does not require MFA"
  echo " -p,    --pw                Set password that does not require MFA"
  echo " -mfn,  --mfa_user          Set username that require MFA"
  echo " -mfp,  --mfa_pw            Set password that require MFA"
  echo " -cid,  --client_id         Set clientID"
  echo " -uri,  --redirect_uri      Set redirect URI"
  echo " -end,  --end_session_uri   Set end session URI"
  echo " -dsc,  --discovery_uri     Set discovery URI"
  echo " -sc    --scopes            Set the scopes"
  echo " -sch   --scheme            Set the scheme in AndroidManifest"
  echo " -pin   --pincode           Set the pin code used for device access"
  echo " -c,    --capture           Optional - Enable screen record when running tests"
  echo " -s,    --sdk               Optional - android sdk home"
  echo " -r,    --repo              Optional - directory containing the repository"
  echo " -o,    --path              Path to where repository is located"
}

while [ "$1" != "" ]; do
    PARAM=`echo $1 | awk -F= '{print $1}'`
    VALUE=`echo $1 | awk -F= '{print $2}'`
    case $PARAM in
        -h | --help)
            display_usage
            exit
            ;;
        -dsc | --discovery_uri)
            discovery_uri=$VALUE
            ;;
        -pin | --pincode)
            pincode=$VALUE
            ;;
        -sch | --scheme)
            scheme=$VALUE
            ;;
        -sc | --scopes)
            scopes=$VALUE
            ;;
        -end | --end_session_uri)
            end_session_uri=$VALUE
            ;;
        -uri | --redirect_uri)
            redirect_uri=$VALUE
            ;;
        -cid | --client_id)
            client_id=$VALUE
            ;;
        -mfp | --mfa_pw)
            mfa_pw=$VALUE
            ;;
        -mfn | --mfa_user)
            mfa_username=$VALUE
            ;;
        -n | --name)
            username=$VALUE
            ;;
        -p | --pw)
            password=$VALUE
            ;;
        -c | --capture)
            capture=true
            ;;
        -s | --sdk)
            ANDROID_HOME=$VALUE
            ;;
        -r | --repo)
            REPO=$VALUE
            ;;
        -o | --path)
            OKTA_HOME=$VALUE
            ;;
        *)
            echo "ERROR: unknown parameter \"$PARAM\""
            display_usage
            exit 1
            ;;
    esac
    shift
done

if [ -z "$discovery_uri" ] || [ -z "$mfa_username" ] || [ -z "$end_session_uri" ] || [ -z "$redirect_uri" ] || [ -z "$client_id" ] || [ -z "$mfa_pw" ] || [ -z "$scheme" ] || [ -z "$scopes" ] || [ -z "$username" ] || [ -z "$password" ] || [ -z "$pincode" ] ; then
    echo "Missing arguments: run espresso-tests.sh --help for more information"
    exit
fi

if [ -z "$OKTA_HOME" ] ; then
    OKTA_HOME=/Users/test/okta/
fi

if [ -z "$ANDROID_HOME" ] ; then
    ANDROID_HOME=/opt/android-sdk-linux
fi

if [ -z "$REPO" ] ; then
    REPO=okta-oidc-android
fi

if [ -z "$SHA" ] ; then
    SHA=sha
fi

echo "Building on `hostname`"
JOB_NAME=`basename $0`

OKTA_OIDC_ANDROID_REPO=${OKTA_HOME}/${REPO}
ESPRESSO_TEST_REPORT=${REPO}/app/build/outputs/androidTest-results/connected/
BUILD_TYPE='CI1'
LOGDIRECTORY_RELATIVE=tmp/ci-builder/${JOB_NAME}/${SHA}
LOGDIRECTORY=${OKTA_HOME}/${LOGDIRECTORY_RELATIVE}
mkdir -p ${LOGDIRECTORY}
LOGFILE=${LOGDIRECTORY}/${JOB_NAME}.log

if [ ! -z "$TEST_SUITE_TYPE_FILE" ] ; then
    export TEST_SUITE_TYPE="androidTest"
    echo ${TEST_SUITE_TYPE} > ${TEST_SUITE_TYPE_FILE}
fi

if [ ! -z "$TEST_RESULT_FILE_DIR_FILE" ] ; then
    export TEST_RESULT_FILE_DIR=${ESPRESSO_TEST_REPORT}
    echo ${TEST_RESULT_FILE_DIR} > ${TEST_RESULT_FILE_DIR_FILE}
fi

exec > >(tee $LOGFILE)
exec 2>&1
set -e

echo "================================================"
echo "Starting Espresso tests"
echo "================================================"

echo "================================================"
echo "Running with BUILD_TYPE " ${BUILD_TYPE}
echo "================================================"

echo "================================================"
echo "Removing existing sample app"
echo "================================================"
pushd ${OKTA_HOME}/${REPO}/ci-build > /dev/null
. delete-okta-app.sh

pushd ${OKTA_HOME}/${REPO} > /dev/null

function create_localproperties() {
    echo "================================================"
    echo "Create local.properties file"
    echo "================================================"
    localprop="local.properties"
    if [[ -f ${localprop} ]] ; then
        rm ${localprop}
    fi

    #Read scopes
    IFS=, read -a arr <<<"${scopes}"
    printf -v _scopes ',"%s"' "${arr[@]}"
    _scopes="${_scopes:1}"
    echo "SCOPES = $_scopes"

    echo "test.scopes=$_scopes" >> ${localprop}
    echo "test.username=\"$username\"" >> ${localprop}
    echo "test.password=\"$password\"" >> ${localprop}
    echo "test.pincode=\"$pincode\"" >> ${localprop}
    echo "test.scheme=$scheme" >> ${localprop}
    echo "test.endSessionUri=\"$end_session_uri\"" >> ${localprop}
    echo "test.discoveryUri=\"$discovery_uri\"" >> ${localprop}
    echo "test.redirectUri=\"$redirect_uri\"" >> ${localprop}
    echo "test.clientId=\"$client_id\"" >> ${localprop}
    echo "mfa.username=\"$mfa_username\"" >> ${localprop}
    echo "mfa.password=\"$mfa_pw\"" >> ${localprop}
    echo "ndk.dir=$ANDROID_HOME/ndk-bundle" >> ${localprop}
    echo "sdk.dir=$ANDROID_HOME" >> ${localprop}
}

if ! create_localproperties; then
    echo "================================================"
    echo "Failed to create local.properties file"
    echo "================================================"
    exit 1
fi


function check_device_connection() {
    echo "================================"
    echo "check for adb-connected device"
    echo "================================"

    # Verify adb connection 
    ALL_DEVICES=$($ANDROID_HOME/platform-tools/adb devices)
    echo $ALL_DEVICES
    DEVICE_CONNECTED=$(echo $ALL_DEVICES | tail -2 | head -1 | cut -f 1 | sed 's/ *$//g')
    NOT_PRESENT="List of devices attached"
    echo DEVICE_CONNECTED: $DEVICE_CONNECTED
    if [[ ${DEVICE_CONNECTED} == ${NOT_PRESENT} ]]; then
      return 1
    fi
}
if ! check_device_connection; then
    echo "================================================="
    echo "Failed to detect at least one connected device!!!"
    echo "================================================="
    exit 1
fi

function gradlew_clean() {
    echo "============="
    echo "gradlew_clean"
    echo "============="
    ./gradlew clean
}
if ! gradlew_clean; then
    echo "================================================"
    echo "Failed to clean project"
    echo "================================================"
    exit 1
fi

function gradlew_clear_data() {
    echo "============="
    echo "gradlew_clearData"
    echo "============="
    ./gradlew clearData
}
if ! gradlew_clear_data; then
    echo "================================================"
    echo "Failed to clear app data"
    echo "================================================"
    exit 1
fi

function gradlew_prepare_device() {
    echo "============="
    echo "gradlew_prepareDeviceForUITesting"
    echo "============="

    ./gradlew prepareDeviceForUITesting
}
if ! gradlew_prepare_device; then
    echo "================================================"
    echo "Failed to prepare device"
    echo "================================================"
    exit 1
fi

function screen_record() {
    ${ANDROID_HOME}/platform-tools/adb shell screenrecord "/sdcard/video.mp4" &
}

function start_logcat() {
    ${ANDROID_HOME}/platform-tools/adb logcat > "${LOGDIRECTORY}/logcat.txt" &
    PID_LOGCAT=$!
}

function gradlew_espresso() {
    echo "====================="
    echo "gradlew_espresso"
    echo "====================="
    if [[ ! -z "$capture" ]] ; then
        echo "====================="
        echo "screen_record enabled"
        echo "====================="
        screen_record
    fi

    start_logcat
    ./gradlew cAT
}

function copy_result() {
    find . -type f -name "* *.xml" -exec bash -c 'mv "$0" "${0// /_}"' {} \;
    PID_RECORD=$(${ANDROID_HOME}/platform-tools/adb shell pgrep -x screenrecord)
    if [ ! -z "$PID_RECORD" ]; then
        ${ANDROID_HOME}/platform-tools/adb shell kill -INT ${PID_RECORD}
        echo "====================="
        echo "stop screen recording"
        echo "====================="
        #Add delay or video file will become corrupt
        sleep 3
    fi

    EXISTS=$(${ANDROID_HOME}/platform-tools/adb shell "[ -f /sdcard/video.mp4 ] || echo 1")
    if [ -z "$EXISTS" ]; then
        echo "====================="
        echo "copy screen recording"
        echo "====================="
        ${ANDROID_HOME}/platform-tools/adb pull /sdcard/video.mp4 ${OKTA_HOME}/${ESPRESSO_TEST_REPORT}
        ${ANDROID_HOME}/platform-tools/adb shell rm /sdcard/video.mp4
    fi
    #stop logcat
    kill -INT ${PID_LOGCAT}
    sleep 2

    cp ${OKTA_HOME}/${ESPRESSO_TEST_REPORT}/* ~/dart/
}

if ! gradlew_espresso; then
    echo "================================================"
    echo "Failed running espresso tests"
    echo "================================================"
    if ! copy_result; then
        exit 1
    else
        exit ${PUBLISH_TYPE_AND_RESULT_DIR}
    fi
fi

function gradlew_restore_device_settings() {
    echo "============="
    echo "gradlew_restoreDeviceSettings"
    echo "============="
    ./gradlew restoreDeviceSettings
}
if ! gradlew_restore_device_settings; then
    echo "================================================"
    echo "Failed to restore device settings"
    echo "================================================"
    exit 1
fi

echo "=============================================================="
echo "Finishing up test suite " ${TEST_SUITE_ID}
echo "=============================================================="

echo "================================================"
echo "Finished Espresso tests"
echo "================================================"
popd > /dev/null

ls -l ${LOGDIRECTORY}
copy_result
exit ${PUBLISH_TYPE_AND_RESULT_DIR}