import java.io.*;
import java.net.Socket;

public class WebLink {
    private final Socket socket;//和Web服务器的socket连接
    public BufferedReader bufferedReader;//连接的输入流
    private BufferedWriter bufferedWriter;//连接的输出流

    public WebLink(String host) throws IOException {
        //通过目标主机名建立和请求的web服务器的连接
        socket = new Socket(host, 80);
        //使用缓存区存放收到的消息
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void MessageSend(String message) throws IOException {
        //准备写数据缓存区，发送一条消息
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        bufferedWriter.write(message);
        bufferedWriter.flush();
    }

    public void ConditionalGet(String message, File cache) throws IOException {
        //条件Get方法
        if (!cache.exists()||cache.length()==0) {
            //无缓存文件检测到或缓存文件无内容时,发起普通的web请求
            MessageSend(message+"\r\n");
            return;
        }
        //读取缓存的上次修改时间，即保存的 Last-Modified
        BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(cache)));
        String[] lastModified = file.readLine().split(":",2);
        if (lastModified.length<2){
            //缓存文件保存时间丢失时,发起普通的web请求
            MessageSend(message+"\r\n");
            return;
        }
        //发起条件请求
        MessageSend(message+"If-modified-since: "+lastModified[1]+"\r\n\r\n");
    }

    //连接的关闭
    public void close() throws IOException {
        bufferedWriter.close();
        bufferedReader.close();
        socket.close();
    }
}