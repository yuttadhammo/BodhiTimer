package org.yuttadhammo.BodhiTimer.Service;

import java.util.ArrayList;


public class TimerList {

    public static class Timer {
        public int duration;
        public String uri;
        public SessionType sessionType;

        public Timer(int mduration, String muri) {
            super();
            duration = mduration;
            uri = muri;
        };
    }

    public ArrayList<Timer> timers;

    public TimerList(String advTimeString) {
        timers = timeStringToList(advTimeString);
    }

    public String getString() {
        return listToTimeString(timers);
    }

    public static  ArrayList<Timer> timeStringToList(String advTimeString) {
        ArrayList<Timer> list = new ArrayList<Timer>();

        String[] advTime = advTimeString.split("\\^");

        for (int i = 0; i < advTime.length; i++) {
            //  advTime[n] will be of format timeInMs#pathToSound
            String[] thisAdvTime = advTime[i].split("#");
            int duration = Integer.parseInt(thisAdvTime[0]);
            Timer timer = new Timer(duration, thisAdvTime[1]);
            list.add(timer);
        }

        return list;
    }

    public static String listToTimeString (ArrayList<Timer> list ) {
        String ret = "";

        for (Timer timer: list) {
            ret += timer.duration + "#" + timer.uri + "#" +  timer.sessionType + "^";
        }

        return ret;
    }

}

