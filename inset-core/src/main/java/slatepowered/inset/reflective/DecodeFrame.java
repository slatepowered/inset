package slatepowered.inset.reflective;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;

@RequiredArgsConstructor
@Getter
public class DecodeFrame {

  private final CodecContext context;
  private final Object parentInstance;
  private final DecodeInput input;

}
