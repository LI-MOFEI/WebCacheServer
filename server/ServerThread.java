import java.io.*;
import java.net.Socket;

public class ServerThread extends Thread{
    private final Socket socket;//和代理客户端的连接
    private String message = "";//待发送的HTTP请求
    private File file;//缓存文件
    private BufferedReader bufferedReader = null;//socket输入流
    private PrintWriter printWriter = null;//socket输出流
    private WebLink link = null;//和Web服务器的连接对象
    public ServerThread(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run(){
        try{
            //建立输入和输出流
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream());
            //获取客户端的信息，即客户端构建的两行HTTP请求
            message += bufferedReader.readLine() + "\r\n";
            message += bufferedReader.readLine() + "\r\n";
            //用空白字符匹配的正则表达式\\s分割请求消息
            String[] split = message.split("\\s+");
            //统一的文件存储，cache目录下以url中"#"代替"/"作为文件名，下方对消息进行了处理以提取url
            file = new File("cache/"+split[4]+split[1].replace("/","#"));

            //和服务器建立连接
            link = new WebLink(split[4]);

            //发起一个条件Get请求
            link.ConditionalGet(message, file);

            //返回文件
            String state = link.bufferedReader.readLine();
            printWriter.println(state);//打印状态
            printWriter.flush();
            synchronized (WebServer.class){//对文件读写部分设置互斥，加锁
                if (state.contains("304 Not Modified")) {//304未修改时直接读取缓存并发送
                    readSend();
                } else {//修改时，保存文件的同时进行发送
                    saveSend();
                }
            }
        }catch (Exception e){
            e.printStackTrace();//异常捕捉
        }finally {
            try{
                //关闭连接
                bufferedReader.close();
                printWriter.close();
                link.close();
                socket.close();
            }catch(IOException e){
                e.printStackTrace();//异常捕捉
            }
        }
    }

    private void readSend() throws IOException {
        String info;//打开要读取的文件，建立写入流
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        //从连接缓存区接受信息
        do {
            //读取消息头，返回到客户端
            info = link.bufferedReader.readLine();
            printWriter.println(info);
            printWriter.flush();
        } while (!info.isEmpty());
        //从文件缓存区接受信息，越过修改时间
        fileReader.readLine();
        while ((info = fileReader.readLine()) != null) {
            //发回消息体
            printWriter.println(info);
            printWriter.flush();
        }
        fileReader.close();
    }
    private void saveSend() throws IOException {
        String info;//打开要读取的文件，建立写入流
        BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        //从连接缓存区接受信息
        do {
            //读取消息头，返回到客户端
            info = link.bufferedReader.readLine();
            printWriter.println(info);
            printWriter.flush();
            //读取保存上次修改时间
            if (info.contains("Last-Modified"))
                fileWriter.write(info+"\r\n");
        } while (!info.isEmpty());//读取到空行时，说明消息头结束
        while ((info = link.bufferedReader.readLine()) != null) {
            //写入获取的文件
            fileWriter.write(info+"\r\n");
            //发回消息体
            printWriter.println(info);
            printWriter.flush();
        }
        fileWriter.close();
    }
}