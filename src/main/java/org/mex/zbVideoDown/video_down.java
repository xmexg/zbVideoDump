package org.mex.zbVideoDown;

import net.m3u8.download.M3u8DownloadFactory;
import net.m3u8.listener.DownloadListener;
import net.m3u8.utils.Constant;

import java.io.File;
import java.util.HashMap;

public class video_down {
    public static String m3u8Dir;
    private final String m3u8_url;
    private final String m3u8_name;
    private final HashMap<String, Object> headersMap;

    public video_down(String Authorization ,String UA, String m3u8_url, String m3u8Dir, String m3u8_name) {
        this.m3u8_url = m3u8_url;
        this.m3u8Dir = m3u8Dir;
        this.m3u8_name = m3u8_name;
        HashMap<String, Object> headersMap = new HashMap<>();
        headersMap.put("Authorization", Authorization);
        headersMap.put("User-Agent", UA);
        this.headersMap = headersMap;
    }

    public void start() {
        File file = new File(m3u8Dir);
        if (!file.exists()) {
            System.out.println(file.mkdirs()); // 创建文件根目录
        }
        M3u8DownloadFactory.M3u8Download m3u8Download = M3u8DownloadFactory.newInstance(m3u8_url);
        m3u8Download.setDir(m3u8Dir);
        m3u8Download.setFileName(m3u8_name);
        headersMap.remove("Host");
        headersMap.forEach((k, v) -> System.out.println(k + ":" + v));
        m3u8Download.addRequestHeaderMap(headersMap);
        m3u8Download.setLogLevel(Constant.NONE);
        //设置监听器间隔（单位：毫秒）
        m3u8Download.setInterval(3000L);
        //添加监听器
        m3u8Download.addListener(new DownloadListener() {
            @Override
            public void start() {
                System.out.println("开始下载！");
            }
            @Override
            public void process(String downloadUrl, int finished, int sum, float percent) {
                System.out.println("下载网址：" + downloadUrl + "\t已下载" + finished + "个\t一共" + sum + "个\t已完成" + percent + "%");
            }
            @Override
            public void speed(String speedPerSecond) {
//                System.out.println("下载速度："+speedPerSecond);
            }
            @Override
            public void end() {
                System.out.println("下载完毕");
            }
        });
        //开始下载
        m3u8Download.start();
    }
}
