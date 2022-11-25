#! /bin/bash -x

./0/bin/dbxc_stop_ALL.sh
sleep 3

./xtract_install_dbxtune.sh $@
#if [ true ]
#then
#	#cp ~sybase/dbxtune/0/lib/h2-1.4.199-SNAPSHOT-2019-08-26.jar ~sybase/dbxtune/0/lib/h2-SNAPSHOT.jar
#	cp ~sybase/dbxtune/0/lib/h2-1.4.200.jar ~sybase/dbxtune/0/lib/h2-SNAPSHOT.jar
#	ls -Fal ~sybase/dbxtune/0/lib/h2-*
#fi

sleep 3
./0/bin/dbxc_start_ALL.sh
