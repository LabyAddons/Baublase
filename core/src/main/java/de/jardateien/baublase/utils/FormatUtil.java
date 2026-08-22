package de.jardateien.baublase.utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class FormatUtil {

  private static final DecimalFormat format = new DecimalFormat("#,###");

  public static String getFormat(double value) {
    return format.format(value);
  }

  public static String getFormat(double value, RoundingMode roundingMode) {
    format.setRoundingMode(roundingMode);
    return getFormat(value);
  }

}
