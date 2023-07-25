package com.easy.tgPlus;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.easy.Pointer;
import java.util.ArrayList;

public class ModuleConfigs {

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
		"it.owlgram.android",
		"xyz.nextalone.nagram",
		"uz.unnarsx.cherrygram"
		//test
		, "com.easy.virtualsight"
	);

	Context con = null;//AndroidAppHelper.currentApplication();
	Activity topActivity = null;
	String runPackage = null;
	String procName = null;
	SharedPreferences conf;

	XC_LoadPackage.LoadPackageParam loadPackage;

	HashMap<String,HookModule> modList = new  HashMap<String,HookModule>();

	public ModuleConfigs(XC_LoadPackage.LoadPackageParam lpparam) {
		this();
		String packageName = lpparam.packageName;
		String procName = lpparam.processName;
		setLoadPackageParam(lpparam);
		setRunPackage(packageName);
		setProcName(procName);
	}

	private ArrayList<Runnable> createActivityCallBack = new ArrayList<Runnable>(1);

	public ModuleConfigs() {
		//程序启动后获得上下文
		//attachBaseContext
		final Pointer handle_attach = new Pointer();
		handle_attach.obj = XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					ModuleConfigs.this.setContext((Context)param.args[0]);
					XposedBridge.log("attach: " + ModuleConfigs.this.getRunPackage() + " Context已获取");
					//获得上下文后刷新模块开关状态
					ModuleConfigs.this.upDateSwitch();
					((XC_MethodHook.Unhook)handle_attach.obj).unhook();
				}
			});

		XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					XposedBridge.log("onCreate: " + ModuleConfigs.this.getRunPackage() + " Activity已创建");
					ModuleConfigs.this.setTopActivity((Activity)param.thisObject);
					for(Runnable r : createActivityCallBack){
						r.run();
					}
					createActivityCallBack.clear();
				}
			});
		XposedHelpers.findAndHookMethod(Activity.class, "onRestart", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					XposedBridge.log("onRestart: " + ModuleConfigs.this.getRunPackage() + " Activity已置于可见状态");
					ModuleConfigs.this.setTopActivity((Activity)param.thisObject);
				}
			});
	}

	public void setOnCreateCallBack(Runnable run) {
		createActivityCallBack.add(run);
	}

	public void setTopActivity(Activity newActivity) {
		topActivity = newActivity;
	}

	public Activity getTopActivity() {
		return topActivity;
	}

	public void modInit() {
		for (HookModule hm : modList.values()) {
			hm.setModuleConfigs(this);
			try {
				hm.load();
			} catch (Throwable e) {

			}
			XposedBridge.log("模块:" + hm.getModuleId() + "(" + hm.getModuleName() + ")" + (hm.isLoadSuccess() ?"加载成功": "加载失败"));
		}
	}

	public void setProcName(String procName) {
		this.procName = procName;
		XposedBridge.log("进程:" + procName);
	}

	public static boolean isThisPackage(String runPackageName) {
		return runPackageName.equals(thisPackage);
	}

	public static boolean isTargetPackage(String runPackageName) {
		return hookPackages.contains(runPackageName);
	}

	public void setLoadPackageParam(XC_LoadPackage.LoadPackageParam lpparam) {
		this.loadPackage = lpparam;
	}

	public XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
		return loadPackage;
	}

	public Context getContext() {
		return con;
	}

	public void setRunPackage(String packageName) {
		runPackage = packageName;
	}

	public String getRunPackage() {
		return runPackage;
	}

	public void setContext(Context con) {
		this.con = con;
	}

	private SharedPreferences getConf() {
		if (conf == null) {
			conf = con.getSharedPreferences(thisAppName, Context.MODE_PRIVATE);
		}
		return conf;
	}

	public void addHookModule(HookModule hMod) {
		modList.put(hMod.getModuleId(), hMod);
	}

	public void upDateSwitch() {
		SharedPreferences sp = getConf();
		for (HookModule h : modList.values()) {
			if (h.isLoadSuccess()) {
				boolean switchOn = sp.getBoolean(h.getModuleId(),/*false*/true);
				h.setSwitchOn(switchOn);
				XposedBridge.log("模块 " + h.getModuleName() + " 激活状态变更 -> " + switchOn);
			}
		}
	}

	public void setSwitch(String moduleId, boolean switchOn) {
		SharedPreferences sp = getConf();
		SharedPreferences.Editor e = sp.edit();
		e.putBoolean(moduleId, switchOn);
		e.apply();
		modList.get(moduleId).setSwitchOn(switchOn);
	}

	public void setSwitch(boolean switchOn) {
		SharedPreferences sp = getConf();
		SharedPreferences.Editor e = sp.edit();
		for (Map.Entry<String, HookModule> entry : modList.entrySet()) {
            String id = entry.getKey();
            HookModule hm = entry.getValue();
			hm.setSwitchOn(switchOn);
			e.putBoolean(id, switchOn);
        }
		e.apply();
	}

}
