package com.example.tao.hotel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.content.Intent;
public class MainActivity extends Activity implements OnClickListener {

    private String IP = "118.89.55.196";//云服务器ip;
    private int Port = 5500;//端口
    private Socket socket;
    private PrintWriter pw;
    private BufferedReader br;
    private EditText editText_name;
    private EditText editText_pwd;

    public String rxd;
    private Handler handler =new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x10:
                    Toast.makeText(getApplicationContext(), "服务器连接失败", Toast.LENGTH_SHORT).show();
                    break;
                case 0x11:
                    Toast.makeText(getApplicationContext(), "用户名或密码错误", Toast.LENGTH_SHORT).show();
                    break;
                case 0x12:
                    Toast.makeText(getApplicationContext(), "用户名已存在", Toast.LENGTH_SHORT).show();
                    break;
                case 0x13:
                    Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button_login = (Button) findViewById(R.id.login);
        button_login.setOnClickListener(this);
        Button button_register = (Button) findViewById(R.id.register);
        button_register.setOnClickListener(this);
        editText_name = (EditText) findViewById(R.id.name);
        editText_pwd = (EditText) findViewById(R.id.pwd);

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login:

                new Thread() {
                    public void run(){
                        try {
                            socket = new Socket(IP, Port);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (socket == null) {
                            Log.e("socket", "null");
                            handler.sendEmptyMessage(0x10);
                        }
                        else {
                            try {
                                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                pw = new PrintWriter(socket.getOutputStream());
                                if (pw != null && br != null) {
                                    //handler.sendEmptyMessage(0x10);
                                    Log.e("socket连接：", "成功");
                                    String str  = editText_name.getText().toString()+':'+editText_pwd.getText().toString();
                                    pw.println(str);
                                    pw.flush();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            int flag=1;
                            while (flag==1) {
                                try {
                                    while ((rxd = br.readLine()) != null) {
                                        Log.e("接收到：", rxd);
                                        if(rxd.equals("ok")){/*服务器返回ok*/
                                            Log.e("登录成功：", "ok");
                                            //socket.close();
                                            Intent intent =new Intent(MainActivity.this,HotelActivity.class);
                                            intent.putExtra("UserName", editText_name.getText().toString());
                                            MainActivity.this.startActivity(intent);
                                            MainActivity.this.finish();
                                            break;
                                        }else {/*服务器返回off*/
                                            Log.e("登录失败：","off");
                                            handler.sendEmptyMessage(0x11);
                                            //socket.close();
                                            flag=0;
                                            break;
                                        }

                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            Log.e("等待下次登录：","off");
                        }
                    };

                }.start();
                break;
            case R.id.register:
                new Thread() {
                    public void run(){
                        try {
                            socket = new Socket(IP, Port);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (socket == null) {
                            Log.e("socket", "null");
                            handler.sendEmptyMessage(0x10);
                        }
                        else {
                            try {
                                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                pw = new PrintWriter(socket.getOutputStream());
                                if (pw != null && br != null) {
                                    //handler.sendEmptyMessage(0x10);
                                    Log.e("socket连接：", "成功");
                                    String str;
                                    str = '@'+editText_name.getText().toString()+':'+editText_pwd.getText().toString();
                                    pw.println(str);
                                    pw.flush();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            int flag=1;
                            while (flag==1) {
                                try {
                                    while ((rxd = br.readLine()) != null) {
                                        Log.e("接收到：", rxd);
                                        if(rxd.equals("ok")){
                                            Log.e("注册成功：", "ok");
                                            handler.sendEmptyMessage(0x13);
                                            socket.close();
                                            flag=0;
                                            break;
                                        }
                                        else {
                                            Log.e("注册失败：","off");
                                            handler.sendEmptyMessage(0x12);
                                            socket.close();
                                            flag=0;
                                            break;
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            Log.e("等待下次注册：","off");
                        }
                    };

                }.start();

                break;
        }

    }
}
