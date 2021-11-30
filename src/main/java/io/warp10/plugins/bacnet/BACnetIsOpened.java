package io.warp10.plugins.bacnet;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class BACnetIsOpened extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  BACnetWarp10Plugin bacnet;

  public BACnetIsOpened(String name, BACnetWarp10Plugin bacnet) {
    super(name);
    this.bacnet = bacnet;
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    stack.push(bacnet.bacNetIsOpened());
    return stack;
  }
}
