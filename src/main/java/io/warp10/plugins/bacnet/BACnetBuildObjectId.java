package io.warp10.plugins.bacnet;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class BACnetBuildObjectId extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  BACnetWarp10Plugin bacnet;

  public BACnetBuildObjectId(String name, BACnetWarp10Plugin bacnet) {
    super(name);
    this.bacnet = bacnet;
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    int instanceNumber;
    String objectType;
    Object o = stack.pop();
    if (o instanceof Long) {
      instanceNumber = ((Long) o).intValue();
    } else {
      throw new WarpScriptException(this.getName() + "expects a string for object type, and a long for object instance number");
    }

    o = stack.pop();
    if (o instanceof String) {
      objectType = (String) o;
    } else {
      throw new WarpScriptException(this.getName() + "expects a string for object type, and a long for object instance number");
    }

    stack.push(this.bacnet.buildOjectIdentifier(objectType, instanceNumber));

    return stack;
  }
}
