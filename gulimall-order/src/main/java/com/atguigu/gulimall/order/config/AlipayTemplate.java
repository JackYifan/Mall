package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    // 在支付宝创建的应用的id 此处为沙箱测试的 APP_ID
    private   String app_id = "2021000117626674";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCwP9SaFoxSX9o6lnGVg2Ptmpv8Ec+D+ztItVEJWpKuZ5WpP/DMNqAKATyjSvBJy8J61Ffwsi4wR1fS6xoJY+NFC8YDf3ODSjj7qMCzZ+AUfPGy29Y9Hbe10q8LcfAo0lVcQ0a1uabTgeLcJuiWd9sqOARCUaE8d7kLo7PYGhFZHX1Qs/2Zf2G5LbW0BojcN4l1MMYzeD+8bHvjosNXVrjAzmzbaAPu6MGF7EU71wtOFtTC1ZpWI6zX8YDaz74dCkGY1tLzyxDEkyqGxykUHaXkuXml6Vp7IvCgX/4AZMrxjGbfHe1L/p7MR6nQe8WPrXhPnXEOMJTaHexusNIihSITAgMBAAECggEAE3L3wwk58t0g99YiMp2NKWsmS2qru6S9pghcKOwVw6kqmsKzj9V3U6NPTbW3Tm1tyKAmFCmVbz5wMpY/CUo8iiICd+BYnuRR5XN50FJmJ9yhz1rzCUt+OlJFfr3UAq0zPjsuUl9qSWzL2/9vKyuOw9nqnmbjpAiSIvatoes+ftbS3+Ihj6OxlVmSldsMQdNmQaiLUC98N/CBDqtj8rkQNE7+YQoradD7d3bVguy0gSKr9OGY4AxoscbiW1jnz6TY56DmemOyxxdPzh7DQoyU2aUOYfey3MyvNFhqlY1nzb7RpIKJZ+SEKoVy10DphjsN1ZUwcxRAurvdTuYfyo5mAQKBgQD1OnFOmJBvvNwyuTmq87XwlpKh1d1tnY2Y4G2h50CNmRkaicJ42hSe4/KTjDEPiQu11tH6M4WrDVS6rvBFwreSR6g369gcvAzuXA6RBnQk0PuPg9znm7l10OE0P8EnjsumCXqB0vCG7M+S53YGXTxMLmfjyVzcxxNB7pDlSeUKpQKBgQC3/bq0Cc8vZmbAvARL5R7DKx7QzMw47/pSDPYrswY04F+0TNAlj0ri5mzGS+N0e7o7ih5TDOh1ZT8tLApnDx0krklKxePC8YMot8xePK009oGYU6yPmTKu4VlyCWsYOt9b5wxFuSDsS99DNDePXnT+Oye+0R7pYnRK+BLx62E0VwKBgEOo5k5LJb7Omuqb1F9ocpB42ugv+7IcwE6nzVfYWCU5UoXR8Igrk3jrZ6hyC68/nq2DIdgokv7I5NHFJqH4wXFot+8F8VCsSd0SJSq6Xx0xU9cbCL4WZyssgJZL6N0jNukOwHBFZPu1JuhfWF8VJmfWfe+JRUfc3WFZkzzQEJPJAoGAYAi4jcZc9PGvCkRYvcBuiRIVuIhcxA9GCdOoHfEIeUrvyS0aq7AaN0psVihAJYl7EB6sa07eeoAJNKu7FkXGQWIS8UtO8W6btgIRUtK62V0dEYQDGsSstqj6xdHyqyf3c4GqXy4c6BlGBE7Z/SQhTp1Txn5icxCngIXxZYIlL2sCgYAjlD1DaOTJ0fPMBPBv5xKhIsIfhcJ9eCR1zwmsOt4VCu9CTCQegGQiuv9vt3oILGct66M9jYjX/SdkAInLdawlQLlHJLg+W16YDQ8jyReMU6fwbw9N+leghA/waG8HMAYkhxOcjNAarJxFmnl+ehXbIVXAEsMa6YwYyXiSHncy3w==" ;
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1m/9s/u3BbTtDmCXdfeSPa8f96cNwuXCj/ZnjoDSEr/UAgwZDDpZJvhPNspcT9B5KwOZduZ8NwQJOkXzuFytsR4tImmgD3w/jCD62v1VtzapB9bsCzTb6iUl7fNzqjY44lR5hzR9lWIONepEqbAzv8RgMYzu+mp4B7cjh5URgI7gj2qy8TTTnBq/Zj5LNFSdrGO+EKnPQinKOtCcZGH6KHCluf31aJWRG2Hcc2kQx2qEH5LITYtexMdZeljzWdIrSmM3R1Z3tWnZK8Fak57SEGLNZitQRL7AFwfnB0J9l9jt7KtHmsMEr5i4iSXCx1FkiB5XOdeoc6krw1S4wZSQVwIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url = "6vxd00en.dongtaiyuming.net/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    private String time = "10m";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+time+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);
        return result;

    }
}
