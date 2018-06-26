package com.example.tao.hotel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Hashtable;

/**
 * Created by tao on 2017/12/17.
 */
public class HotelActivity extends Activity implements OnClickListener{

    private String IP = "118.89.55.196";//"192.168.1.100";
    private int Port = 5000;
    private Socket socket;
    private PrintWriter pw;
    private BufferedReader br;
    String UserName;
    String key;
    ImageView imageView;
    Button button_key;

    private Handler handler =new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x20:
                    Toast.makeText(getApplicationContext(), "服务器连接中断", Toast.LENGTH_SHORT).show();
                    break;
                case 0x21:
                    Toast.makeText(getApplicationContext(), "登陆成功", Toast.LENGTH_SHORT).show();
                    break;
                case 0x22:
                    imageView.setVisibility(View.VISIBLE);
                    Log.e("二维码密钥：", key);
                    String fileName ="qr_" + System.currentTimeMillis() + ".jpg";
                    File file = getFileRoot(fileName);
                    Bitmap bitmap=createQRImage(key, imageView,200,200);
                    saveImage(HotelActivity.this, bitmap, file, fileName);
                    //button_key.setEnabled(true);
                    break;
                case 0x23:
                    imageView.setVisibility(View.GONE);
                    button_key.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "注销成功", Toast.LENGTH_SHORT).show();
                    break;
                case 0x24:
                    Toast.makeText(getApplicationContext(), "注销失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hotel);

        UserName = this.getIntent().getStringExtra("UserName");

        imageView = (ImageView) findViewById(R.id.img_qr);
        button_key = (Button) findViewById(R.id.key);
        button_key.setOnClickListener(this);
        Button button_delete = (Button) findViewById(R.id.delete);
        button_delete.setOnClickListener(this);

        new Thread() {
            public void run(){
                boolean flag=true;
                while (true){
                    try {
                        socket = new Socket(IP, Port);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (socket == null) {
                        Log.e("socket", "false");
                        if(flag) {
                            flag=false;
                            handler.sendEmptyMessage(0x20);
                        }
                    }
                    else {
                        try {
                            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            pw = new PrintWriter(socket.getOutputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (pw != null && br != null) {
                            handler.sendEmptyMessage(0x21);
                            Log.e("socket", "success");
                            break;
                        }
                    }
                }
            };
        }.start();

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.key:
                button_key.setEnabled(false);
                new Thread() {
                    boolean key_flag=true;
                    public void run() {
                        String str = '$'+ UserName;
                        Log.e("明钥：", str);
                        pw.println(str);
                        pw.flush();
                        while (key_flag) {
                            try {
                                while ((key = br.readLine()) != null) {
                                    Log.e("接收到：", key);
                                    key_flag=false;
                                    break;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        handler.sendEmptyMessage(0x22);
                        Log.e("二维码", "success");
                    }
                }.start();
                break;

            case R.id.delete:
                new Thread() {
                    boolean key_flag=true;
                    String Del;
                    public void run() {
                        String str = '#'+ UserName;
                        Log.e("删除：", str);
                        pw.println(str);
                        pw.flush();
                        while (key_flag) {
                            try {
                                while ((Del = br.readLine()) != null) {
                                    Log.e("接收到：", Del);
                                    if(Del.equals("ok")||(Del.contains("ok"))) {
                                        Log.e("删除成功：", "ok");
                                        handler.sendEmptyMessage(0x23);
                                    }
                                    else {
                                        Log.e("删除失败：", "off");
                                        handler.sendEmptyMessage(0x24);
                                    }
                                    key_flag=false;
                                    break;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
                break;
        }
    }
    //文件存储根目录
    private File getFileRoot(String fileName) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File appDir = new File(Environment.getExternalStorageDirectory(), "Boohee");
            if (!appDir.exists()) {
                appDir.mkdir();
            }
            File file = new File(appDir, fileName);
            Log.e("路径：", file.toString());
            return file;
        }
        return null;
    }
    //要转换的地址或字符串,可以是中文
    public static Bitmap createQRImage(String url, ImageView sweepIV, int QR_WIDTH, int QR_HEIGHT ) {
        try {//判断URL合法性
            if (url == null || "".equals(url) || url.length() < 1) {
                return null;
            }
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            //图像数据转换，使用了矩阵转换
            BitMatrix bitMatrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);
            int[] pixels = new int[QR_WIDTH * QR_HEIGHT];
            //下面这里按照二维码的算法，逐个生成二维码的图片，
            //两个for循环是图片横列扫描的结果
            for (int y = 0; y < QR_HEIGHT; y++) {
                for (int x = 0; x < QR_WIDTH; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * QR_WIDTH + x] = 0xff000000;
                    }
                    else {
                        pixels[y * QR_HEIGHT + x] = 0xffffffff;
                    }
                }
            }//生成二维码图片的格式，使用ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(QR_WIDTH, QR_HEIGHT, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, QR_WIDTH, 0, 0, QR_WIDTH, QR_HEIGHT);
            //显示到一个ImageView上面
            sweepIV.setImageBitmap(bitmap);
            return bitmap;
            }
            catch (WriterException e) {
                e.printStackTrace();
            }
        return null;
    }
    //保存图片
    public static void saveImage(Context context,Bitmap bitmap,File filePath,String fileName){
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {// 把文件插入到系统图库
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    filePath.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + "/sdcard/Boohee/image.jpg")));/*二维码存储路径*/
    }
}


