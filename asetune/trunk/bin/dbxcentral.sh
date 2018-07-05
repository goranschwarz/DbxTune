#! /bin/bash
#=======================================================================
# NOTE: This one needs more work...
#-----------------------------------------------------------------------
scriptPath="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

${scriptPath}/dbxtune.sh central $@
