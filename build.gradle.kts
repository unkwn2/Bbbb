#!/system/bin/sh
# Launch proxy script for Open BYD

echo "Searching for existing OpenBYD proxy..."
pid=$(ps -ef | grep -w openbyd_proxy | grep -v grep | awk '{print $2}')
if [ -n "$pid" ]; then
    echo "Killing existing proxy (PID: $pid)"
    kill $pid
fi

# The path to the APK containing the proxy classes
# Assuming the app package is com.sr.openbyd
APK_PATH=$(pm path com.sr.openbyd | cut -d':' -f2)

if [ -z "$APK_PATH" ]; then
    echo "Error: Could not find APK path for com.sr.openbyd. Is the app installed?"
    exit 1
fi

echo "Starting new proxy process using APK: $APK_PATH"

# We inject the services.jar and dilink-services.jar frameworks to get access to BYD APIs
nohup app_process -Djava.class.path=/system/framework/services.jar:/system/framework/dilink-services.jar:$APK_PATH \
    -Djava.library.path=/system/lib64:/product/lib64:$APK_PATH!/lib/arm64-v8a \
    /system/bin --nice-name=openbyd_proxy \
    com.sr.openbyd.proxy.EntryPoint \
    --uid=2000 > /dev/null 2>&1 &

echo "Waiting for proxy to start..."
while [ $(ps -ef | grep -w openbyd_proxy | grep -v grep | wc -l) -eq 0 ]; do 
    sleep 1
done

echo "Proxy started successfully."
exit 0
