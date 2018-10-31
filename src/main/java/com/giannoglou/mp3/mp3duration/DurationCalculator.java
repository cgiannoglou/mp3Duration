/*
 * @author Christos Giannoglou
 *
 * 2018
 *
 */
package com.giannoglou.mp3.mp3duration;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurationCalculator {
  private String[] versions = {"2.5", "0", "2", "1"};
  private String[] layers = {"0", "3", "2", "1"};

  private Map<String, int[]> bitRates = new HashMap<String, int[]>();
  private Map<String, int[]> sampleRates = new HashMap<String, int[]>();
  private Map<String, int[]> samples = new HashMap<String, int[]>();


  public void setBitRates() {
    bitRates.put("V1L0", new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    bitRates.put("V1L1",
        new int[] {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448});
    bitRates.put("V1L2",
        new int[] {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384});
    bitRates.put("V1L3",
        new int[] {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320});
    bitRates.put("V2L0", new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    bitRates.put("V2L1",
        new int[] {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256});
    bitRates.put("V2L2", new int[] {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160});
    bitRates.put("V2L3", new int[] {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160});
    bitRates.put("V0L0", new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    bitRates.put("V0L1", new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    bitRates.put("V0L2", new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    bitRates.put("V0L3", new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
  }

  public void setSampleRates() {
    sampleRates.put("0", new int[] {0, 0, 0, 0});
    sampleRates.put("1", new int[] {44100, 48000, 32000, 0});
    sampleRates.put("2", new int[] {22050, 24000, 16000, 0});
    sampleRates.put("2.5", new int[] {11025, 12000, 8000, 0});
  }

  public void setSamples() {
    samples.put("0", new int[] {0, 0, 0, 0});
    samples.put("1", new int[] {0, 384, 1152, 1152});
    samples.put("2", new int[] {0, 384, 1152, 576});
  }

  private int skipID3(int[] buffer) {
    int id3v2_flags = 0;
    int footer_size;
    int tag_size;
    if (buffer[0] == 0x49 && buffer[1] == 0x44 && buffer[2] == 0x33) { // 'ID3'
      id3v2_flags = buffer[5];
    }
    if ((id3v2_flags & 0x10) != 0) {
      footer_size = 10;
    } else {
      footer_size = 0;
    }

    int z0 = buffer[6];
    int z1 = buffer[7];
    int z2 = buffer[8];
    int z3 = buffer[9];

    if ((z0 & 0x80) == 0 && (z1 & 0x80) == 0 && (z2 & 0x80) == 0 && (z3 & 0x80) == 0) {
      tag_size =
          ((z0 & 0x7f) * 2097152) + ((z1 & 0x7f) * 16384) + ((z2 & 0x7f) * 128) + (z3 & 0x7f);
      return 10 + tag_size + footer_size;
    }
    return 0;
  }

  private double roundDuration(double duration) {
    duration = Math.round(duration * 1000);
    return (duration / 1000);
  }

  private double[] parseFrameHeader(int[] header) {
    int b1 = header[1];
    int b2 = header[2];
    String simple_version;
    int bit_rate;
    int sample_rate;
    int sample_rate_idx;
    int sample;
    int padding_bit;

    int version_bits = (b1 & 0x18) >> 3;
    String version = versions[version_bits];
    if (version.equals("2.5")) {
      simple_version = "2";
    } else {
      simple_version = version;
    }

    int layer_bits = (b1 & 0x06) >> 1;
    String layer = layers[layer_bits];

    String bit_rate_key = "V" + simple_version + "L" + layer;
    int bit_rate_index = (b2 & 0xf0) >> 4;

    if (bitRates.containsKey(bit_rate_key) && bit_rate_index!=15 && !layer.equals("0") && !version.equals("0")) {
      bit_rate = bitRates.get(bit_rate_key)[bit_rate_index];
    }
    else {
      bit_rate = 0;
    }

    sample_rate_idx = (b2 & 0x0c) >> 2;

    if (sampleRates.containsKey(version) && sample_rate_idx!=3) {
      sample_rate = sampleRates.get(version)[sample_rate_idx];
    } else {
      sample_rate = 0;
    }

    sample = samples.get(simple_version)[Integer.parseInt(layer)];

    padding_bit = (b2 & 0x02) >> 1;

    double[] results = {bit_rate, sample_rate,
        frameSize(sample, Integer.parseInt(layer), bit_rate, sample_rate, padding_bit), sample};
    return results;
  }

  public double frameSize(int samples, int layer, int bit_rate, int sample_rate, int paddingBit) {
    if (Integer.toString(layer).equals("1")) {
       if(sample_rate == 0 || bit_rate == 0) {
       return 0;
       }
      return (((samples * bit_rate * 125) / sample_rate) + paddingBit * 4);
    } else { // layer 2, 3
       if(sample_rate == 0 || bit_rate == 0) {
       return 0;
       }
      return (((samples * bit_rate * 125) / sample_rate) + paddingBit);
    }
  }

  public double mp3Duration(String filename) {
    double duration = 0;
    double[] info = null;
    File f = new File(filename);
    long filesize = f.length();

    try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(f))) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      int read;
      byte[] buff = new byte[1024];
      while ((read = input.read(buff)) > 0) {
        out.write(buff, 0, read);
      }
      out.flush();
      byte[] audioBytes = out.toByteArray();
      int[] audioBytes1 = new int[(int) filesize];
      for (int i = 0; i < audioBytes.length; i++) {
        audioBytes1[i] = audioBytes[i] & 0xFF;
      }

      int[] buffer = new int[100];
      if (audioBytes1.length >= 100) {
        for (int i = 0; i < 100; i++) {
          buffer[i] = audioBytes1[i];
        }
      } else {
        return 0;
      }

      int offset = skipID3(buffer);
      while (true) {
        int[] buffer1 = new int[10];

        if (audioBytes1.length - offset >= 10) {
          for (int i = 0; i < 10; i++) {
            buffer1[i] = audioBytes1[offset + i];
          }
        } else {
          return roundDuration(duration);
        }
        if (buffer1[0] == 0xff && (buffer1[1] & 0xe0) == 0xe0) {
          info = parseFrameHeader(buffer1);
          if (info[2] != 0 && info[3] != 0) {
            offset += info[2];
            duration += (info[3] / info[1]);
          } else {
            offset += 1; // Corrupt file?
          }
        } else if (buffer1[0] == 0x54 && buffer1[1] == 0x41 && buffer1[2] == 0x47) { // #TAG'
          offset += 128; // Skip over id3v1 tag size
        } else {
          offset += 1; // Corrupt file?
        }
      }
    } catch (FileNotFoundException e) {
      Logger logger = LoggerFactory.getLogger(DurationCalculator.class);
      logger.error("ERROR", e);
    } catch (IOException e) {
      Logger logger = LoggerFactory.getLogger(DurationCalculator.class);
      logger.error("ERROR", e);
    }
    return roundDuration(duration);
  }
}
