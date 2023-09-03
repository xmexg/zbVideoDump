package org.mex.zbVideoDown;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept;
import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static final int listenPort = 8111;
    public static final String m3u8Dir = "m3u8down";
    public static String Authorization = null;
    public static String UA = null;
    public static volatile List<String> m3u8_list = new ArrayList<>();

    public static void main_test(String[] args){
        String m3u8_url = "http://1259801863.vod2.myqcloud.com/4acb4ad5vodtranscq1259801863/1866ee823270835011237918881/drm/v.f47444.m3u8";
        String m3u8_name = m3u8_url.substring(m3u8_url.lastIndexOf("/")+1, m3u8_url.lastIndexOf("."));
        String m3u8_dir = m3u8Dir + "/" + m3u8_name;
        new video_down( "your_Authorization", "Dalvik/2.1.0 (Linux; U; Android 12; OPD2102 Build/RKQ1.211119.001)", m3u8_url, m3u8_dir, m3u8_name).start();
    }

    public static void main(String[] args) {
        System.out.println("Starting proxy for localhost:" + listenPort);

        HttpProxyServerConfig config =  new HttpProxyServerConfig();
        new HttpProxyServer()
                .serverConfig(config)
                .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline) {
                        pipeline.addLast(new FullResponseIntercept() { // 在请求头获取认证码和UA

                            @Override
                            public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                                if ((Authorization==null)&&httpRequest.uri().endsWith(".m3u8")) {
                                    String auth = httpRequest.headers().get("Authorization");
                                    String ua = httpRequest.headers().get("User-Agent");
                                    if(auth!=null){ // 不用if也可以, 因为若为null, 还会再来一次
                                        Authorization = auth;
                                        System.out.println("Authorization: "+Authorization);
                                        UA = ua;
                                        System.out.println("User-Agent: "+UA);
                                    }
                                }
                                return false;
                            }

                            // 不需要做任何事
                            @Override
                            public void handleResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {}
                        });
                        pipeline.addLast(new FullResponseIntercept() { // 获取视频详情,内置m3u8地址和视频名称

                            @Override
                            public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                                return pipeline.getHttpRequest().uri().startsWith("/getplayinfo");
                            }

                            @Override
                            public void handleResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                                if(Authorization==null){
                                    System.out.println("已捕获视频信息,但Authorization无效,无法下载视频");
                                    return;
                                }
                                String content = httpResponse.content().toString(Charset.defaultCharset());
                                System.out.println(content);
                                Gson gson = new Gson();
                                JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
                                //获取最后一个m3u8地址
                                String m3u8_url = jsonObject.get("videoInfo").getAsJsonObject().get("transcodeList").getAsJsonArray().get(jsonObject.get("videoInfo").getAsJsonObject().get("transcodeList").getAsJsonArray().size()-1).getAsJsonObject().get("url").getAsString();
                                // 获取视频名称
                                String info_name = jsonObject.get("videoInfo").getAsJsonObject().get("basicInfo").getAsJsonObject().get("name").getAsString();
                                //Print raw packet
                                String m3u8_name = info_name + "_" + httpRequest.uri().split("/")[2];
                                String m3u8_dir = m3u8Dir + "/" + m3u8_name;
                                System.out.println("视频名称:" + m3u8_name + "\t" + "视频下载地址:" + m3u8_url);
                                if(m3u8_list.contains(m3u8_url)){
                                    return;
                                } else {
                                    System.out.println("开始下载: "+m3u8_url);
                                    m3u8_list.add(m3u8_url);
                                }
                                // httpRequest.headers().entries().stream().collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll)
                                new Thread(() -> new video_down(Authorization, UA, m3u8_url, m3u8_dir, m3u8_name).start()).start();
                            }
                        });
                    }
                })
                .start(listenPort);
    }
}
