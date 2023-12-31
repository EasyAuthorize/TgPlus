package com.easy.tgPlus.HookImpl;

import com.easy.tgPlus.HookModule;
import com.easy.tgPlus.ModuleConfigs;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class UnlockCopySave extends HookModule {

	@Override
	public boolean isHideModule() {
		return false;
	}

    //模块标签
    public static final String ModuleId = "UnlockCopySave";
	//模块名称
	public static final String ModuleName = "解锁复制保存";
	//模块描述
	public static final String ModuleDoc = "解除内容保护，允许复制保存消息";

	@Override
	public String getModuleId(){
		return ModuleId;
	}

	@Override
	public String getModuleName(){
		return ModuleName;
	}

	@Override
	public String getModuleDoc(){
		return ModuleDoc;
	}
	
	@Override
	public boolean init() throws Throwable{
		//解锁复制保存
		ModuleConfigs modConf = this.getModuleConfigs();
		XC_LoadPackage.LoadPackageParam lpparam = modConf.getLoadPackageParam();
		try{
			XposedHelpers.findAndHookMethod("org.telegram.messenger.MessagesController", 
				lpparam.classLoader, 
				"isChatNoForwards", 
				lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$Chat"), 
				new XC_MethodHook(){
					@Override
					public void afterHookedMethod(MethodHookParam param){
						if(getSwitchOn()){
							param.setResult(false);
						}
					}
				});
			//加载成功
			return true;
		}catch (ClassNotFoundException e){
			XposedBridge.log(e);
		}
		return false;
	}

}
