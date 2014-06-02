#!/bin/sh

if [ -n "$AAPT" ] || [ -n "$ANDROID_HOME" ] || type aapt >/dev/null 2>&1; then :; else
	echo ''
	echo '# WARNING: $AAPT and $ANDROID_HOME are unset and "aapt" is not on your PATH! images.py will probably fail.'
	echo '#'
	echo '# If you are building from Eclipse:'
	echo '#   Go to Preferences -> Run/Debug -> String Substitution'
	echo '#   Add the variable "android_home" pointing to your Android SDK directory'
	echo ''
fi

if [ -z "$ANT_GEN_DIR" ]; then
	export ANT_GEN_DIR="gen"
fi
if [ -z "$ANT_OUT_DIR" ]; then
	export ANT_OUT_DIR="bin"
fi

python shaders/shaders.py "$@" "$CUSTOM_GEN_DIR" uk.co.ordnancesurvey.android.maps.Shaders

python images/images.py "$@" --crunchdir "$ANT_OUT_DIR"/images "$CUSTOM_GEN_DIR" uk.co.ordnancesurvey.android.maps.Images
