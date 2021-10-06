import cn.hutool.core.util.RandomUtil;
import com.demo.wxpay.utill.PayConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.AutoUpdateCertificatesVerifier;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import com.wechat.pay.contrib.apache.httpclient.util.RsaCryptoUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

public class MyTest {

    private CloseableHttpClient httpClient;
    private AutoUpdateCertificatesVerifier verifier;

    /**
     * 初始化 httpClient 和 verifier
     * @throws IOException
     */
    @Before
    public void setup() throws IOException {
        PrivateKey merchantPrivateKey = PemUtil.loadPrivateKey(
                new ByteArrayInputStream(PayConstants.PRIVATE_KEY.getBytes("utf-8")));

        //使用自动更新的签名验证器，不需要传入证书
        verifier = new AutoUpdateCertificatesVerifier(
                new WechatPay2Credentials(PayConstants.MCH_ID, new PrivateKeySigner(PayConstants.MCH_SERIAL_NO, merchantPrivateKey)),
                PayConstants.API_V3KEY.getBytes("utf-8"));

        httpClient = WechatPayHttpClientBuilder.create()
                .withMerchant(PayConstants.MCH_ID, PayConstants.MCH_SERIAL_NO, merchantPrivateKey)
                .withValidator(new WechatPay2Validator(verifier))
                .build();
    }

    /**
     * jsapi下单获取预支付id
     * @throws Exception
     */
    @Test
    public void createOrder() throws Exception{
        //----------------------------------- jsapi下单获取预支付id 开始 -------------------------
        HttpPost httpPost = new HttpPost("https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi");
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-type","application/json; charset=utf-8");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("mchid",PayConstants.MCH_ID)
                .put("appid", PayConstants.APP_ID)
                .put("notify_url", PayConstants.NOTIFY_URL) // 回调地址
                .put("description", "Image形象店-深圳腾大-QQ公仔") // 商品名称

                .put("out_trade_no", "1217752501201407033233368018"); // 己方唯一订单号(雪花)
        rootNode.putObject("amount")
                .put("total", 1); // 总金额 分
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
    }
}
