package com.dq.mylibrary.tcp;



public class TcpLoopThread extends Thread {

    private boolean canLoop = true;

    public void cancelLoop() {
        this.canLoop = false;
    }


    public boolean canLoop() {
        return canLoop;
    }


}
