package com.easy.tgPlus;

import android.app.AlertDialog;
import com.easy.Pointer;
import com.easy.tgPlus.HookImpl.*;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.HashMap;

public class HookLeader implements IXposedHookLoadPackage,IXposedHookInitPackageResources {

	int testId = 0;

	@Override
	public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resParam) throws Throwable {
		String pkName = resParam.packageName;
		if (ModuleConfigs.isTargetPackage(pkName)) {
			if (BuildConfig.DEBUG) {
				testId = resParam.res.getIdentifier("EditedMessage", "string", pkName);
				XposedBridge.log("正在检测字符串(" + resParam.res.getString(testId) + ")调用堆栈");
			}
		}
	}

	int a = 0;
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		String packageName = lpparam.packageName;
		XposedBridge.log(a++ + "\n");
		if (ModuleConfigs.isThisPackage(packageName)) {
			Class<?> mainActivity = lpparam.classLoader.loadClass("com.easy.tgPlus.MainActivity");
			Method m = com.easy.tgPlus.MainActivity.class.getDeclaredMethod("isActivate");
			XposedHelpers.findAndHookMethod(mainActivity, m.getName(), XC_MethodReplacement.returnConstant(true));
			//测试
			return;
		}

		if (!ModuleConfigs.isTargetPackage(packageName))return;

		final ModuleConfigs modConf = new ModuleConfigs(lpparam);

		if (lpparam.packageName.equals("com.easy.virtualsight")) {
			XposedBridge.log("test到此一游");
			return;
		}

		XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl.EditorImpl", modConf.getLoadPackageParam().classLoader, "putBoolean", String.class, boolean.class, new XC_MethodHook(){
				boolean isShow = false;
				@Override
				public void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (!isShow && param.args[0].equals("telegram_helper_hook") && ((Boolean)param.args[1]) == true) {
						isShow = true;
						try {
							XposedHelpers.callStaticMethod(modConf.getLoadPackageParam().classLoader.loadClass("org.telegram.messenger.AndroidUtilities"), "runOnUIThread", new Class[]{Runnable.class}, new Object[]{
									new Runnable(){
										@Override
										public void run() {
											AlertDialog.Builder adBuilder = new AlertDialog.Builder(modConf.getTopActivity());
											adBuilder.setTitle("TgPlus")
												.setMessage("检测到不兼容的模块加载\n已关闭TgPlus开关")
												.setPositiveButton("我已知晓", null);
											adBuilder.show();
										}
									}
								});
						} catch (ClassNotFoundException e) {e.printStackTrace();}
						modConf.setSwitch(false);
					}
				}
			});
		//像这样两个连续的三角函数我称之为“驼峰函数”
		final Pointer p = new Pointer();
		p.obj = XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl", modConf.getLoadPackageParam().classLoader, "getBoolean", String.class, boolean.class, new XC_MethodHook(){
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (param.args[0].equals("telegram_helper_hook")) {
						if (((Boolean)param.getResult()) == true) {
							modConf.setSwitch(false);
							modConf.setOnCreateCallBack(new Runnable(){
									@Override
									public void run() {
										try {
											XposedHelpers.callStaticMethod(modConf.getLoadPackageParam().classLoader.loadClass("org.telegram.messenger.AndroidUtilities"), "runOnUIThread", new Class[]{Runnable.class}, new Object[]{
													new Runnable(){
														@Override
														public void run() {
															AlertDialog.Builder adBuilder = new AlertDialog.Builder(modConf.getTopActivity());
															adBuilder.setTitle("TgPlus")
																.setMessage("检测到不兼容的模块加载\n已关闭TgPlus开关")
																.setPositiveButton("我已知晓", null);
															adBuilder.show();
														}
													}
												});
										} catch (ClassNotFoundException e) {e.printStackTrace();}
									}
								});
						}
						((XC_MethodHook.Unhook)p.obj).unhook();
					}
				}
			});

		final boolean isWebPackage = lpparam.packageName.equals("org.telegram.messenger.web");
		XposedBridge.log(isWebPackage ?"TG(Web)运行～": "TG运行～" + 
			new SimpleDateFormat("yyyy年MM月dd日 a hh:mm:ss").format(System.currentTimeMillis()));

		//语言包hook
		XposedHelpers.findAndHookConstructor("org.telegram.messenger.LocaleController", lpparam.classLoader, new XC_MethodHook(){
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					//实例化之后执行
					Object mLocaleController = param.thisObject;
					HashMap<String,String> mlocaleValues = (HashMap<String,String>)XposedHelpers.getObjectField(mLocaleController, "localeValues");
					String rawStr = mlocaleValues.get("Delete");
					//这代表删除并非删除(防撤回)
					//createDeleteMessagesAlert(selectedObject, selectedObjectGroup, 1,true);
					mlocaleValues.put("Delete", "(" + rawStr + ")");
					if (isWebPackage) {
						mlocaleValues.put("AppName", "Telegram(Web)");
						mlocaleValues.put("Page1Title", "Telegram(Web)");
					}
				}
			});

		System.loadLibrary("TgPlus");

		HookModule hm = new UnlockCopySave();
		modConf.addHookModule(hm);

		hm = new Repeater();
		modConf.addHookModule(hm);

		hm = new AntiRetraction();
		modConf.addHookModule(hm);

		modConf.modInit();

		//debug
		if (BuildConfig.DEBUG) {
			//打印调用堆栈
			XposedHelpers.findAndHookMethod("org.telegram.messenger.LocaleController", lpparam.classLoader, "getString", String.class , int.class, new XC_MethodHook(){
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						//获取某字符串的调用堆栈
						if ((int)param.args[1] == testId) {
							// 获取调用堆栈信息
							XposedBridge.log(getStackTraceInfo(new StringBuilder("getString")).toString());
						}
					}
				});
			Class<?> sendMsgHelper = lpparam.classLoader.loadClass("org.telegram.messenger.SendMessagesHelper");
			XposedBridge.hookAllMethods(sendMsgHelper, "sendMessage", new XC_MethodHook(){
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						Object[] args = param.args;
						StringBuilder sb = new StringBuilder("SendMessages\nsend参数:").append(args.length).append('\n');
						for (Object obj : args) {
							sb.append(obj);
							sb.append('\n');
						}
						XposedBridge.log(sb.toString());
					}
				});
		}

	}

	public static StringBuilder objDump(StringBuilder sb, Object obj, int hideModifiers) {
        if (sb == null || obj == null) {
            throw new IllegalArgumentException("Object parameters cannot be null.");
        }

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
			if ((field.getModifiers() & hideModifiers) != 0) {
				continue;
			}
            field.setAccessible(true);
			String modifierString = Modifier.toString(field.getModifiers());
			String className = field.getType().getSimpleName();
            String fieldName = field.getName();
            Object fieldValue = null;

            try {
                fieldValue = field.get(obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            sb.append(modifierString)
				.append(' ')
				.append(className)
				.append(' ')
				.append(fieldName)
				.append(" = ")
				.append(fieldValue)
				.append(System.lineSeparator());
        }

		return sb;
    }

	public static StringBuilder getStackTraceInfo(StringBuilder sb) {
		if (sb == null)
			sb = new StringBuilder();
		StackTraceElement[] stackElements = Thread.currentThread().getStackTrace();
		if (stackElements != null) {
			for (int i = 0; i < stackElements.length; i++) {
				sb.append("\tat ");
				sb.append(stackElements[i].getClassName()).append(".");
				sb.append(stackElements[i].getMethodName());
				sb.append("(").append(stackElements[i].getFileName()).append(":");
				sb.append(stackElements[i].getLineNumber()).append(")\n");
			}
		}
		return sb;
	}

	public static void log(HookModule hm) {
		XposedBridge.log("模块:" + hm.getModuleId() + "(" + hm.getModuleName() + ")" + (hm.isLoadSuccess() ?"加载成功": "加载失败"));
	}

}
