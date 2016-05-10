package texasholdem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Shared static methods.
 */
public class SharedUtilities {

   /**
    * Converts a serializable object to a byte array.
    * @param ser The object to be serialized
    * @return The serialized object, as a byte array
    * @throws IOException If an I/O error occurs
    */
   public static byte[] toByteArray(Serializable ser) throws IOException {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(ser);
      return bos.toByteArray();
   }

   /**
    * Converts a byte array to an object.
    * @param bytes The byte array
    * @return The object
    * @throws IOException If an I/O error occurs
    * @throws ClassNotFoundException If class of a serialized object cannot be
    *         found
    */
   public static Object toObject(byte[] bytes)
         throws IOException, ClassNotFoundException {
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bis);
      return ois.readObject();
   }

   /**
    * Converts an array of bytes to a long.
    * @param bytes The array of bytes to be converted
    * @return The long value
    */
   public static long bytesToLong(byte[] bytes) {
      long mask = 0b11111111L;
      long value = 0;
      for(byte b: bytes) {
         value <<= Byte.SIZE;
         value |= (b & mask);
      }
      return value;
   }
}