aws lambda update-function-configuration --function-name $1 --environment "Variables={JAVA_TOOL_OPTIONS=-XX:+TieredCompilation -XX:TieredStopAtLevel=1}"

