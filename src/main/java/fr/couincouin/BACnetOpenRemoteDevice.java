package fr.couincouin;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class BACnetOpenRemoteDevice extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  BACnetWarp10Plugin bacnet;

  public BACnetOpenRemoteDevice(String name, BACnetWarp10Plugin bacnet) {
    super(name);
    this.bacnet = bacnet;
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    int address, timeout;
    Object o = stack.pop();
    if (o instanceof Long) {
      timeout = ((Long) o).intValue();
    } else {
      throw new WarpScriptException(this.getName() + "expects a long for address, and a long for timeout");
    }

    o = stack.pop();
    if (o instanceof Long) {
      address = ((Long) o).intValue();
    } else {
      throw new WarpScriptException(this.getName() + "expects a long for address, and a long for timeout");
    }

    stack.push(this.bacnet.openRemoteDevice(address, timeout));
    return stack;
  }
}
