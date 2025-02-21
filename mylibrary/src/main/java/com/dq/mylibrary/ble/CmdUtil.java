package com.dq.mylibrary.ble;

import static com.dq.mylibrary.DqLogUtilKt.dqLog;

import android.util.Log;


public class CmdUtil {
    final static byte[] key1 = new byte[]{0x01, 0x03, 0x19, 0x78, 0x44, 0x62, (byte) 0xea, (byte) 0x89, 0x55, (byte) 0xbe};
    final static byte[] key2 = new byte[]{(byte) 0xae, 0x25, 0x33, 0x02, 0x74, (byte) 0xbe, 0x5b, 0x50, 0x10, 0x77};
    final static byte[] key3 = new byte[]{(byte) 0xcd, 0x2a, 0x5b, 0x3d, 0x2f, 0x66, (byte) 0xbd, (byte) 0xa0, 0x72, 0x6d};
    final static byte[] key4 = new byte[]{(byte) 0xaa, 0x16, (byte) 0xcd, 0x4d, 0x38, 0x27, (byte) 0xac, (byte) 0xbe, 0x52, (byte) 0x88};
    public static int desNum = 0;//当前选择的秘钥
    private static int Num = 0;//0-255 自加1




    public static String printBytesByStringBuilder(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();

        for (byte aByte : bytes) {
            stringBuilder.append(byteToHexStr(aByte));
        }

        return stringBuilder.toString();

    }

    //把整数字符串，转变成byte字节
    public static byte hexToByte(String inHex) {
        byte nub = (byte) Integer.parseInt(inHex, 16);
        return (byte) Integer.parseInt(inHex, 16);
    }

    /**
     * 读取数据  解密
     *
     * @return
     */
    public static byte[] MsgRead(byte[] msgByte) {
        byte[] msgByte_new = msgByte.clone();
        byte keyType = (byte) ((byte) msgByte[2] ^ (byte) 0x5a); // 异或 获取秘钥索引
        msgByte_new[2] = keyType;

        int keyPos = HexStringToInt(((keyType & 0xf0) >> 4) + "");//字节转换十进制
//        keyPos = keyPos % 10;//取个位数
//        Log.e("1111","解密索引："+keyPos);
        byte[] keySeart = key1;//秘钥

        switch (keyPos) {
            case 0:
                keySeart = key1;
                break;
            case 1:
                keySeart = key2;
                break;
            case 2:
                keySeart = key3;
                break;
            case 3:
                keySeart = key4;
                break;
        }

        for (int i = 3; i < msgByte.length; i++) {
            keyPos = HexStringToInt(byteToHexStr(msgByte[i - 1]));//字节转换十进制
            keyPos = keyPos % 10;//取个位数
            msgByte_new[i] = (byte) ((byte) msgByte[i] ^ keySeart[keyPos]); //
        }
        return msgByte_new;
    }


    /**
     * byte  转 字符串 十六进制
     *
     * @param byteData
     * @return
     */
    public static String byteToHexStr(byte byteData) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[2];
        int v = byteData & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
        return new String(hexChars);
    }

    /**
     * 16进制字符串转十进制int
     *
     * @param HexString
     * @return
     */
    public static int HexStringToInt(String HexString) {
        int inJTFingerLockAddress = Integer.valueOf(HexString, 16);
        return inJTFingerLockAddress;
    }

    /**
     * byte[]  转 字符串 十六进制
     *
     * @param byteArray
     * @return
     */
    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}