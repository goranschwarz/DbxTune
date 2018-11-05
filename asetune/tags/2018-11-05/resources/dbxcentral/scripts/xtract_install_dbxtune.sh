#! /bin/bash -x

## set what release we want to download and install
releaseDir="asetune_${1:-$(date '+%Y-%m-%d')}"

## Remove current release DIR
rm -f 0
rm -f -r ${releaseDir}

## Create the new release dir and create a softlink
mkdir ${releaseDir}
ln -s ${releaseDir} 0

## Remove current release ZIP and get a new one
rm -f ${releaseDir}.zip
wget http://gorans.org/www/asetune/tmp/${releaseDir}.zip

## goto the release dir and unpack
cd ${releaseDir}
unzip ../${releaseDir}.zip
#unzip dbxtune_beta_tools.zip
#chmod 755 *.sh
chmod 755 bin/*.sh
cd ..

## Check install
ls -Fal ${releaseDir}
ls -Fal ${releaseDir}/bin


