#! /bin/bash
#=======================================================================
# DbxLoadGen - load generator for test servers
# Example: loadgen.sh run -t sqlserver -S myhost --duration 15m-45m --intensity low
#-----------------------------------------------------------------------
scriptPath="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

${scriptPath}/dbxtune.sh loadgen "$@"
