package com.easy.tgPlus;
import de.robv.android.xposed.XposedBridge;

public abstract class HookModule {

    public static final String TAG = "HookModule";

	//是否加载成功
	private boolean loadSuccess;
	//是否启用
	private boolean switchOn;
	
	private ModuleConfigs conf;

	public void setSwitchOn(boolean switchOn) {
		this.switchOn = switchOn;
	}

	public final boolean getSwitchOn() {
		return this.switchOn;
	}

	public final boolean isLoadSuccess() {
		return loadSuccess;
	}
	
	public ModuleConfigs getModuleConfigs(){
		return this.conf;
	}
	
	public void setModuleConfigs(ModuleConfigs conf){
		this.conf = conf;
	}
	
	public void load() throws Throwable{
		loadSuccess = init();
	}

	public abstract String getModuleId();
	public abstract String getModuleName();
	public abstract String getModuleDoc();
	public abstract boolean init() throws Throwable;
	public abstract boolean isHideModule();
	
	//感觉接口不好用
	public interface Impl{
		public abstract String getModuleId();
		public abstract String getModuleName();
		public abstract String getModuleDoc();
		public abstract boolean init(ModuleConfigs mConf) throws Throwable;
		public abstract boolean isHideModule();
		public abstract void switchCallBack();
	}
	
}
