package com.iflytek;

/**
 * key 由14-16位统一转为15位
 * @author 10530
 * @date 2018/1/30.
 */
public class Utils {
    /**
     * 格式化IMEI
     * 因为IMEI格式不统一，长度有14位和16位的，所以，为了统一，将14位和16位的MEID，统一设置为15位的 设置格式：
     * 如果IMEI长度为14位，那么直接得到第15位，如果MEID长度为16位，那么直接在根据前14位得到第15位
     * 如果IMEI长度为其他长度，那么直接返回原值
     *
     * @param imei
     * @return
     */
    public static String formatImei(String imei) {
        int dxml = imei.length();
        if (dxml != 14 && dxml != 16) {
            return imei;
        } else if (!imei.substring(0, 14).matches("[0-9]+")) {
            // 添加一步判断是否为纯数字，否则后面做int运算会抛异常
            return imei;
        }
        String imeiRes = "";
        if (dxml == 14) {
            imeiRes = imei + getimei15(imei);
        }
        if (dxml == 16) {
            imeiRes = imei.substring(0, 14) + getimei15(imei.substring(0, 14));
        }
        return imeiRes;
    }

    /**
     *根据IMEI的前14位，得到第15位的校验位
     * IMEI校验码算法：
     * (1).将偶数位数字分别乘以2，分别计算个位数和十位数之和
     * (2).将奇数位数字相加，再加上上一步算得的值
     * (3).如果得出的数个位是0则校验位为0，否则为10减去个位数
     * 如：35 89 01 80 69 72 41 偶数位乘以2得到5*2=10 9*2=18 1*2=02 0*2=00 9*2=18 2*2=04 1*2=02,计算奇数位数字之和和偶数位个位十位之和，
     * 得到 3+(1+0)+8+(1+8)+0+(0+2)+8+(0+0)+6+(1+8)+7+(0+4)+4+(0+2)=63
     * 校验位 10-3 = 7
     *
     * @param imei
     * @return
     */
    public static String getimei15(String imei) {
        if (imei.length() == 14) {
            char[] imeiChar = imei.toCharArray();
            int resultInt = 0;
            for (int i = 0; i < imeiChar.length; i++) {
                int a = Integer.parseInt(String.valueOf(imeiChar[i]));
                i++;
                final int temp = Integer.parseInt(String.valueOf(imeiChar[i])) * 2;
                final int b = temp < 10 ? temp : temp - 9;
                resultInt += a + b;
            }
            resultInt %= 10;
            resultInt = resultInt == 0 ? 0 : 10 - resultInt;
            return resultInt + "";
        } else {
            return "";
        }
    }
}
