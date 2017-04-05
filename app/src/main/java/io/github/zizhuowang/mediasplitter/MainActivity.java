package io.github.zizhuowang.mediasplitter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int FILE = 0;
    private static final int UPDATE = 1;
    Button openBtn,startBtn,endBtn,seekBack,playPause,seekForward,go;
    EditText directory,start,duration;
    TextView output,current,total;
    SeekBar seekBar;
    MediaPlayer player;
    int time1,time2;
    Thread time;
    Uri uri;
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == FILE){
                MainActivity.this.directory.setText(uri.getPath().toString());

                player = MediaPlayer.create(MainActivity.this,uri);
                seekBar.setMax(player.getDuration());
                total.setText(getTime(player.getDuration()));
                time = new Thread(){
                    public void run(){
                        while (true){
                            boolean isPlaying = player.isPlaying();
                            while(isPlaying){
                                int pos = player.getCurrentPosition();
                                seekBar.setProgress(pos);

                                Message m = new Message();
                                m.what = UPDATE;
                                handler.sendMessage(m);
                                try {
                                    Thread.sleep(100);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                };
                time.start();

                MainActivity.this.total.setText(getTime(player.getDuration()));
//                player.start();
            }
            if(msg.what == UPDATE){
                current.setText(getTime(player.getCurrentPosition()));
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openBtn = (Button) findViewById(R.id.openBtn);
        startBtn = (Button) findViewById(R.id.startBtn);
        endBtn = (Button) findViewById(R.id.endBtn);
        seekBack = (Button) findViewById(R.id.seekBack);
        playPause = (Button) findViewById(R.id.playPause);
        seekForward = (Button) findViewById(R.id.seekForward);
        go = (Button) findViewById(R.id.go);
        directory = (EditText) findViewById(R.id.directory);
        start = (EditText) findViewById(R.id.start);
        duration = (EditText) findViewById(R.id.duration);
        output = (TextView) findViewById(R.id.output);
        current = (TextView) findViewById(R.id.current);
        total = (TextView) findViewById(R.id.total);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        player = new MediaPlayer();

        final FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onFailure() {}

                @Override
                public void onSuccess() {}

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
            e.printStackTrace();
        }

        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(intent,"选择文件"),1);
                }catch (ActivityNotFoundException e){
                    Toast.makeText(MainActivity.this, "亲，木有文件管理器啊-_-!!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time1 = player.getCurrentPosition();
                start.setText(getTime(player.getCurrentPosition()));
            }
        });

        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time2 = player.getCurrentPosition();
                duration.setText(getTime(time2-time1));
            }
        });

//        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//
//            }
//        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(directory.getText().length()!=0){
                    if(!player.isPlaying()){
                        player.start();
                        playPause.setText("暂停");
                        player.setLooping(true);
                    }else {
                        player.pause();
                        playPause.setText("播放");
                    }
                }
            }
        });

        seekBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.seekTo(player.getCurrentPosition()-5000);
            }
        });

        seekForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.seekTo(player.getCurrentPosition()+5000);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(directory.getText().length()!=0
                        &&start.getText().length()!=0
                        &&duration.getText().length()!=0
                        ){
                    File dir = new File("/storage/emulated/0/MediaSplitter/");
                    File file = new File("/storage/emulated/0/MediaSplitter/output.mp3");
                    try{
                        if(!dir.exists()){
                            dir.mkdirs();
                        }
                        if(file.exists()){
                            file.delete();
                        }
//                        file.createNewFile();
                    }catch (Exception e){
                        Toast.makeText(MainActivity.this, "请联系作者\nwjwzzc@163.com", Toast.LENGTH_SHORT).show();
                    }
                    String rawCMD = "-ss "+start.getText()+" -t "+duration.getText()
                            + " -i "+directory.getText()
                            + " -acodec mp3 /storage/emulated/0/MediaSplitter/output.mp3";
                    String[] cmd = rawCMD.split(" ");
                    if(cmd.length!=0){
                        try{
                            ffmpeg.execute(cmd,new ExecuteBinaryResponseHandler(){
                                @Override
                                public void onStart() {}

                                @Override
                                public void onProgress(String message) {
                                    output.append(message+"\n");
                                }

                                @Override
                                public void onFailure(String message) {
                                    output.append(message+"\n");
                                }

                                @Override
                                public void onSuccess(String message) {
                                    output.append(message+"\n");
                                }

                                @Override
                                public void onFinish() {}
                            });
                        }catch (FFmpegCommandAlreadyRunningException e){
                            Toast.makeText(MainActivity.this,"一个一个来哦！", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode== Activity.RESULT_OK){
            if(requestCode==1){
                uri = data.getData();
                Toast.makeText(MainActivity.this,"已选择"+uri.getPath().toString(), Toast.LENGTH_SHORT).show();
                Message msg = new Message();
                msg.what = FILE;
                handler.sendMessage(msg);
            }
        }
    }
    public String getTime(int t){
        Date date = new Date(t);
        if(date.getMinutes()>=10&&date.getSeconds()>=10){
            return new String(date.getMinutes() + ":" + date.getSeconds());
        }else if(date.getMinutes()>=10&&date.getSeconds()<10){
            return new String(date.getMinutes() + ":0" + date.getSeconds());
        }else if(date.getMinutes()<10&&date.getSeconds()>=10){
            return new String("0"+date.getMinutes() + ":" + date.getSeconds());
        }else if(date.getMinutes()<10&&date.getSeconds()<10){
            return new String("0"+date.getMinutes() + ":0" + date.getSeconds());
        }
        return "";
    }
}
