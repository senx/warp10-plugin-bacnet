# Bacnet plugin for Warp 10 TSDB

This plugin allow to open one MS/TP port from within [WarpScript](https://www.warp10.io/), and poll 
values from several devices on the bus.

Behind the hood, it uses BACnet4J (GPL licence), so this plugin is also released in GPL.

There is very very very few documentation for BACnet4J, so the code is 
based on these examples: [1](https://github.com/MangoAutomation/BACnet4J/blob/master/src/test/java/com/serotonin/bacnet4j/adhoc/rs485/MasterTest.java) 
[2](https://gist.github.com/splatch/3216feba4bcad3cfd741644552f93870).

## Capabilities and limitations
The plugin is designed to handle only one MS/TP BACNet port for the moment.

Once opened, the port is monitored by the plugin. If the serial port is disconnected, it will be closed within 10 seconds.

If you call `BACnetOpenLocalDevice` several times without checking the 
port is opened (with `BACnetIsOpened`), you will close and re-initialize the port.

Feel free to contribute to add multiple networks or BACnet over IP!

## Code example

```warpscript
// @endpoint http://localhost:8080/api/v0/exec

BACnetIsOpened ! // if not already opened...
<% 
  {
    'port' '/dev/ttyUSB1'
    'baudrate' 19200
    'databits' 8
    'stopbits' 1
    'parity' 'none'
  } BACnetOpenLocalDevice   // do not returns anything. Only one MS/TP network is supported. 
                            // Feel free to contribute for multiple ports or IP :)
%> IFT

// note : if the port is unplugged, it will be detected and safely closed within 10 seconds
// by the plugin.

// open device #620001  with a 15000 millisecond timeout
620001 15000 BACnetOpenRemoteDevice  'remoteDevice' STORE

true 'showAllBacnetProperties' STORE

$remoteDevice
{
  "CurrentTemperatureZ1"   "analogValue" 101 BACnetBuildObjectId
  "CurrentSetPointZ1"      "analogValue" 102 BACnetBuildObjectId
  "CurrentTemperatureZ2"   "analogValue" 201 BACnetBuildObjectId
  "CurrentSetPointZ2"      "analogValue" 202 BACnetBuildObjectId
} $showAllBacnetProperties  BACnetReadObjects

```

## Compile and deploy

```bash
./gradlew shadowJar
mv build/libs/bacnetplugin.jar /opt/warp10/lib/ 
sudo systemctl restart warp10.service
```

