![Title](images/title.png)

Java Periphery is a high performance library for GPIO, LED, PWM, SPI, I2C, MMIO
and Serial peripheral I/O interface access in userspace Linux. Rather than try to
build this from scratch I used [c-periphery](https://github.com/vsergeev/c-periphery)
and [HawtJNI](https://github.com/fusesource/hawtjni) to generate the JNI wrappers.
This saves a lot of hand coding and allows for easier synchronization with
c-periphery changes moving forward.
* Generates JNI source code for c-periphery.
* Generates an autoconf and msbuild source project to build the native library.
This gets attached to the Maven project as as the native source zip file.
* Builds the native source tar for the current platform.
* Built native library is stored in a platform specific jar. This gets attached
to the Maven project as a platform specific jar file.
* All wrapper classes support AutoCloseable, so you can use the
[try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)
statement to automatically close and free native resources. This prevents hard
to track down native memory leaks.
```
try (final var spi = new Spi("/dev/spidev1.0", 0, 500000)) {
    final var txBuf = new byte[128];
    // Change some data at beginning and end.
    txBuf[0] = (byte) 0xff;
    txBuf[127] = (byte) 0x80;
    final var rxBuf = new byte[128];
    Spi.spiTransfer(spi.getHandle(), txBuf, rxBuf, txBuf.length);
    logger.info(String.format("%02X, %02X", (short) rxBuf[0] & 0xff, (short) rxBuf[127] & 0xff));
}
```

![Title](images/duo.png)

Behold the FrankenDuo which is used to test all Java Periphery features.

Java Periphery will be targeting Armbian, but the code should work with most
Linux distributions. Demo apps are included that illustrate how to leverage the
bindings. The idea is to have consistent APIs across
[C](https://github.com/vsergeev/c-periphery),
[Python](https://github.com/vsergeev/python-periphery),
[Lua](https://github.com/vsergeev/lua-periphery) and JVM languages without having
to use board specific drivers or the deprecated sysfs interface for GPIO. The
possibility of using other JVM based languages such as Groovy, Kotlin, Scala,
etc. opens up language opportunities that do not currently exist in the
IoT space.
* Why Linux userspace? This is really the only way to get cross platform
libraries to work since most SBCs have different chip sets. The trade off is
performance compared to native C written to specific chip sets. However, since
I'm wrapping C with JNI it guarantees the fastest userspace experience for Java.
* Why Armbian? Because Armbian supports many SBCs and the idea is to be truly
SBC cross platform. See [downloads](https://www.armbian.com/download).
* Why Java 11? Because Java 11 is the current LTS version of Java. Java 8 the
previous LTS release was end of life January 2019 for Oracle (commercial) and
will be end of life December 2020 for Oracle (personal use). I'm only moving
forward with Java. You can always create a fork and make a Java 8 version of
Java Periphery.
* Why Zulu OpenJDK? Because it's easy to download without all the crap Oracle
puts you through. You can always use another JDK 11 vendor, but you will have to
do that manually.

## SBC configuration
* If you are using Armbian then use `armbian-config` or edit `/boot/armbianEnv.txt`
to configure various devices. Userspace devices are exposed through /dev or
/sys. Verify the device is showing up prior to trying demo apps.
    * `sudo apt install armbian-config`
* If you are not using Armbian then you will need to know how to configure
devices to be exposed to userspace for your Linux distribution and SBC model.
Check each log in scripts directory to be sure there were no errors after running
install.sh.
* Since linux 4.8 the GPIO sysfs interface is deprecated. Userspace should use
the character device instead.
* I have tested NanoPi Duo v1.1 for 32 bit and NanoPi Neo 2 Plus for 64 bit using
the latest Armbian release. The ability to switch seemlessly between 32 and 64
bit platforms gives you a wide range of SBC choices. I'm currently testing with
Ubuntu 20.04 LTS Focal Fossa using 5.6 kernel.

## Armbian and built in buttons
On the NanoPi Duo the built in button causes it to shutdown by default. You can
remove the r_gpio_keys section in the DTB as follows (this may work on other SBCs,
but you'll need to know the correct dtb file and section to remove) :
* `cd /boot/dtb`
* `sudo cp sun8i-h2-plus-nanopi-duo.dtb sun8i-h2-plus-nanopi-duo.dtb.old`
* `sudo dtc -@ -I dtb -O dts -o sun8i-h2-plus-nanopi-duo.dts sun8i-h2-plus-nanopi-duo.dtb`
* `sudo nano sun8i-h2-plus-nanopi-duo.dts`
    * Remove `r_gpio_keys` section
* `sudo dtc -@ -I dts -O dtb -o sun8i-h2-plus-nanopi-duo.dtb sun8i-h2-plus-nanopi-duo.dts`
* `reboot`

## Non-root access
If you want to access devices without root do the following (you can try udev
rules instead if you wish):
* `sudo usermod -a -G dialout username` (Use a non-root username)
* `sudo groupadd periphery`
* `sudo usermod -a -G periphery username` (Use a non-root username)
* `ls /dev/gpio*` (Note chip names to add below)
* `ls /dev/spidev*` (Note SPI channels below)
* `ls /dev/i2c*` (Note i2c devices below)
* `sudo nano /etc/rc.local`
<pre><code>chown -R root:periphery /dev/gpiochip0
chmod -R ug+rw /dev/gpiochip0
chown -R root:periphery /dev/gpiochip1
chmod -R ug+rw /dev/gpiochip1
chown -R root:periphery /dev/i2c-0
chmod -R ug+rw /dev/i2c-0
chown -R root:periphery /dev/spidev1.0
chmod -R ug+rw /dev/spidev1.0
chown -R root:periphery /sys/devices/platform/leds/leds
chmod -R ug+rw /sys/devices/platform/leds/leds</code></pre>
* PWM udev rules
    * You need kernel 4.16 or greater to use non-root access for PWM.
    * `sudo nano /etc/udev/rules.d/99-pwm.rules`
    <pre><code>SUBSYSTEM=="pwm*", PROGRAM="/bin/sh -c '\
  chown -R root:periphery /sys/class/pwm && chmod -R 770 /sys/class/pwm;\
  chown -R root:periphery /sys/devices/platform/soc/*.pwm/pwm/pwmchip* && chmod -R 770 /sys/devices/platform/soc/*.pwm/pwm/pwmchip*\
  '"</code></pre>

## Download project
* `sudo apt install git`
* `cd ~/`
* `git clone --depth 1 https://github.com/sgjava/java-periphery.git`

## Install script
The install script assumes a clean OS install. If you would like to install on
a OS with your own version of Java 11, etc. then you can look at what install.sh
does and do it manually. What does the script do?
* Install build dependencies for HawtJNI 
* Installs Zulu OpenJDK 11 to /usr/lib/jvm
* Installs Maven to /opt
* Build HawtJNI
* Build Java Periphery
The Java Periphery POM uses download-maven-plugin to download c-periphery source
to `src/main/native-package/src`. The files are cached in
`~/.m2/repository/.cache/download-maven-plugin`, so they are not downloaded
again unless they are updated. If you want to build the GPIO C code to use sysfs
comment out `<configureArgs>` in the `hawtjni-maven-plugin` section of the POM.

### Run script
* `cd ~/java-periphery/scripts`
* `./install.sh`
* Check various log files if you have issues running the demo code. Something
could have gone wrong during the build/bindings generation processes.

### Build java-periphery with custom CFLAGS
The gcc default include paths usually do not point to the latest kernel source.
In order to use the latest features of c-periphery you will need to use the
correct include path. After the install.sh script completes:
* `uname -a` to get kernel version
* `sudo armbian-config` Software, Headers_install
* `grep -R -i "GPIOHANDLE_REQUEST_BIAS_DISABLE" /usr/src`
* `cd ~/java-periphery`
* `mvn clean install "-Dcflags=-I/usr/src/linux-headers-5.8.16-sunxi/include/uapi -I/usr/src/linux-headers-5.8.16-sunxi/include"` replace with your paths

## High performance GPIO using MMIO
I have created a generic way to achieve fast GPIO for times when performance (bit
banging, software based PWM, low CPU latency, etc) is required. I have written a
mapper, so you can extract the configuration, data and pull registers/masks
without having to do it by hand from the datasheet. Doing this totally by hand
is tedious and error prone. The method I use is using a well know interface
(GPIO device) to make changes and detecting register deltas. You still need to
create a input file with various board specific parameters. Let's use the NanoPi
Duo (H2+) as an example:
* `sudo java -cp $HOME/java-periphery/target/java-periphery-1.0.0-SNAPSHOT.jar:$HOME/java-periphery/target/java-periphery-1.0.0-SNAPSHOT-linux32.jar com.codeferm.periphery.mmio.Gen -i duo.properties -o duo-map.properties`
* `sudo java -cp $HOME/java-periphery/target/java-periphery-1.0.0-SNAPSHOT.jar:$HOME/java-periphery/target/java-periphery-1.0.0-SNAPSHOT-linux32.jar com.codeferm.periphery.mmio.Perf -i duo-map.properties -d 0 -l 203`
NanoPi Neo Plus2 (H5) example:
* `sudo java -cp $HOME/java-periphery/target/java-periphery-1.0.0-SNAPSHOT.jar:$HOME/java-periphery/target/java-periphery-1.0.0-SNAPSHOT-linux64.jar com.codeferm.periphery.mmio.Gen -i neoplus2.properties -o neoplus2-map.properties`
* `sudo java -cp $HOME/java-periphery/target/java-periphery-1.0.0-SNAPSHOT.jar:$HOME/java-periphery/target/java-periphery-1.0.0-SNAPSHOT-linux64.jar com.codeferm.periphery.mmio.Perf -i neoplus2-map.properties -d 1 -l 203`
As you can see above the same code works on 32 bit H2+ and 64 bit H5 CPU. This
means almost all Allwinner CPUs can be easily supported with the right input
file.

## How GPIO pins are mapped
This is based on testing on a NanoPi Duo. gpiochip0 starts at 0 and gpiochip1
start at 352. Consider the following table:

|Name                           |Chip Name |dev |sysfs|
| ----------------------------- | -------- | -- | --- |
|DEBUG_TX(UART_TXD0)/GPIOA4     |gpiochip0 | 004|  004|
|DEBUG_RX(UART_RXD0)/GPIOA5/PWM0|gpiochip0 | 005|  005|
|I2C0_SCL/GPIOA11               |gpiochip0 | 011|  011|
|I2C0_SDA/GPIOA12               |gpiochip0 | 012|  012|
|UART3_TX/SPI1_CS/GPIOA13       |gpiochip0 | 013|  013|
|UART3_RX/SPI1_CLK/GPIOA14      |gpiochip0 | 014|  014|
|UART3_RTS/SPI1_MOSI/GPIOA15    |gpiochip0 | 015|  015|
|UART3_CTS/SPI1_MISO/GPIOA16    |gpiochip0 | 016|  016|
|UART1_TX/GPIOG6                |gpiochip0 | 198|  198|
|UART1_RX/GPIOG7                |gpiochip0 | 199|  199|
|GPIOG11                        |gpiochip0 | 203|  203|
|ON BOARD BUTTON                |gpiochip1 | 003|  355|
|GPIOL11/IR-RX                  |gpiochip1 | 011|  363|

So basically you just need to know the starting number for each chip and realize
GPIO character devices always starts at 0 and calculate the offset. Thus gpiochip1
starts at 352 and the on board button is at 355, so 355 - 352 = 3 for GPIO
character device.

## Run demos
* `cd ~/java-periphery/target`
* `java -cp java-periphery-1.0.0-SNAPSHOT.jar:java-periphery-1.0.0-SNAPSHOT-linux32.jar com.codeferm.periphery.demo.GpioPerf -d /dev/gpiochip0 -l 203`

Note that the native library jar has a suffix such as linux32, so depending on
your target platform it could be different. To see a list of demos 
[browse](https://github.com/sgjava/java-periphery/tree/master/src/main/java/com/codeferm/periphery/demo)
code.

## Use Java Periphery in your own Maven projects
After bulding Java Periphery simpily add the following artifact:
```
<groupId>com.codeferm</groupId>
<artifactId>java-periphery</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

## GPIO Performance using GpioPerf
Note that most performance tests focus on writes and not CPU overhead, so it's
hard to compare. Technically you will actually be doing something like bit
banging to simulate a protocol, so you need extra CPU bandwidth to do that.
Please note write frequency is based on square wave.

|SBC              |OS           |CPU Freq|Write KHz|Read KHz|Average CPU|
| --------------- | ----------- | ------ | ------- | ------ | --------- |
|Odroid XU4       |Armbian Focal|2.0 GHz | 96      |195     |14%        |
|Nano Pi Duo v1.0 |Armbian Focal|1.0 GHz |250      |318     |27%        |
|Nano Pi Neo Plus2|Armbian Focal|1.0 GHz |325      |413     |27%        |
|Odroid C2        |Armbian Focal|1.5 GHz |345      |488     |29%        |

## Zulu Mission Control
[Zulu Mission Control](https://docs.azul.com/zmc/ZMCUserGuide/Title.htm) allows
you to profile your applications.
[Download](https://docs.azul.com/zmc/ZMCUserGuide/LaunchingZMC/LaunchZMC.htm)
zmc and launch on your desktop. To profile your Java Periphery application use:
`java -XX:+FlightRecorder -Djava.rmi.server.hostname=your_ip -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=8888 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -cp java-periphery-1.0.0-SNAPSHOT.jar:java-periphery-1.0.0-SNAPSHOT-linux32.jar com.codeferm.periphery.demo.GpioPerf`

![Title](images/zmc.png)
