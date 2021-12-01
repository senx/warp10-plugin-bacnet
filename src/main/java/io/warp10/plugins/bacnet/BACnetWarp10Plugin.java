package io.warp10.plugins.bacnet;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.mstp.Constants;
import com.serotonin.bacnet4j.npdu.mstp.MasterNode;
import com.serotonin.bacnet4j.npdu.mstp.MstpNetwork;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptLib;
import io.warp10.warp.sdk.AbstractWarp10Plugin;
import jssc.SerialPort;
import jssc.SerialPortList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.LockSupport;

public class BACnetWarp10Plugin extends AbstractWarp10Plugin implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BACnetWarp10Plugin.class);

  // variable for one bacnet network
  private LocalDevice localDevice = null;
  private Network network = null;
  private SerialPort bacnetSerialPort = null;

  private String serialPort;
  private int baudRate;
  private int dataBits;
  private int stopBits;
  private int parity;

  public void openLocalDevice(String serialPort, int baudRate, int dataBits, int stopBits, int parity) throws WarpScriptException {
    closeAll();
    if (!Arrays.asList(SerialPortList.getPortNames()).contains(serialPort)) {
      throw new WarpScriptException(serialPort + " is not a serial port");
    }
    this.bacnetSerialPort = new SerialPort(serialPort);
    boolean b;
    try {
      b = bacnetSerialPort.openPort();
      b |= bacnetSerialPort.setParams(baudRate, dataBits, stopBits, parity);
    } catch (Exception e) {
      bacnetSerialPort = null;
      throw new WarpScriptException(e.getMessage());
    }

    if (!b) {
      LOG.info("Cannot open BACnet serial port !");
      this.bacnetSerialPort = null;
      throw new WarpScriptException("Cannot open BACnet serial port " + serialPort + " (ports on the system: " + Arrays.asList(SerialPortList.getPortNames()) + ")");
    } else {
      LOG.info("Opened serial port " + serialPort + " for BACnet");
    }

    JsscSerialPortInputStream inp = new JsscSerialPortInputStream(bacnetSerialPort);
    JsscSerialPortOutputStream out = new JsscSerialPortOutputStream(bacnetSerialPort);
    //Create Local Device
    MasterNode masterNode = new MasterNode("test", inp, out,
        (byte) 0, 1);
    masterNode.setMaxInfoFrames(Constants.MAX_INFO_FRAMES);
    masterNode.setMaxMaster(Constants.MAX_MASTER);
    masterNode.setUsageTimeout(Constants.USAGE_TIMEOUT);
    network = new MstpNetwork(masterNode, 0);
    Transport transport = new DefaultTransport(network);
    transport.setTimeout(Transport.DEFAULT_TIMEOUT);
    transport.setSegTimeout(Transport.DEFAULT_SEG_TIMEOUT);
    transport.setSegWindow(Transport.DEFAULT_SEG_WINDOW);
    transport.setRetries(Transport.DEFAULT_RETRIES);

    this.localDevice = new LocalDevice(0, transport);
    this.localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.objectName, new CharacterString("Warp10Node"));
    this.localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.modelName, new CharacterString("BACnet4J"));
    try {
      this.localDevice.initialize();
    } catch (Exception e) {
      throw new WarpScriptException(e);
    }
    // quickly forgets device timeouts.
    localDevice.setTimeoutDeviceRetention(1000);

  }

  public boolean bacNetIsOpened() {
    return (this.localDevice != null && this.localDevice.isInitialized());
  }

  public RemoteDevice openRemoteDevice(int address, int timeout) throws WarpScriptException {
    RemoteDevice remoteDevice;
    if (this.localDevice == null) {
      throw new WarpScriptException("localDevice not available");
    }
    try {
      remoteDevice = localDevice.getRemoteDeviceBlocking(address, timeout);
    } catch (Exception e) {
      throw new WarpScriptException(e);
    }
    return remoteDevice;
  }

  public ObjectIdentifier buildOjectIdentifier(String objectType, int instanceNumber) throws WarpScriptException {
    if (objectType.equalsIgnoreCase("analogInput")) {
      return new ObjectIdentifier(ObjectType.analogInput, instanceNumber);
    } else if (objectType.equalsIgnoreCase("analogOutput")) {
      return new ObjectIdentifier(ObjectType.analogOutput, instanceNumber);
    } else if (objectType.equalsIgnoreCase("analogValue")) {
      return new ObjectIdentifier(ObjectType.analogValue, instanceNumber);
    } else if (objectType.equalsIgnoreCase("binaryInput")) {
      return new ObjectIdentifier(ObjectType.binaryInput, instanceNumber);
    } else if (objectType.equalsIgnoreCase("binaryOutput")) {
      return new ObjectIdentifier(ObjectType.binaryOutput, instanceNumber);
    } else if (objectType.equalsIgnoreCase("binaryValue")) {
      return new ObjectIdentifier(ObjectType.binaryValue, instanceNumber);
    } else if (objectType.equalsIgnoreCase("multiStateInput")) {
      return new ObjectIdentifier(ObjectType.multiStateInput, instanceNumber);
    } else if (objectType.equalsIgnoreCase("multiStateOutput")) {
      return new ObjectIdentifier(ObjectType.multiStateOutput, instanceNumber);
    } else if (objectType.equalsIgnoreCase("multiStateValue")) {
      return new ObjectIdentifier(ObjectType.multiStateValue, instanceNumber);
    } else {
      throw new WarpScriptException(objectType + " is not a supported object type, choose among " +
          "(binary|analog|multiState)+(Input|Output|Value) " +
          "ie : analogOutput, binaryInput, multiStateValue...");
    }
  }

  public void writeRemoteDevice(RemoteDevice remoteDevice, ObjectIdentifier id, Object value) throws WarpScriptException {

    // build a value compatible for Bacnet
    Encodable bacNetValue;
    if (value instanceof Long) {
      bacNetValue = new com.serotonin.bacnet4j.type.primitive.UnsignedInteger((Long) value);
    } else if (value instanceof Double) {
      bacNetValue = new com.serotonin.bacnet4j.type.primitive.Real(((Double) value).floatValue());
    } else if (value instanceof Boolean) {
      bacNetValue = ((Boolean) value).booleanValue() ? BinaryPV.active : BinaryPV.inactive;
    } else {
      throw new WarpScriptException(" supports only Double or Long write");
    }

    WritePropertyRequest wp = new WritePropertyRequest(id, PropertyIdentifier.presentValue, null,
        bacNetValue, new com.serotonin.bacnet4j.type.primitive.UnsignedInteger(0));

    try {
      this.localDevice.send(remoteDevice, wp).get();
    } catch (Exception e) {
      throw new WarpScriptException(e);
    }


  }

  public Map readRemoteDevice(RemoteDevice remoteDevice, Map<String, ObjectIdentifier> objMap, boolean valueOnly) throws WarpScriptException {
    if (remoteDevice == null) {
      throw new WarpScriptException("remoteDevice cannot be null");
    }
    if (this.localDevice.getCachedRemoteDevice(remoteDevice.getInstanceNumber()) == null) {
      throw new WarpScriptException("remoteDevice unknown in localDevice cache");
    }
    Map results = new LinkedHashMap<>();

    if (valueOnly) {
      // read only present value

      SequenceOf<ReadAccessSpecification> propsToRead = new SequenceOf<>();
      ArrayList<String> requestUserNameList = new ArrayList<>();
      for (Map.Entry<String, ObjectIdentifier> entry: objMap.entrySet()) {
        ObjectIdentifier id = entry.getValue();
        String requestUserName = entry.getKey();
        requestUserNameList.add(requestUserName);
        propsToRead.add(new ReadAccessSpecification(id, PropertyIdentifier.presentValue));
      }

      ReadPropertyMultipleRequest multipleRequest = new ReadPropertyMultipleRequest(propsToRead);
      try {
        ReadPropertyMultipleAck send = localDevice.send(remoteDevice, multipleRequest).get();
        SequenceOf<ReadAccessResult> readAccessResults = send.getListOfReadAccessResults();
        // we assume we receive data in the order we asked for.
        if (readAccessResults.size() != requestUserNameList.size()) {
          throw new WarpScriptException("The slave did not return the number of read requested");
        }

        for (int i = 0; i < readAccessResults.size(); i++) {
          for (ReadAccessResult.Result r: readAccessResults.get(i).getListOfResults()) {
            Map props = new LinkedHashMap();
            props.put(PropertyIdentifier.nameForId(r.getPropertyIdentifier().intValue()), r.getReadResult().toString());
            results.put(requestUserNameList.get(i), props);
          }
        }

      } catch (Exception ex) {
        throw new WarpScriptException(ex);
      }

    } else {
      // read present value and other usefull metadata to check mapping
      for (Map.Entry<String, ObjectIdentifier> entry: objMap.entrySet()) {
        String requestUserName = entry.getKey();
        ObjectIdentifier id = entry.getValue();


        List<ReadAccessSpecification> specs = new ArrayList<>();
        specs.add(new ReadAccessSpecification(id, PropertyIdentifier.presentValue));
        specs.add(new ReadAccessSpecification(id, PropertyIdentifier.units));
        specs.add(new ReadAccessSpecification(id, PropertyIdentifier.objectName));
        specs.add(new ReadAccessSpecification(id, PropertyIdentifier.description));
        specs.add(new ReadAccessSpecification(id, PropertyIdentifier.objectType));
        SequenceOf<ReadAccessSpecification> propsToRead = new SequenceOf<>(specs);
        ReadPropertyMultipleRequest multipleRequest = new ReadPropertyMultipleRequest(propsToRead);
        try {
          ReadPropertyMultipleAck send = localDevice.send(remoteDevice, multipleRequest).get();
          SequenceOf<ReadAccessResult> readAccessResults = send.getListOfReadAccessResults();
          Map props = new LinkedHashMap();
          for (ReadAccessResult result: readAccessResults) {
            SequenceOf<ReadAccessResult.Result> listResults = result.getListOfResults();
            for (int j = 0; j < listResults.size(); j++) {
              ReadAccessResult.Result r = listResults.get(j);
              props.put(PropertyIdentifier.nameForId(r.getPropertyIdentifier().intValue()), r.getReadResult().toString());
            }
          }
          results.put(requestUserName, props);
        } catch (Exception ex) {
          throw new WarpScriptException(ex);
        }

      }
    }

    return results;
  }


  private void closeAll() {
    if (localDevice != null) {
      localDevice.terminate();
      localDevice = null;
      System.out.println("BACNet localDevice terminate");
    }
    if (network != null) {
      network.terminate();
      network = null;
      System.out.println("BACNet network terminate");
    }
    if (bacnetSerialPort != null) {
      try {
        bacnetSerialPort.closePort();
        bacnetSerialPort = null;
        System.out.println("BACNet port closed");
      } catch (Exception e) {
        System.out.println("BACNet port failed to close properly");
      }
    }
  }

  @Override
  public void init(Properties properties) {
    LOG.info("BACnet plugin init");
    // Add functions
    WarpScriptLib.addNamedWarpScriptFunction(new BACnetOpenLocalDevice("BACnetOpenLocalDevice", this));
    WarpScriptLib.addNamedWarpScriptFunction(new BACnetIsOpened("BACnetIsOpened", this));
    WarpScriptLib.addNamedWarpScriptFunction(new BACnetOpenRemoteDevice("BACnetOpenRemoteDevice", this));
    WarpScriptLib.addNamedWarpScriptFunction(new BACnetBuildObjectId("BACnetBuildObjectId", this));
    WarpScriptLib.addNamedWarpScriptFunction(new BACnetReadObjects("BACnetReadObjects", this));
    WarpScriptLib.addNamedWarpScriptFunction(new BACnetWriteObjects("BACnetWriteObjects", this));

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        closeAll();
      }
    });

    Thread t = new Thread(this);
    t.setDaemon(true);
    t.setName("[Warp 10 BACNet Plugin]");
    t.start();
  }

  public BACnetWarp10Plugin() {
    super();
  }

  @Override
  public void run() {
    System.out.println("BACnet monitoring init");
    while (true) {
      LockSupport.parkNanos(10000 * 1000000L);
      if (bacnetSerialPort != null) {
        if (!Arrays.asList(SerialPortList.getPortNames()).contains(bacnetSerialPort.getPortName())) {
          System.out.println("Bacnet: serial port " + bacnetSerialPort.getPortName() + " do not exist anymore on the system ! Force closing");
          closeAll();
        }
      }
    }
  }

}
