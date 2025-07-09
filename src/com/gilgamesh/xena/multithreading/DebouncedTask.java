package com.gilgamesh.xena.multithreading;

import java.util.Timer;
import java.util.TimerTask;

// A timer task which may be debounced and reset.
public class DebouncedTask {
	static public abstract class Callback {
		public abstract void onRun();

		public void onDebounce() {
		}
	}

	private Callback callback;
	private boolean isAwaiting = false;

	private TimerTask task = new TimerTask() {
		@Override
		public void run() {
		}
	};

	public DebouncedTask(Callback callback) {
		this.callback = callback;
	}

	// Negative delay means a task that is scheduled in infinity time, but is
	// always awaiting.
	public void debounce(int delayMs) {
		this.isAwaiting = true;
		this.task.cancel();
		if (delayMs >= 0) {
			// Split into a few steps in case of multi-threading errors.
			TimerTask newTask = new TimerTask() {
				@Override
				public void run() {
					isAwaiting = false;
					callback.onRun();
				}
			};
			new Timer().schedule(newTask, delayMs);
			this.task = newTask;
		}
		this.callback.onDebounce();
	}

	public boolean cancel() {
		return this.task.cancel();
	}

	public boolean isAwaiting() {
		return this.isAwaiting;
	}
}
