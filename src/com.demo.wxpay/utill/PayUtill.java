package com.demo.wxpay.utill;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.AutoUpdateCertificatesVerifier;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import com.wechat.pay.contrib.apache.httpclient.util.RsaCryptoUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

public class PayUtill {

    /**
     * 获取预支付id，返回调起app支付所需要的数据
     * @param goodMsg
     * @param amount
     * @return
     * @throws Exception
     */
    public static Map createOrder(String goodMsg, String amount) throws Exception{

        //-----------------------------初始化开始-------------------
        PrivateKey merchantPrivateKey = PemUtil.loadPrivateKey(
                new ByteArrayInputStream(PayConstants.PRIVATE_KEY.getBytes("utf-8")));

        //使用自动更新的签名验证器，不需要传入证书
        AutoUpdateCertificatesVerifier verifier = new AutoUpdateCertificatesVerifier(
                new WechatPay2Credentials(PayConstants.MCH_ID, new PrivateKeySigner(PayConstants.MCH_SERIAL_NO, merchantPrivateKey)),
                PayConstants.API_V3KEY.getBytes("utf-8"));

        CloseableHttpClient httpClient = WechatPayHttpClientBuilder.create()
                .withMerchant(PayConstants.MCH_ID, PayConstants.MCH_SERIAL_NO, merchantPrivateKey)
                .withValidator(new WechatPay2Validator(verifier))
                .build();
        //-----------------------------初始化结束-------------------

        //----------------------------------- jsapi下单获取预支付id 开始 -------------------------
        HttpPost httpPost = new HttpPost("https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi");
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-type","application/json; charset=utf-8");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode rootNode = objectMapper.createObjectNode();
        String outTradeNo = System.currentTimeMillis() + "";
        System.out.println("内部订单号:"  +outTradeNo);
        rootNode.put("mchid",PayConstants.MCH_ID)
                .put("appid", PayConstants.APP_ID)
                .put("notify_url", PayConstants.NOTIFY_URL) // 回调地址
                .put("description", goodMsg) // 商品名称
                // .put("description", "Image形象店-深圳腾大-QQ公仔") // 商品名称

                .put("out_trade_no", outTradeNo); // 己方唯一订单号(雪花)
        rootNode.putObject("amount")
                .put("total", amount); // 总金额 分
        //rootNode.putObject("payer")
        //        .put("openid", "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o");

        objectMapper.writeValue(bos, rootNode);

        httpPost.setEntity(new StringEntity(bos.toString("UTF-8"), "UTF-8"));
        CloseableHttpResponse response = httpClient.execute(httpPost);

        String bodyAsString = EntityUtils.toString(response.getEntity());
        System.out.println(bodyAsString); //  { "prepay_id": "wx26112221580621e9b071c00d9e093b0000"}
        JsonNode node = objectMapper.readTree(bodyAsString);
        String prepayId = node.get("prepay_id").toString();
        //----------------------------------- jsapi下单获取预支付id 结束 -------------------------

        //----------------------------------- 计算签名 开始 -------------------------
        StringBuilder sb = new StringBuilder();
        String timeStamp = System.currentTimeMillis() + ""; // 由前端传入
        String nonce = RandomUtil.randomString(30); // 30位随机字符串
        //应用id
        sb.append(PayConstants.APP_ID).append("\n");
        //时间戳
        sb.append(timeStamp).append("\n");
        //随机字符串
        sb.append(nonce).append("\n");
        //预支付交易会话ID
        sb.append(prepayId).append("\n");
        //RsaCryptoUtil.encryptOAEP() 加密
        //RsaCryptoUtil.decryptOAEP() 解密
        String ciphertext = RsaCryptoUtil.encryptOAEP(sb.toString(), verifier.getValidCertificate());
        System.out.println(ciphertext); // 计算后的签名值
        //----------------------------------- 计算签名 结束 -------------------------

        // 前端调起app支付所需要的参数
        Map map = new HashMap();
        map.put("noncestr", nonce);
        map.put("package", PayConstants.PACKAGE);
        map.put("timestamp", timeStamp);
        map.put("sign", ciphertext);

        return map;
    }

