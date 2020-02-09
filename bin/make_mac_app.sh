#! /bin/bash
#===========================================================
# NOTE: this does NOT work yeat, use the asetune.sh script for the moment
#-----------------------------------------------------------
# Create a native Mac application called AseTune
# http://developer.apple.com/library/mac/#documentation/Java/Conceptual/Java14Development/03-JavaDeployment/JavaDeployment.html#//apple_ref/doc/uid/TP40001885-SW1
#-----------------------------------------------------------
appname=AseTune
rm -r ${appname}.app
mkdir ${appname}.app
mkdir ${appname}.app/Contents
mkdir ${appname}.app/Contents/MacOS
mkdir ${appname}.app/Contents/Resources
mkdir ${appname}.app/Contents/Resources/Java

javaLibDir=\$APP_PACKAGE/Contents/Resources/Java

#
# Compose the class path that we will use later on.
#
export CP=${CP}:$javaLibDir/asetune.jar
export CP=${CP}:$javaLibDir/jconn4.jar
export CP=${CP}:$javaLibDir/jconn3.jar
export CP=${CP}:$javaLibDir/jtds-1.3.1.jar
export CP=${CP}:$javaLibDir/dsparser.jar
export CP=${CP}:$javaLibDir/log4j-1.2.17.jar
export CP=${CP}:$javaLibDir/h2-SNAPSHOT.jar
export CP=${CP}:$javaLibDir/h2-1.4.200.jar
export CP=${CP}:$javaLibDir/wizard.jar
export CP=${CP}:$javaLibDir/miglayout-swing-5.2.jar
export CP=${CP}:$javaLibDir/miglayout-core-5.2.jar
export CP=${CP}:$javaLibDir/swingx-all-1.6.5-1.jar
export CP=${CP}:$javaLibDir/jchart2d-3.2.2.jar
export CP=${CP}:$javaLibDir/planviewer.jar
export CP=${CP}:$javaLibDir/commons-lang3-3.7.jar
export CP=${CP}:$javaLibDir/commons-io-2.6.jar
export CP=${CP}:$javaLibDir/commons-csv-1.5.jar
export CP=${CP}:$javaLibDir/commons-cli-1.4.jar
export CP=${CP}:$javaLibDir/proxy-vole_20131209.jar
export CP=${CP}:$javaLibDir/ganymed-ssh2-262.jar
export CP=${CP}:$javaLibDir/rsyntaxtextarea.jar
export CP=${CP}:$javaLibDir/autocomplete.jar
export CP=${CP}:$javaLibDir/rstaui.jar
export CP=${CP}:$javaLibDir/jcommon-1.0.21.jar
export CP=${CP}:$javaLibDir/jfreechart-1.0.17.jar
export CP=${CP}:$javaLibDir/juniversalchardet-2.3.0.jar
export CP=${CP}:$javaLibDir/DDLGen.jar

#
# Copy some stub file
#
cp /System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub ${appname}.app/Contents/MacOS/

#
# Create Info.plist file in the Contents directory 
#
#TODO
cat <<EOF> ${appname}.app/Contents/Info.plist
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>CFBundleDevelopmentRegion</key>       <string>en</string>
	<key>CFBundleName</key>                    <string>$appname</string>
	<key>CFBundleVersion</key>                 <string>1.0.0</string>
	<key>CFBundleShortVersionString</key>      <string>$appname 1.0.0</string>
	<key>CFBundleExecutable</key>              <string>JavaApplicationStub</string>
	<key>CFBundlePackageType</key>             <string>APPL</string>
	<key>CFBundleSignature</key>               <string>????</string>
	<key>CFBundleGetInfoString</key>           <string>$appname 1.0.0</string>
	<key>CFBundleIconFile</key>                <string>asetune_icon_32.png</string>
	<key>CFBundleAllowMixedLocalizations</key> <true/>
	<key>CFBundleInfoDictionaryVersion</key>   <string>6.0</string>
	<key>CFBundleIdentifier</key>              <string>com.AseTune</string>
	<key>LSHasLocalizedDisplayName</key>       <true/>
	<key>CFBundleDisplayName</key>             <string>$appname</string>
	<key>Java</key>
	<dict>
		<key>MainClass</key>               <string>com.asetune.AseTune</string>

		<key>ClassPath</key>               <string>$CP</string>

		<key>Properties</key>
		<dict>
			<key>apple.laf.useScreenMenuBar</key> <string>true</string>
			<key>sybase.home</key>                <string>\$SYBASE</string>
			<key>SYBASE</key>                     <string>\$SYBASE</string>
			<key>ASETUNE_HOME</key>               <string>\$APP_PACKAGE</string>
			<key>ASETUNE_SAVE_DIR</key>           <string>~/.asetune/</string>
		</dict>

		<key>VMOptions</key>
		<array>
			<string>-Xms64m</string>
			<string>-Xmx1024m</string>
			<string>-Xdock:name=$appname</string>
			<string>-Xdock:icon=$javaLibDir/asetune_icon_32.gif</string>
		</array>
		
		<key>SplashFile</key> <string>$javaLibDir/asetune_splash.jpg</string>

		<key>JVMVersion</key> <string>1.6+</string>

		<key>JVMArchs</key>
		<array>
			<string>x86_64</string>
			<string>i386</string>
			<string>ppc</string>
		</array>
	</dict>
</dict>
</plist>
EOF

#<key>Java</key>
#    <dict>
#        <key>JVMVersion</key>
#        <string>1.6+</string>
#
#        <key>MainClass</key>
#        <string>com.asetune.AseTune</string>
#
#        <key>Properties</key>
#        <dict>
#			<key>sun.java2d.noddraw</key>         <string>true</string>
#			<key>sun.java2d.d3d</key>             <string>false</string>
#			<key>sun.java2d.opengl</key>          <string>false</string>
#			<key>sun.java2d.pmoffscreen</key>     <string>false</string>
#            <key>apple.laf.useScreenMenuBar</key> <string>true</string>
#            <key>sybase.home</key>                <string>$SYBASE</string>
#            <key>SYBASE</key>                     <string>$SYBASE</string>
#            <key>ASETUNE_HOME</key>               <string>$APP_PACKAGE</string>
#            <key>ASETUNE_SAVE_DIR</key>           <string>~/.asetune/</string>
#        </dict>
#
#        <key>VMOptions</key>
#        <array>
#            <string>-Xms64m</string>
#            <string>-Xmx1024m</string>
#            <string>-Xdock:name=$appname</string>
#            <string>-Xdock:icon=\$APP_PACKAGE/lib/asetune_icon_32.gif</string>
#            <string>-splash:lib/asetune_splash.jpg</string>
#        </array>
#
#    </dict>

#
# Create the PkgInfo file
#
cat <<EOF> ${appname}.app/Contents/PkgInfo
APPL????
EOF

#
# Copy jar files into Contents/Resources/Java/
#
cp lib/* ${appname}.app/Contents/Resources/Java/
cp lib/asetune_icon_32.png ${appname}.app/Contents/Resources/
cp dbxtune.properties ${appname}.app/

#
# Set the bundle bit Finder attribute
#
SETFILE=/Developer/Tools/SetFile
SETFILE=/Applications/Xcode.app/Contents/Developer/Tools/SetFile
$SETFILE -a B ${appname}.app
