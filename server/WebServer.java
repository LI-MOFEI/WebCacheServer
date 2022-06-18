import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;

public class WebServer {

    public static void main(String[] args){
        try {
            //连接建立前交互设置代理服务器端口和缓存更新时间
            int port = 0;
            int timeToUpdate;
            Scanner scan = new Scanner(System.in);
            System.out.println("Please set the time to update the caches");
            System.out.println("h for hours, m for min, only number for seconds");
            String time = scan.next();
            if (time.contains("h")){ //时间设置，单位小时
                time = time.replace("h","");
                timeToUpdate = Integer.parseInt(time)*3600*1000;
            } else if (time.contains("m")){ //时间设置，单位分钟
                time = time.replace("m", "");
                timeToUpdate = Integer.parseInt(time)*60*1000;
            }else { //时间设置，单位秒
                timeToUpdate = Integer.parseInt(time)*1000;
            }
            while (port>65536||port<1024){ //获取正确的端口
                System.out.println("Please input the port where the server run");
                port = scan.nextInt();
                if (port<1024)
                    System.out.println("Port can not be a Well Known Port");
            }
            //没有缓存时创建缓存
            File f = new File("cache");
            if (!f.exists()){
                boolean mkdir = f.mkdir();
            }
            //创建代理服务器socket
            ServerSocket serverSocket = new ServerSocket(port);

            //创建和客户端握手的socket
            Socket socket;

            //建立更新程序
            Timer update = new Timer(true);
            //建立更新的进程
            ScheduledUpdate task = new ScheduledUpdate();
            //安排更新时间和任务，更新在更新间隔除以16的时间后开始
            update.schedule(task, timeToUpdate>>>4, timeToUpdate);
            //循环监听客户端，等待连接
            while (true){
                //监听到来自客户端的socket
                socket = serverSocket.accept();
                if (socket.isConnected()){
                    //开始启动线程
                    ServerThread thread = new ServerThread(socket);
                    thread.start();
                }
            }
        }catch(Exception e){
            //异常反馈
            e.printStackTrace();
        }
    }
}
