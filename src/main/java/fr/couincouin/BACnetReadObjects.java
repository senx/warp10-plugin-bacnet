package fr.couincouin;

import com.serotonin.bacnet4j.RemoteDevice;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

import java.util.Map;

public class BACnetReadObjects extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  BACnetWarp10Plugin bacnet;


  public BACnetReadObjects(String name, BACnetWarp10Plugin bacnet) {
    super(name);
    this.bacnet = bacnet;
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {

    Map params;
    boolean readDetail = false;
    RemoteDevice rd;
    
    Object o = stack.pop();
    if (o instanceof Boolean) {
      readDetail = (Boolean) o;
      o = stack.pop();
    }
    if (o instanceof Map) {
      params = (Map) o;
    } else {
      throw new WarpScriptException(this.getName() + " expects remote device, a parameters map and an optional boolean");
    }
    o = stack.pop();
    if (o instanceof RemoteDevice) {
      rd = (RemoteDevice) o;
    } else {
      throw new WarpScriptException(this.getName() + " expects remote device, a parameters map and an optional boolean");
    }


    stack.push(this.bacnet.readRemoteDevice(rd, params, !readDetail));

    return stack;
  }
}
