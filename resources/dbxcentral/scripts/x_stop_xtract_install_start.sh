#! /bin/bash -x


## Only list and exit
if [ "$1" == "list" ]
then
	./xtract_install_dbxtune.sh list
	exit
fi


## STOP
./0/bin/dbxc_stop_ALL.sh
sleep 3


## Xtract and install
./xtract_install_dbxtune.sh $@


## START
sleep 3
./0/bin/dbxc_start_ALL.sh

