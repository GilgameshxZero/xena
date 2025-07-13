package com.gilgamesh.multithreading;

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
		this.task.cancel();
		if (delayMs > 0) {
			this.isAwaiting = true;
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
		} else if (delayMs == 0) {
			this.isAwaiting = false;
			callback.onRun();
		} else if (delayMs < 0) {
			this.isAwaiting = true;
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
