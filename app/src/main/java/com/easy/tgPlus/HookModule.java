package com.easy.tgPlus;

public abstract class HookModule {

    public static final String TAG = "HookModule";

	//是否加载成功
	private boolean loadSuccess;
	//是否启用
	private boolean switchOn;

	public HookModule(){
		//init();
	}

	public final void setSwitchOn(boolean switchOn) {
		this.switchOn = switchOn;
	}

	public final boolean getSwitchOn() {
		return this.switchOn;
	}

	public final boolean isLoadSuccess() {
		return loadSuccess;
	}

	public abstract String getModuleId();
	public abstract String getModuleName();
	public abstract String getModuleDoc();
	public abstract boolean init() throws Throwable;
	public abstract boolean isHideModule();
}
