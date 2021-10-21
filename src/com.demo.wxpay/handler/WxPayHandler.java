package com.demo.wxpay.handler;

import com.demo.wxpay.utill.PayUtill;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WxPayHandler {

    @PostMapping("/create")
    public Map createOrder(String goodMsg, String amount) throws Exception {
        // TODO 鉴权 验证库存等业务代码

        Map order = PayUtill.createOrder(goodMsg, amount);
        return order;
    }

    @PostMapping("/callback")
    public Map callBack(HttpServletRequest request) {
        // 拿去请求头中的信息，用与验签
        String timeStamp = request.getHeader("Wechatpay-Timestamp"); // 应答时间戳
        String nonce = request.getHeader("Wechatpay-Nonce"); // 应答时随机串 跟随签名变化
        String sign = request.getHeader("Wechatpay-Signature"); // 应答签名 根据报文主体的变化而变化
        String serial = request.getHeader("Wechatpay-Serial"); // 序列号
        System.out.println("Wechatpay-Timestamp:" + timeStamp);
        System.out.println("Wechatpay-Nonce:" + nonce);
        System.out.println("Wechatpay-Signature:" + sign);
        System.out.println("Wechatpay-Serial:" + serial);
        Map result = new HashMap();
        try {
            BufferedReader reader = request.getReader();
            String str;
            StringBuilder sb = new StringBuilder();
            while((str = reader.readLine()) != null){
                sb.append(str);
            }
            System.out.println("请求主体：" + sb);
            // ----------------验证签名----------------
            StringBuilder signStr = new StringBuilder();
            signStr.append(timeStamp).append("\n");
            signStr.append(nonce).append("\n");
            signStr.append(sb).append("\n");

            if(!PayUtill.signVerify(serial, signStr.toString(), sign)){
                result.put("code","FAIL");
                result.put("message","***");
                return result;
            }

            // ----------------解密密文----------------
            String s = PayUtill.decryptData(sb.toString());
            System.out.println("解密密文:" + s); // 解密密文

            // ----------------验证订单----------------



//            result.put("code","SUCCESS");
            result.put("code","FAIL"); // 微信回调resp判断code，为suc则支付成功，其他则支付失败
            result.put("message","成功");


        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