    /**
     * 验签工具类
     * @param serialNumber
     * @param message
     * @param signature
     * @return
     */
    public static boolean signVerify(String serialNumber, String message, String signature) {

        try {
            //-----------------------------初始化开始-------------------
            PrivateKey merchantPrivateKey = PemUtil.loadPrivateKey(
                    new ByteArrayInputStream(PayConstants.PRIVATE_KEY.getBytes("utf-8")));

            //使用自动更新的签名验证器，不需要传入证书
            AutoUpdateCertificatesVerifier verifier = new AutoUpdateCertificatesVerifier(
                    new WechatPay2Credentials(PayConstants.MCH_ID, new PrivateKeySigner(PayConstants.MCH_SERIAL_NO, merchantPrivateKey)),
                    PayConstants.API_V3KEY.getBytes("utf-8"));

            CloseableHttpClient httpClient = WechatPayHttpClientBuilder.create()
                    .withMerchant(PayConstants.MCH_ID, PayConstants.MCH_SERIAL_NO, merchantPrivateKey)
                    .withValidator(new WechatPay2Validator(verifier))
                    .build();
            //-----------------------------初始化结束-------------------

            return verifier.verify(serialNumber, message.getBytes("utf-8"), signature);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 解密请求体
     * @param data
     * @return
     */
    public static String decryptData(String data)  {
        try {
            AesUtil aes  =new AesUtil(PayConstants.API_V3KEY.getBytes("utf-8"));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(data);
            JsonNode resource = node.get("resource"); // 通知资源数据
            String ciphertext = resource.get("ciphertext").textValue(); // 数据密文(需解密)
            String associatedData = resource.get("associated_data").textValue();// 附加数据
            String nonce = resource.get("nonce").textValue(); // 随机串
            return aes.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8), nonce.getBytes(StandardCharsets.UTF_8)
            , ciphertext);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 关闭订单
     * @param outTradeNo
     * @return
     * @throws Exception
     */
    public static String closeOrder(String outTradeNo) throws Exception{
        //-----------------------------初始化开始-------------------
        PrivateKey merchantPrivateKey = PemUtil.loadPrivateKey(
                new ByteArrayInputStream(PayConstants.PRIVATE_KEY.getBytes("utf-8")));

        //使用自动更新的签名验证器，不需要传入证书
        AutoUpdateCertificatesVerifier verifier = new AutoUpdateCertificatesVerifier(
                new WechatPay2Credentials(PayConstants.MCH_ID, new PrivateKeySigner(PayConstants.MCH_SERIAL_NO, merchantPrivateKey)),
                PayConstants.API_V3KEY.getBytes("utf-8"));

        CloseableHttpClient httpClient = WechatPayHttpClientBuilder.create()
                .withMerchant(PayConstants.MCH_ID, PayConstants.MCH_SERIAL_NO, merchantPrivateKey)
                .withValidator(new WechatPay2Validator(verifier))
                .build();
        //-----------------------------初始化结束-------------------

        HttpPost httpPost = new HttpPost("https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/"+ outTradeNo +"/close");
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-type","application/json; charset=utf-8");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("mchid",PayConstants.MCH_ID);

        objectMapper.writeValue(bos, rootNode);

        httpPost.setEntity(new StringEntity(bos.toString("UTF-8"), "UTF-8"));
        CloseableHttpResponse response = httpClient.execute(httpPost);
        System.out.println("订单号："+ outTradeNo +" 请求关单返回状态：" + response.getStatusLine().getStatusCode()); // 接口响应204，无内容即成功
        String bodyAsString = EntityUtils.toString(response.getEntity());
        System.out.println(bodyAsString);
        return bodyAsString;
    }
}
