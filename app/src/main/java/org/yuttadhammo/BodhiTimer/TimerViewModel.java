package org.yuttadhammo.BodhiTimer;

import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.yuttadhammo.BodhiTimer.Service.AlarmTaskManager;

import static org.yuttadhammo.BodhiTimer.Service.TimerState.RUNNING;

public class TimerViewModel extends ViewModel {


    private MutableLiveData<String> mainLabelText;
    private MutableLiveData<String> secondaryLabelText;
    private AlarmTaskManager mAlarmTaskManager;

    public MutableLiveData<String> getMainLabel() {
        if (mainLabelText == null) {
            mainLabelText = new MutableLiveData<String>();
            loadUsers();
        }
        return mainLabelText;
    }

    private void loadUsers() {
        // Do an asynchronous operation to fetch users.
    }

//    private void onCreate() {
//
//        // Setup a new AlarmTaskManager
//        mAlarmTaskManager = new AlarmTaskManager(this);
//
//        setupListener();
//
//        tts = new TextToSpeech(this, null);
//    }


//    private void setupListener() {
//
//        mAlarmTaskManager.setListener(new AlarmTaskManager.AlarmTaskListener() {
//            @Override
//            public void onEnterState(int state) {
//                enterState(state);
//            }
//
//            @Override
//            public void onObjectReady(String title) {
//                //AlarmTaskManager
//            }
//
//            @Override
//            public void onDataLoaded(String data) {
//                // Code to handle data loaded from network
//                // Use the data here!
//            }
//
//            @Override
//            public void onUpdateTime(int elapsed, int duration) {
//                updateInterfaceWithTime(elapsed, duration);
//            }
//
//            @Override
//            public void onEndTimers() {
//                if (prefs.getBoolean("AutoRestart", false)) {
//                    Log.i(TAG, "AUTO RESTART");
//                    mAlarmTaskManager.stopAlarmsAndTicker();
//                    startAdvancedAlarm(retrieveTimerList());
//                    enterState(RUNNING);
//                }
//            }
//        });
//    }




}
