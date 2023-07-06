package com.easy.tgPlus;
import android.app.ActivityThread;
import android.content.Context;
import java.util.List;
import android.content.SharedPreferences;
import java.util.Arrays;
import android.app.AndroidAppHelper;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.content.res.Resources;
import java.util.HashMap;
import de.robv.android.xposed.XposedBridge;

public class ModuleConfigs{

    public static final String TAG = "ModuleConfigs";

	public static final String thisPackage = "com.easy.tgPlus";
	public static final String thisAppName = "TgPlus";
	public static final List hookPackages = Arrays.asList(
		"org.telegram.messenger", 
		"org.telegram.messenger.web", 
		"org.telegram.messenger.beta", 
		"nekox.messenger", 
		"com.cool2645.nekolite", 
		"org.telegram.plus", 
		"com.iMe.android", 
		"org.telegram.BifToGram", 
		"ua.itaysonlab.messenger", 
		"org.forkclient.messenger", 
		"org.forkclient.messenger.beta", 
		"org.aka.messenger", 
		"ellipi.messenger", 
		"org.nift4.catox", 
		"it.owlgram.android"
	);

	private static ModuleConfigs INSTANCE = new ModuleConfigs();

	Context con = null;//AndroidAppHelper.currentApplication();
	String runPackage = null;
	SharedPreferences conf;

	XC_LoadPackage.LoadPackageParam loadPackage;

	boolean isThisPackage = false,isTargetPackage = false;
	
	HashMap<String,HookModule> modList = new  HashMap<String,HookModule>();

	private ModuleConfigs(){
		//单例
	}

	public void setLoadPackageParam(XC_LoadPackage.LoadPackageParam lpparam){
		this.loadPackage = lpparam;
	}
	
	public XC_LoadPackage.LoadPackageParam getLoadPackageParam(){
		return loadPackage;
	}

	public Context getContext(){
		return con;
	}

	public void setRunPackage(String packageName){
		runPackage = packageName;

		isThisPackage = thisPackage.equals(runPackage);
		if (isThisPackage){
			isTargetPackage = false;
			return;
		}
		isTargetPackage = hookPackages.contains(runPackage);
	}
	
	public String getRunPackage(){
		return runPackage;
	}

	public void setContext(Context con){
		this.con = con;
	}

	public SharedPreferences getConf(){
		if (conf == null){
			conf = con.getSharedPreferences(thisAppName, Context.MODE_PRIVATE);
		}
		return conf;
	}

	public static ModuleConfigs getInstance(){
		return INSTANCE;
	}
	
	public void addHookModule(HookModule hMod){
		modList.put(hMod.getModuleId(),hMod);
	}
	
	public void upDateSwitch(){
		SharedPreferences sp = getConf();
		for(HookModule h : modList.values()){
			h.setSwitchOn(sp.getBoolean(h.getModuleId(),false));
			XposedBridge.log("模块 " + h.getModuleName() +" 激活状态变更 ->" + sp.getBoolean(h.getModuleId(),false));
		}
	}
	
	public void setSwitch(String moduleNmae,boolean switchOn){
		
	}

}
