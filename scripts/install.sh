#!/bin/sh
#
# Created on April 30, 2020
#
# @author: sgoldsmith
#
# Install dependencies, Zulu OpenJDK 11 and Maven for Ubuntu/Debian.
# If JDK or Maven was already installed with this script then they will be
# replaced.
#
# Steven P. Goldsmith
# sgjava@gmail.com
# 

# Get architecture
arch=$(uname -m)

# Temp dir for downloads, etc.
tmpdir="$HOME/temp"

# stdout and stderr for commands logged
logfile="$PWD/install.log"
rm -f $logfile

# Simple logger
log(){
	timestamp=$(date +"%m-%d-%Y %k:%M:%S")
	echo "$timestamp $1"
	echo "$timestamp $1" >> $logfile 2>&1
}

log "Installing dependenices..."
# Install build/dev tools
sudo apt-get -y install build-essential autoconf automake libtool git >> $logfile 2>&1

# JDK stuff
javahome=/usr/lib/jvm/jdk11
export javahome
# ARM 32
if [ "$arch" = "armv7l" ]; then
	jdkurl="https://cdn.azul.com/zulu-embedded/bin/zulu11.39.61-ca-jdk11.0.7-linux_aarch32hf.tar.gz"
# ARM 64
elif [ "$arch" = "aarch64" ]; then
	jdkurl="https://cdn.azul.com/zulu-embedded/bin/zulu11.39.61-ca-jdk11.0.7-linux_aarch64.tar.gz"
# X86_32
elif [ "$arch" = "i586" ] || [ "$arch" = "i686" ]; then
	jdkurl="https://cdn.azul.com/zulu/bin/zulu11.39.15-ca-jdk11.0.7-linux_i686.tar.gz"
# X86_64	
elif [ "$arch" = "x86_64" ]; then
    jdkurl="https://cdn.azul.com/zulu/bin/zulu11.39.15-ca-jdk11.0.7-linux_musl_x64.tar.gz"
fi
# Just JDK archive name
jdkarchive=$(basename "$jdkurl")


log "Installing Java..."
# Remove temp dir
log "Removing temp dir $tmpdir"
rm -rf "$tmpdir" >> $logfile 2>&1
mkdir -p "$tmpdir" >> $logfile 2>&1

# Install Zulu Java JDK
log "Downloading $jdkarchive to $tmpdir"
wget -q --directory-prefix=$tmpdir "$jdkurl" >> $logfile 2>&1
log "Extracting $jdkarchive to $tmpdir"
tar -xf "$tmpdir/$jdkarchive" -C "$tmpdir" >> $logfile 2>&1
log "Removing $javahome"
sudo -E rm -rf "$javahome" >> $logfile 2>&1
# Remove .gz
filename="${jdkarchive%.*}"
# Remove .tar
filename="${filename%.*}"
sudo mkdir -p /usr/lib/jvm >> $logfile 2>&1
log "Moving $tmpdir/$filename to $javahome"
sudo -E mv "$tmpdir/$filename" "$javahome" >> $logfile 2>&1
sudo -E update-alternatives --install "/usr/bin/java" "java" "$javahome/bin/java" 1 >> $logfile 2>&1
sudo -E update-alternatives --install "/usr/bin/javac" "javac" "$javahome/bin/javac" 1 >> $logfile 2>&1
sudo -E update-alternatives --install "/usr/bin/jar" "jar" "$javahome/bin/jar" 1 >> $logfile 2>&1
sudo -E update-alternatives --install "/usr/bin/javadoc" "javadoc" "$javahome/bin/javadoc" 1 >> $logfile 2>&1
# See if JAVA_HOME exists and if not add it to /etc/environment
if grep -q "JAVA_HOME" /etc/environment; then
	log "JAVA_HOME already exists"
else
	# Add JAVA_HOME to /etc/environment
	log "Adding JAVA_HOME to /etc/environment"
	sudo -E sh -c 'echo "JAVA_HOME=$javahome" >> /etc/environment'
	. /etc/environment
	log "JAVA_HOME = $JAVA_HOME"
fi

# Apache Maven
mavenurl="http://www.gtlib.gatech.edu/pub/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz"
mavenarchive=$(basename "$mavenurl")
mavenver="apache-maven-3.6.3"
mavenhome="/opt/maven"
export mavenhome
mavenbin="/opt/maven/bin"

# Install latest Maven
log "Installing Maven $mavenver..."
log "Downloading $mavenurl$mavenarchive to $tmpdir     "
wget -q --directory-prefix=$tmpdir "$mavenurl" >> $logfile 2>&1
log "Extracting $tmpdir/$mavenarchive to $tmpdir"
tar -xf "$tmpdir/$mavenarchive" -C "$tmpdir" >> $logfile 2>&1
log "Removing $mavenhome"
sudo -E rm -rf "$mavenhome" >> $logfile 2>&1
# In case /opt doesn't exist
sudo mkdir -p /opt >> $logfile 2>&1
log "Moving $tmpdir/$mavenver to $mavenhome"
sudo -E mv "$tmpdir/$mavenver" "$mavenhome" >> $logfile 2>&1
# See if M2_HOME exists and if not add it to /etc/environment
if grep -q "M2_HOME" /etc/environment; then
	log "M2_HOME already exists"
else
	# OS will not find Maven by M2_HOME, so create link to where it's looking
	sudo -E ln -sf "$mavenbin/mvn" /usr/bin/mvn >> $logfile 2>&1
	# Add M2_HOME to /etc/environment
	log "Adding M2_HOME to /etc/environment"
	sudo -E sh -c 'echo "M2_HOME=$mavenhome" >> /etc/environment'
	. /etc/environment
	log "M2_HOME = $M2_HOME"
fi

log "PATH = $PATH"

# Clean up
log "Removing $tmpdir"
rm -rf "$tmpdir" >> $logfile 2>&1

# HawtJNI install
export JAVA_HOME=$javahome
cd >> $logfile 2>&1
log "Removing hawtjni"
rm -rf hawtjni >> $logfile 2>&1
log "Cloning HawtJNI jdk11 branch..."
git clone https://github.com/fusesource/hawtjni.git >> $logfile 2>&1
cd hawtjni >> $logfile 2>&1
log "Building HawtJNI..."
# Callback assert fails on 32 bit ARM, so skipping tests until I can fix
# hawtjni-example fails to build on armv7l
mvn clean install -Dmaven.compiler.source=11 -Dmaven.compiler.target=11 -DskipTests -pl '!hawtjni-example' --log-file="../java-periphery/scripts/hawtjni.log" >> $logfile 2>&1

# Java Periphery build
cd >> $logfile 2>&1
cd java-periphery >> $logfile 2>&1
log "Building Java Periphery..."
mvn clean install --log-file="scripts/java-periphery.log" >> $logfile 2>&1
