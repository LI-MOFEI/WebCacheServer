import java.io.*;
import java.util.TimerTask;

public class ScheduledUpdate extends TimerTask {//缓存定时更新程序
    WebLink link = null;//和目标服务器的Web连接
    File editingFile;//待修改的文件
    @Override
    public void run() {
        //获取缓存的目录，打开缓存获取列表
        File cacheDir = new File("cache");
        File[] caches = cacheDir.listFiles();
        if (caches != null) {//有缓存时进入更新程序
            for(File cache:caches){//遍历缓存的文件
                if(!cache.isDirectory()) {//保证非目录的情况下打开文件
                    //读取文件名，替换斜杠
                    String linkName = cache.getName().replace("#","/");
                    String[] split = linkName.split("/", 2);//解析主机名和文件名
                    String host = split[0];
                    String fileUrl = "/" + split[1];
                    String message = "GET " + fileUrl + " HTTP/1.1\r\n" + "Host: " + host + "\r\n";//解析出相应的HTTP请求
                    try {
                        link = new WebLink(host);//建立连接
                        link.ConditionalGet(message, cache);//发送相应的条件GET
                        String state = link.bufferedReader.readLine();//读取状态码
                        editingFile = cache;//确认需要修改的文件
                        synchronized (WebServer.class){//文件修改的互斥，加锁
                            if (!state.contains("304 Not Modified")) {//在不是304的情况下更新缓存
                                saveFile();
                            }
                        }
                        link.close();//关闭连接
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
    private void saveFile()  throws IOException {
        String info;
        BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(editingFile)));
        //从连接缓存区接受信息，越过文件头
        do {
            info = link.bufferedReader.readLine();
            //写入上次修改时间，越过响应头
            if (info.contains("Last-Modified"))
                fileWriter.write(info+"\r\n");
        } while (!info.isEmpty());
        //写入获取的文件
        while ((info = link.bufferedReader.readLine()) != null) {
            fileWriter.write(info+"\r\n");
        }
        fileWriter.close();
    }
}
