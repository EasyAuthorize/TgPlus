package com.easy.tgPlus;

public abstract class HookModule {
    
    public static final String TAG = "HookModule";
    
	//模块ID
    public String ModuleId;
	//模块名称
	public String ModuleName;
	//模块描述
	public String ModuleDoc;
	//是否加载成功
	private boolean loadSuccess;
	//是否启用
	private boolean switchOn;
	
	public HookModule(String mId,String mName,String mDoc){
		ModuleId = mId;
		ModuleName = mName;
		ModuleDoc = mDoc;
		//这个时候是没有Context的
		//switchOn = ModuleConfigs.getInstance().getConf().getBoolean(ModuleId,false);
		loadSuccess = init();
	}

	public void setSwitchOn(boolean switchOn){
		this.switchOn = switchOn;
	}

	public boolean isSwitchOn(){
		return switchOn;
	}

	public boolean isLoadSuccess(){
		return loadSuccess;
	}

	public String getModuleDoc(){
		return ModuleDoc;
	}

	public String getModuleName(){
		return ModuleName;
	}

	public String getModuleId(){
		return ModuleId;
	}
	
	protected boolean init(){
		return false;
	}
    
}
