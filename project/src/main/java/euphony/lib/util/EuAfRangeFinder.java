package euphony.lib.util;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import euphony.lib.receiver.EuFreqObject;
import euphony.lib.receiver.PositionDetector;
import euphony.lib.transmitter.EuFreqGenerator;
import euphony.lib.transmitter.EuPlayer;

public class EuAfRangeFinder {
	
	EuFreqGenerator mEuFreqGenerator;
	EuPlayer mEuPlayer;
	EuFreqObject mEuObject;
	GettingFQRunner mFQRunner;
	EuOption mOption;

	private static final int RX_FREQ = 2;
	private static final int RX_REF = 3;
	
	public EuAfRangeFinder() {
		mOption = new EuOption();
		ClassInitializer();
	}

	public EuAfRangeFinder(EuOption option)
	{
		mOption = option;
		ClassInitializer();
	}
	
	private void ClassInitializer(){
		mEuFreqGenerator = new EuFreqGenerator(mOption);
		mEuPlayer = new EuPlayer();
		mEuObject = new EuFreqObject(mOption);
	}
	
	
	PositionDetector mPositionDetector;
	public void setPositionDetector(PositionDetector detector) {
		this.mPositionDetector = detector;
	}

	Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch(msg.what)
			{
			case RX_FREQ:
				mSuitableFreq = (Integer)msg.obj;
				mPositionDetector.detectFq(mSuitableFreq);

				mFQRunner = null;
				break;
			}
		}

	};
	
	int mSuitableFreq;

	public void findSuitableChannel()
	{
		ClassInitializer();
		mFQRunner = new GettingFQRunner();
		mFQRunner.start();
	}
	public void stopFindingChannel()
	{
		while(mFQRunner.isAlive())
		{
			try {
				mFQRunner.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		mFQRunner = null;
	}
	
	public
	
	class GettingFQRunner extends Thread {

		int ref = 0;
		int current_freq = 21000;
		
		@Override
		public void run() {

			do
			{
				mEuPlayer.setSource(mEuFreqGenerator.applyCrossFade(mEuFreqGenerator.makeStaticFrequency(current_freq, 0), EuFreqGenerator.CrossfadeType.BOTH));
				try{	mEuPlayer.Play();	}
				catch(IllegalStateException e)
				{	
					mEuPlayer = new EuPlayer();
					Log.e("PLAY", e.getMessage().toString());	
					continue;
				}
				int cnt = 0;
				do {
					mEuObject.processFFT();
					ref = mEuObject.detectFreq(current_freq);
				}while(ref <= 250 && cnt++ < 50);
				
				Log.i("RANGE", ref + " : " + current_freq);
				current_freq -= mOption.getDataInterval();
				try { mEuPlayer.Stop(); }
				catch(IllegalStateException e)
				{	
					mEuPlayer = new EuPlayer();
					Log.e("PLAY", e.getMessage().toString());	
					continue;
				}
			}while(ref <= 250 && current_freq > 16500);
			
			current_freq += mOption.getDataInterval();
			
			Message msg = mHandler.obtainMessage();
			msg.what = RX_FREQ;
			msg.obj = current_freq;
			mHandler.sendMessage(msg);
			
			mEuObject.destroyFFT();
		}
	};

}
