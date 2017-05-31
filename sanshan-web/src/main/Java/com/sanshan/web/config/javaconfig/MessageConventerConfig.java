package com.sanshan.web.config.javaconfig;

import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter4;
import com.alibaba.fastjson.util.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.fastjson.serializer.SerializerFeature.WriteNullListAsEmpty;

/**
 * 采用的FastJson作为Http转换器
 */
@Configuration
public class MessageConventerConfig {

    @Bean
    public FastJsonHttpMessageConverter4 fastJsonHttpMessageConverter4(FastJsonConfig fastJsonConfig){
        FastJsonHttpMessageConverter4 jsonHttpMessageConverter4 = new FastJsonHttpMessageConverter4();
        List<MediaType> list = new ArrayList();
        list.add(MediaType.APPLICATION_JSON_UTF8);
        jsonHttpMessageConverter4.setSupportedMediaTypes(list);
        jsonHttpMessageConverter4.setFastJsonConfig(fastJsonConfig);
        return jsonHttpMessageConverter4;
    }

    @Bean
    public FastJsonConfig fastJsonConfig(){
        FastJsonConfig config = new FastJsonConfig();
        //其实默认也是UTF-8编码
        config.setCharset(IOUtils.UTF8);
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        return config;
    }

}
