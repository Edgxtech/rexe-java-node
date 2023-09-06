package tech.edgx.dee.util;

public class HexUtil {
    public HexUtil() {
    }

    public static String encodeHexString(byte[] byteArray) {
        return byteArray == null ? null : encodeHexString(byteArray, false);
    }

    public static String encodeHexString(byte[] byteArray, boolean withPrefix) {
        if (byteArray == null) {
            return null;
        } else {
            StringBuffer hexStringBuffer = new StringBuffer();

            for(int i = 0; i < byteArray.length; ++i) {
                hexStringBuffer.append(byteToHex(byteArray[i]));
            }

            String hexString = hexStringBuffer.toString();
            if (hexString == null) {
                return null;
            } else {
                return withPrefix ? "0x" + hexString : hexString;
            }
        }
    }

    public static byte[] decodeHexString(String hexString) {
        if (hexString != null && hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException("Invalid hexadecimal String supplied. " + hexString);
        } else {
            byte[] bytes = new byte[hexString.length() / 2];

            for(int i = 0; i < hexString.length(); i += 2) {
                bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
            }

            return bytes;
        }
    }

    public static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte)((firstDigit << 4) + secondDigit);
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[]{Character.forDigit(num >> 4 & 15, 16), Character.forDigit(num & 15, 16)};
        return new String(hexDigits);
    }

    private static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if (digit == -1) {
            throw new IllegalArgumentException("Invalid Hexadecimal Character: " + hexChar);
        } else {
            return digit;
        }
    }

    public static void main(String[] args) {
        String hexValue = "4123d70f66414cc921f6ffc29a899aafc7137a99a0fd453d6b200863ef5702d6";
        byte[] bytes = decodeHexString(hexValue);
        String newVal = encodeHexString(bytes);
        System.out.printf(newVal);
    }
}
