#!/bin/bash

PATH_TO_BASEDIR=$(pwd)

BOOTCLASSPATH_ENTRY="$ANDROID_HOME/platforms/android-28/android.jar"

DESUGAR="${PATH_TO_BASEDIR}/desugar.sh"
DESUGAR_TOOL_PATH=$(pwd)
LIB_PATH="${PATH_TO_BASEDIR}/authnlibs"
OUT_DIR="${PATH_TO_BASEDIR}/libs"

OKTA_AUTHN_SDK_API="okta-authn-sdk-api-1.0.0"
OKTA_AUTHN_SDK_IMPL="okta-authn-sdk-impl-1.0.0"
OKTA_SDK_API="okta-sdk-api-1.5.2"
OKTA_SDK_IMPL="okta-sdk-impl-1.5.2"
OKTA_HTTP_API="okta-http-api-1.2.0"
OKTA_SDK_OKHTTP="okta-sdk-okhttp-1.5.2"
OKTA_CONFIG_CHECK="okta-config-check-1.1.1"
OKTA_COMMON_LANG="okta-commons-lang-1.1.1"
JACKSON_DATABIND="jackson-databind-2.9.8"
OKHTTP="okhttp-3.11.0"
JAVA8_LIBS="java8_libs"

mkdir -p "${OUT_DIR}"
cp "${LIB_PATH}/${JAVA8_LIBS}.jar" ${OUT_DIR}

echo "desugar okta-config-check"
"${DESUGAR}" \
	"${DESUGAR_TOOL_PATH}" \
        "--input ${LIB_PATH}/${OKTA_CONFIG_CHECK}.jar" \
        "--output ${OUT_DIR}/${OKTA_CONFIG_CHECK}-android.jar" \
        "--bootclasspath_entry ${BOOTCLASSPATH_ENTRY}"

echo "desugar okta-commons-lang"
"${DESUGAR}" \
	"${DESUGAR_TOOL_PATH}" \
        "--input ${LIB_PATH}/${OKTA_COMMON_LANG}.jar" \
        "--output ${OUT_DIR}/${OKTA_COMMON_LANG}-android.jar" \
        "--bootclasspath_entry ${BOOTCLASSPATH_ENTRY}"

echo "desugar okta-http-api"
"${DESUGAR}" \
	"${DESUGAR_TOOL_PATH}" \
        "--input ${LIB_PATH}/${OKTA_HTTP_API}.jar" \
        "--output ${OUT_DIR}/${OKTA_HTTP_API}-android.jar" \
        "--bootclasspath_entry ${BOOTCLASSPATH_ENTRY}"

echo "desugar okta-sdk-api"
"${DESUGAR}" \
	"${DESUGAR_TOOL_PATH}" \
        "--input ${LIB_PATH}/${OKTA_SDK_API}.jar" \
        "--output ${OUT_DIR}/${OKTA_SDK_API}-android.jar" \
        "--bootclasspath_entry ${BOOTCLASSPATH_ENTRY}" \
        "--classpath_entry ${OUT_DIR}/${OKTA_COMMON_LANG}-android.jar" 

echo "desugar okta-sdk-impl"
"${DESUGAR}" \
	"${DESUGAR_TOOL_PATH}" \
        "--input ${LIB_PATH}/${OKTA_SDK_IMPL}.jar" \
        "--output ${OUT_DIR}/${OKTA_SDK_IMPL}-android.jar" \
        "--bootclasspath_entry ${BOOTCLASSPATH_ENTRY}" \
        "--classpath_entry ${OUT_DIR}/${OKTA_SDK_API}-android.jar" \
        "--classpath_entry ${OUT_DIR}/${OKTA_HTTP_API}-android.jar" \
	"--classpath_entry ${LIB_PATH}/${JACKSON_DATABIND}.jar" \
	"--classpath_entry ${LIB_PATH}/${JAVA8_LIBS}.jar"

echo "desugar okta-sdk-okhttp"
"${DESUGAR}" \
	"${DESUGAR_TOOL_PATH}" \
        "--input ${LIB_PATH}/${OKTA_SDK_OKHTTP}.jar" \
        "--output ${OUT_DIR}/${OKTA_SDK_OKHTTP}-android.jar" \
        "--bootclasspath_entry ${BOOTCLASSPATH_ENTRY}" \
        "--classpath_entry ${OUT_DIR}/${OKTA_SDK_IMPL}-android.jar" \
        "--classpath_entry ${OUT_DIR}/${OKTA_SDK_API}-android.jar" \
        "--classpath_entry ${LIB_PATH}/${OKHTTP}.jar"

echo "desugar okta-authn-sdk-api"
"${DESUGAR}" \
	"${DESUGAR_TOOL_PATH}" \
        "--input ${LIB_PATH}/${OKTA_AUTHN_SDK_API}.jar" \
        "--output ${OUT_DIR}/${OKTA_AUTHN_SDK_API}-android.jar" \
        "--bootclasspath_entry ${BOOTCLASSPATH_ENTRY}"

echo "desugar okta-authn-sdk-impl"
"${DESUGAR}" \
	"${DESUGAR_TOOL_PATH}" \
        "--input ${LIB_PATH}/${OKTA_AUTHN_SDK_IMPL}.jar" \
        "--output ${OUT_DIR}/${OKTA_AUTHN_SDK_IMPL}-android.jar" \
	"--bootclasspath_entry ${BOOTCLASSPATH_ENTRY}" \
        "--classpath_entry ${OUT_DIR}/${OKTA_AUTHN_SDK_API}-android.jar" \
        "--classpath_entry ${OUT_DIR}/${OKTA_SDK_API}-android.jar" \
	"--classpath_entry ${OUT_DIR}/${OKTA_SDK_IMPL}-android.jar" \
	"--classpath_entry ${LIB_PATH}/${JAVA8_LIBS}.jar" \
        "--classpath_entry ${LIB_PATH}/${JACKSON_DATABIND}.jar"

