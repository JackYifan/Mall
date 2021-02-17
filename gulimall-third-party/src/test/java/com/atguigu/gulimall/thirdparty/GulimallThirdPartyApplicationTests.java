package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class GulimallThirdPartyApplicationTests {


    @Autowired
    OSSClient ossClient;
    @Test
    public void testUpload() throws Exception {
        FileInputStream fileInputStream = new FileInputStream("C:\\Users\\HONOR\\Pictures\\Snipaste_2020-12-19_12-40-39.png");
        ossClient.putObject("edu-wyf","test11.jpg",fileInputStream);
        ossClient.shutdown();
        System.out.println("success");
    }


}
