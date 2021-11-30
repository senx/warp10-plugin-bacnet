package io.warp10.plugins.bacnet;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;
import jssc.SerialPort;

import java.util.LinkedHashMap;

public class BACnetOpenLocalDevice extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  BACnetWarp10Plugin bacnet;

  public BACnetOpenLocalDevice(String name, BACnetWarp10Plugin bacnet) {
    super(name);
    this.bacnet = bacnet;
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object o = stack.pop();
    if (!(o instanceof LinkedHashMap)) {
      throw new WarpScriptException("BACnetOpenLocalDevice expects a MAP on top of the stack");
    }
    LinkedHashMap params = (LinkedHashMap) o;

    if (!params.containsKey("port") ||
        !params.containsKey("baudrate") ||
        !params.containsKey("databits") ||
        !params.containsKey("stopbits") ||
        !params.containsKey("parity")) {
      throw new WarpScriptException("BACnetOpenLocalDevice expects a MAP with port, baudrate, databits, stopbits, parity");
    }

    String serialPort = (String) params.get("port");
    int baudrate = ((Long) params.get("baudrate")).intValue();
    int databits = ((Long) params.get("databits")).intValue();
    int stopbits = ((Long) params.get("stopbits")).intValue();
    String paritystr = (String) params.get("parity");

    int parity = SerialPort.PARITY_NONE; // none
    if (paritystr.equalsIgnoreCase("even")) {
      parity = SerialPort.PARITY_EVEN;
    }
    if (paritystr.equalsIgnoreCase("odd")) {
      parity = SerialPort.PARITY_ODD;
    }

    try {
      this.bacnet.openLocalDevice(serialPort, baudrate, databits, stopbits, parity);
    } catch (Exception e) {
      throw new WarpScriptException(e);
    }
    return stack;
  }
}
