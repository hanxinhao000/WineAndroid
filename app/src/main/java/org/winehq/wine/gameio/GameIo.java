package org.winehq.wine.gameio;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 游戏传输事件
 */
public class GameIo {


    static Context mContext;
    //目录
    //static File data = new File("/data/data/org.winehq.wine/files/prefix/dosdevices/c:/users/u0_a583/Desktop/");
    static File data = new File("/data/data/org.winehq.wine/files/prefix/dosdevices/c:/");
    //内存卡目录
    static File sd = new File(Environment.getExternalStorageDirectory() + "/xinhaowine/");

    //传输默认游戏

    public static void sefGame(Context context) {
        init();
        mContext = context;
        File file = new File(sd, "360zip.exe");
        if (file.exists()) {
            startIoRun("360zip.exe");
        } else {
            Toast.makeText(mContext, "文件不存在!", Toast.LENGTH_SHORT).show();
        }


    }

    //初始化目录
    private static void init() {

        if (!sd.exists()) {
            sd.mkdirs();
        }

    }

    //java中执行命令
    public static void execCommand(String command) throws IOException {


        Runtime runtime = Runtime.getRuntime();

        Process proc = runtime.exec(command);
        try {

            if (proc.waitFor() != 0) {

                System.err.println("exit value = " + proc.exitValue());

            }

        } catch (InterruptedException e) {

            System.err.println(e);

        }

    }


    //开始传输
    private static void startIoRun(String fileName) {

        try {
            File file = new File(sd, fileName);

            FileInputStream fileInputStream = new FileInputStream(file);

            FileOutputStream fileOutputStream = new FileOutputStream(new File(data, fileName));

            int i = 0;

            byte[] b = new byte[1024];

            while ((i = fileInputStream.read(b)) != -1) {

                fileOutputStream.write(b, 0, i);

            }
            fileInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();

            Toast.makeText(mContext, "文件传入完成!", Toast.LENGTH_SHORT).show();

            execCommand("/system/bin/chmod 777 " + new File(data, fileName).getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(mContext, "文件传入出错!" + e.toString(), Toast.LENGTH_SHORT).show();
        }


    }
}
