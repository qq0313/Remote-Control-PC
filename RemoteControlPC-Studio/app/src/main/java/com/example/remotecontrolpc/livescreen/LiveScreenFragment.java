package com.example.remotecontrolpc.livescreen;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.example.remotecontrolpc.MainActivity;
import com.example.remotecontrolpc.R;

import java.util.Timer;
import java.util.TimerTask;

public class LiveScreenFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private int xCord, yCord, disY;
    boolean mouseMoved = false, moultiTouch = false;
    private ImageView screenshotImageView;
    private Timer timer;
    private int screenshotImageViewX, screenshotImageViewY;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_live_screen, container, false);
        screenshotImageView = (ImageView) rootView.findViewById(R.id.screenshotImageView);
        ViewTreeObserver vto = screenshotImageView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                screenshotImageViewX = screenshotImageView.getHeight();
                screenshotImageViewY = screenshotImageView.getWidth();
                ViewTreeObserver obs = screenshotImageView.getViewTreeObserver();
                obs.removeGlobalOnLayoutListener(this);
            }
        });
        screenshotImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (MainActivity.clientSocket != null) {
                    switch(event.getAction() & MotionEvent.ACTION_MASK){
                        case MotionEvent.ACTION_DOWN:
                            xCord = screenshotImageViewX - (int) event.getY();
                            yCord = (int) event.getX();
                            MainActivity.sendMessageToServer("MOUSE_MOVE_LIVE");
                            //send mouse movement to server by adjusting coordinates
                            MainActivity.sendMessageToServer((float) xCord / screenshotImageViewX);
                            MainActivity.sendMessageToServer((float) yCord / screenshotImageViewY);
                            mouseMoved = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if(moultiTouch == false) {
                                xCord = screenshotImageViewX - (int) event.getY();
                                yCord = (int) event.getX();
                                MainActivity.sendMessageToServer("MOUSE_MOVE_LIVE");
                                //send mouse movement to server by adjusting coordinates
                                MainActivity.sendMessageToServer((float) xCord / screenshotImageViewX);
                                MainActivity.sendMessageToServer((float) yCord / screenshotImageViewY);
                            }
                            mouseMoved=true;
                            break;
                        case MotionEvent.ACTION_UP:
                            //consider a tap only if user did not move mouse after ACTION_DOWN
                            if(!mouseMoved){
                                MainActivity.sendMessageToServer("LEFT_CLICK");
                                delayedUpdateScreenshot();
                            }
                            break;
                    }
                }
                return true;
            }
        });
        timer = new Timer();
        updateScreenshot();
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(
                ARG_SECTION_NUMBER));
    }

    private void updateScreenshot() {
        if (MainActivity.clientSocket != null) {
            MainActivity.sendMessageToServer("SCREENSHOT_REQUEST");
            new UpdateScreenshot() {
                @Override
                public void receiveData(Object result) {
                    String path = (String) result;
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(-90);
                    Bitmap rotated = Bitmap.createBitmap(bitmap ,0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    screenshotImageView.setImageBitmap(rotated);
                }
            }.execute();
        }
    }

    private void delayedUpdateScreenshot() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateScreenshot();
            }
        }, 500);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timer.cancel();
        timer.purge();
    }
}
