package io.warp10.plugins.bacnet;

import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

import java.math.BigDecimal;
import java.util.Map;

public class BACnetWriteObjects extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  BACnetWarp10Plugin bacnet;


  public BACnetWriteObjects(String name, BACnetWarp10Plugin bacnet) {
    super(name);
    this.bacnet = bacnet;
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    
    ObjectIdentifier id;
    RemoteDevice rd;
    
    Object v = stack.pop();
    boolean isDouble =Double.class.isAssignableFrom(v.getClass()) || Float.class.isAssignableFrom(v.getClass()) || BigDecimal.class.isAssignableFrom(v.getClass());
    if (!(isDouble || v instanceof Long)) {
      throw new WarpScriptException(this.getName() + " only support Long and Double values");
    }  
    
    Object o = stack.pop();
    if (o instanceof ObjectIdentifier) {
      id = (ObjectIdentifier) o;
    } else {
      throw new WarpScriptException(this.getName() + " expects remote device, objectId and a value");
    }
    o = stack.pop();
    if (o instanceof RemoteDevice) {
      rd = (RemoteDevice) o;
    } else {
      throw new WarpScriptException(this.getName() + " expects remote device, objectId and a value");
    }


    this.bacnet.writeRemoteDevice(rd, id,v);
    
    return stack;
  }
}
