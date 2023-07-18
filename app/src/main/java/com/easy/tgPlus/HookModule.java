package com.easy.tgPlus;

public class HookModule {

    public static final String TAG = "HookModule";

	//是否加载成功
	private boolean loadSuccess;
	//是否启用
	private boolean switchOn;

	public HookModule.Impl impl;

	public HookModule(HookModule.Impl impl) throws Throwable {
		this.impl = impl;
		loadSuccess = impl.init();
	}

	public void setSwitchOn(boolean switchOn) {
		if (this.switchOn != switchOn) {
			impl.setSwitchOn(switchOn);
			this.switchOn = switchOn;
		}
	}

	public boolean isSwitchOn() {
		return switchOn;
	}

	public boolean isLoadSuccess() {
		return loadSuccess;
	}

	public interface Impl {
		public abstract String getModuleId();
		public abstract String getModuleName();
		public abstract String getModuleDoc();
		public abstract boolean init() throws Throwable;
		public abstract void setSwitchOn(boolean switchOn);
	}
}
