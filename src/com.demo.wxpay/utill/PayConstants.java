package com.demo.wxpay.utill;

public interface PayConstants {
    String NOTIFY_URL = "";  // 支付成功回调地址
    String MCH_ID = ""; // 商户号
    String MCH_SERIAL_NO = ""; // 商户证书序列号
    String API_SERIAL_NO = ""; // api密匙
    String APP_ID = ""; // appid
    String PACKAGE = "Sign=WXPay"; // 签名固定字符串(wx要求)
    String API_V3KEY = ""; // API_V3KEY
    // 你的商户私钥
    String PRIVATE_KEY =  "-----BEGIN PRIVATE KEY-----\n"
            + "-----END PRIVATE KEY-----\n";
}
