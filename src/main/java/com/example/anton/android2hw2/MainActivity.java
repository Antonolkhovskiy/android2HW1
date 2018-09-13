package com.example.anton.android2hw2;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import java.util.concurrent.Semaphore;

public class MainActivity extends AppCompatActivity {
    private Button startButton, pauseButton, clearButton;
    private TextView leftDigit, rightDigit, status;
    private int left = 0, right = 0;
    private Handler handler, handler2, statusHandler;
    private boolean flag1 = true, flag2 = false, flag = true,
            flagStopThread1 = false, flagStopThread2 = false, flagStart = true;
    private Semaphore semaphore = new Semaphore(1);
    private Semaphore semaphore2 = new Semaphore(1);
    private Semaphore stopSemThread1 = new Semaphore(1);
    private Semaphore stopSemThread2 = new Semaphore(1);
    private final String START_TO_GO = "Press Start to Go!";
    private final String FIRST_WAITING = "First Thread Waits, Second Thread is Working!";
    private final String SECOND_WAITING = "First Thread is now Working, Second Thread is set to Zero!";
    private final String STOPPED = "Counter is Stopped";
    private Thread thread1, thread2;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button)findViewById(R.id.buttonStart);
        pauseButton = (Button)findViewById(R.id.buttonPause);
        clearButton = (Button)findViewById(R.id.buttonClear);
        leftDigit = (TextView)findViewById(R.id.textViewLeft);
        rightDigit = (TextView)findViewById(R.id.textViewRight);
        status = (TextView)findViewById(R.id.statusView);


        handler = new Handler() {
           @Override
            public void handleMessage(Message msg) {

                leftDigit.setText(String.valueOf(msg.obj));
            }
        };
        handler2 = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                rightDigit.setText(String.valueOf(msg.obj));

            }
        };
        statusHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                status.setText(String.valueOf(msg.obj));

            }
        };

        setStatus(START_TO_GO);

        startButton.setOnClickListener((View)->{
            if(flag){
                if(flagStart) {
                    flagStart = false;
                    thread1 = getThread1();
                    thread2 = getThread2();
                    flagStopThread1 = false;
                    flagStopThread2 = false;
                    flag2 = false;
                    flag = true;
                    flag1 = true;

                    if(semaphore2.availablePermits() < 1) semaphore2.release();
                    if(semaphore.availablePermits() < 1) semaphore.release();
                    thread1.start();
                    thread2.start();
                }
            }else{
                pauseCounter();
            }



        });

        pauseButton.setOnClickListener((View) -> {
            String leftDigitInString = String.valueOf(left);
            String rightDigitInString = String.valueOf(right);
            String totalValue = leftDigitInString + rightDigitInString;
            if(totalValue.equals("00")){
                clearProc();
            }else {
                pauseCounter();
            }
        });

        clearButton.setOnClickListener((View) -> {
            clearProc();

        });




    }

    public void pauseCounter() {
        if (flag == false) {
            flag = true;
            flagStopThread1 = false;
            flagStopThread2 = false;
            if(stopSemThread1.availablePermits() < 1) stopSemThread1.release();
            if(stopSemThread2.availablePermits() < 1) stopSemThread2.release();
        } else {
            flag = false;
            flagStopThread1 = true;
            flagStopThread2 = true;
        }

    }
    public synchronized void tryIncRightDigit(){
        right++;
        if(right == 10){
            right = 0;
            flag2 = true;
            flag1 = false;
        }
        Message msg = new Message();
        msg.obj = String.valueOf(right);
        handler2.sendMessage(msg);
    }
    public synchronized void tryIncLeftDigit(){
        left++;
        Message msg = new Message();
        msg.obj = String.valueOf(left);
        handler.sendMessage(msg);
    }

    public boolean isFinished(){
        String leftDigitInString = String.valueOf(left);
        String rightDigitInString = String.valueOf(right);
        String totalValue = leftDigitInString + rightDigitInString;
        if(totalValue.equals("99")){
            setStatus(STOPPED);
            return true;
        }
        return false;
    }

    public void clearCounter(){
        left = 0;
        right = 0;

    }

    public void setStatus(String status){
        Message msg = new Message();
        msg.obj = String.valueOf(status);
        statusHandler.sendMessage(msg);
    }

    public Thread getThread1(){
        Thread thread1 = new Thread(() -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            while(true) {
                if(flagStopThread1){
                    try {
                        stopSemThread1.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                setStatus(SECOND_WAITING);
                if (isFinished()) {
                    Thread.currentThread().interrupt();
                    break;
                }
                tryIncLeftDigit();
                semaphore2.release();
                flag2 = false;
                flag1 = true;
            }
        });

        return thread1;
    }

    public Thread getThread2(){
        Thread thread2 = new Thread(()->{
            while (true) {
                if(flagStopThread2) {
                    try {
                        stopSemThread2.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                try {
                    if(flag2) semaphore2.acquire();
                    semaphore2.acquire();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                setStatus(FIRST_WAITING);
                tryIncRightDigit();

                if(!flag1) {
                    semaphore.release();
                }
                if(isFinished()){
                    Thread.currentThread().interrupt();
                    break;
                }
                semaphore2.release();
            }

        });

        return thread2;
    }

    public void clearProc(){
        Thread thread = new Thread(()->{
            flag = true;
            flagStart = true;
            flagStopThread1 = true;
            flagStopThread2 = true;
            if(thread1 != null){
                thread1.interrupt();
            }
            if(thread2 != null){
                thread2.interrupt();
            }
            clearCounter();
            Message msg = new Message();
            Message msg2 = new Message();
            msg.obj = String.valueOf(0);
            msg2.obj = String.valueOf(0);
            handler.sendMessage(msg);
            handler2.sendMessage(msg2);
            setStatus(START_TO_GO);
            flagStopThread1 = false;
            flagStopThread2 = false;
        });
        thread.start();
    }

}
