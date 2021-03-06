package com.kakacl.product_service.utils;

import com.kakacl.product_service.config.Constant;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author wangwei
 * @version v1.0.0
 * @description 签名
 * @date 2019-01-12
 */
@RefreshScope
public class SignUtil {

    private static Logger logger = LoggerFactory.getLogger(SignUtil.class);

    public static SignUtil signUtil;

    // 对外的密钥
    public String secretkey = Constant.SIGN_SECRETKEY;

    /** 加密密钥 */
    public String appkey = Constant.SIGN_APPKEY;

    // 间隔时间
    //@Value("${sign.timeout}")
    public int timeout = 1 * 30 * 1000;

    /** 字符编码 */
    private final static String INPUT_CHARSET = "UTF-8";

    public static SignUtil getInstance() {
        if(signUtil == null) {
            signUtil = new SignUtil();
        }
        return signUtil;
    }

    /**
     * 请求参数Map转换验证Map
     * @param requestParams 请求参数Map
     * @param charset 是否要转utf8编码
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Map<String,String> toVerifyMap(Map<String, String[]> requestParams, boolean charset) {
        Map<String,String> params = new HashMap<>();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
            if(charset)
                valueStr = getContentString(valueStr, INPUT_CHARSET);
            params.put(name, valueStr);
        }
        return params;
    }

    /**
     * 除去数组中的空值和签名参数
     * @param sArray 签名参数组
     * @return 去掉空值与签名参数后的新签名参数组
     */
    public static Map<String, String> paraFilter(Map<String, String> sArray) {
        Map<String, String> result = new HashMap<>();
        if (sArray == null || sArray.size() <= 0) {
            return result;
        }
        for (String key : sArray.keySet()) {
            String value = sArray.get(key);
            if (value == null || value.equals("") || key.equalsIgnoreCase("sign")) {
                // || key.equalsIgnoreCase("time") 这里可以将时间参数分离，因为时间参数为毫秒 接口传递需要时间，如果毫秒数一旦超过，则签名验证失败，后面有判断签名时间在合理范围内
                continue;
            }
            result.put(key, value);
        }
        return result;
    }

    /**
     * 把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
     * @param params 需要排序并参与字符拼接的参数组
     * @return 拼接后字符串
     */
    public static String createLinkString(Map<String, String> params) {
        return createLinkString(params, false);
    }

    /**
     * 把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
     * @param params 需要排序并参与字符拼接的参数组
     * @param encode 是否需要UrlEncode
     * @return 拼接后字符串
     */
    public static String createLinkString(Map<String, String> params, boolean encode) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        String prestr = "";
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (encode)
                value = urlEncode(value, INPUT_CHARSET);
            if (i == keys.size() - 1) {//拼接时，不包括最后一个&字符
                prestr = prestr + key + "=" + value;
            } else {
                prestr = prestr + key + "=" + value + "&";
            }
        }
        return prestr;
    }

    /**
     * 编码转换
     * @param content
     * @param charset
     * @return
     * @throws UnsupportedEncodingException
     */
    public static byte[] getContentBytes(String content, String charset) {
        if (charset == null || "".equals(charset)) {
            return content.getBytes();
        }
        try {
            return content.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("MD5签名过程中出现错误,指定的编码集不对,您目前指定的编码集是:" + charset);
        }
    }

    /**
     * 编码转换
     * @param content
     * @param charset
     * @return
     */
    private static String getContentString(String content, String charset) {
        if (charset == null || "".equals(charset)) {
            return new String(content.getBytes());
        }
        try {
            return new String(content.getBytes("ISO-8859-1"), charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("指定的编码集不对,您目前指定的编码集是:" + charset);
        }
    }

    /**
     * URL转码
     * @param content
     * @param charset
     * @return
     */
    private static String urlEncode(String content, String charset) {
        try {
            return URLEncoder.encode(content, charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("指定的编码集不对,您目前指定的编码集是:" + charset);
        }
    }



    //TODO 签名
    /**
     * 生成要请求的签名参数数组
     * @param sParaTemp 需要签名的参数Map
     * @return 要请求的签名参数数组
     */
    public static Map<String, String> signMap(Map<String, String[]> sParaTemp) {
        //请求参数Map转换验证Map，并生成要请求的签名参数数组
        return SignUtil.getInstance().sign(toVerifyMap(sParaTemp, false));
    }

    /**
     * 生成要请求的签名参数数组
     * @param sParaTemp 需要签名的参数
     * @return 要请求的签名参数数组
     */
    public Map<String, String> sign(Map<String, String> sParaTemp) {
        //时间戳加入签名参数组中
        sParaTemp.put("timestamp", String.valueOf(System.currentTimeMillis()));
        //除去数组中的空值和签名参数
        Map<String, String> sPara = paraFilter(sParaTemp);
        //把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        String prestr = createLinkString(sPara);
        //生成签名结果
        String mysign = DigestUtils.md5Hex(getContentBytes(prestr + appkey, INPUT_CHARSET));
        //签名结果加入请求提交参数组中
        sPara.put("sign", mysign);
        return sPara;
    }

    public static String getSignStr(Map<String, String> sParaTemp) {
        return SignUtil.getInstance().sign(sParaTemp).get("sign");
    }



    /**
     * 生成要请求的签名参数字符串“参数=参数值”&链接
     * @param sParaTemp 需要签名的参数Map
     * @return 请求的签名参数字符串
     */
    public static String signStringMap(Map<String, String[]> sParaTemp) {
        //生成要请求的签名参数数组
        Map<String, String> sign = signMap(sParaTemp);
        //生成要请求的签名参数字符串“参数=参数值”&链接
        return createLinkString(sign, true);
    }

    /**
     * 生成要请求的签名参数字符串“参数=参数值”&链接
     * @param sParaTemp 需要签名的参数
     * @return
     */
    public static String signString(Map<String, String> sParaTemp) {
        //生成要请求的签名参数数组
        Map<String, String> sign = SignUtil.getInstance().sign(sParaTemp);
        //生成要请求的签名参数字符串“参数=参数值”&链接
        return createLinkString(sign, true);
    }


    //TODO 验证签名
    /**
     * 根据反馈回来的信息，生成签名结果
     * @param paramsMap 通知返回来的请求参数Map
     * @return 验证结果
     */
    public static boolean verifyMap(Map<String, String[]> paramsMap) {
        //请求参数Map转换验证Map，并根据反馈回来的信息，生成签名结果
        return SignUtil.getInstance().verify(toVerifyMap(paramsMap, false));
    }

    /**
     * 根据反馈回来的信息，生成签名结果
     * @param params 通知返回来的参数数组
     * @return 验证结果
     */
    public boolean verify(Map<String, String> params) {
        String sign = "";
        if (params.get("sign") != null) {
            sign = params.get("sign");
        }else {
            logger.info("sign is null");
            return false;
        }
        String timestamp = "";
        if (params.get("time") != null) {
            timestamp = params.get("time");
        }else {
            return false;
        }
        //过滤空值、sign
        Map<String, String> sParaNew = paraFilter(params);
        //获取待签名字符串
        String preSignStr = createLinkString(sParaNew);
        //获得签名验证结果
        String mysign = DigestUtils.md5Hex(getContentBytes(preSignStr + appkey, INPUT_CHARSET));
        logger.info("mysign {}", mysign);
        if (mysign.equals(sign)) {
            //是否超时
            long curr = System.currentTimeMillis();
            if ((curr - Long.valueOf(timestamp)) > timeout){
                logger.info("api is time out " + curr);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }


}

