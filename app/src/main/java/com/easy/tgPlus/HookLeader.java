package com.easy.tgPlus;

import android.app.Application;
import android.content.Context;
import com.easy.tgPlus.HookImpl.Repeater;
import com.easy.tgPlus.HookImpl.UnlockCopySave;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.HashMap;

public class HookLeader implements IXposedHookLoadPackage{

	final ModuleConfigs modConf = ModuleConfigs.getInstance();

	int testId = 0;

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable{
		if(modConf.isTargetPackage||modConf.isThisPackage)return;
		modConf.setLoadPackageParam(lpparam);
		modConf.setRunPackage(lpparam.packageName);
		if (modConf.isThisPackage){
			Class<?> mainActivity = lpparam.classLoader.loadClass("com.easy.tgPlus.MainActivity");
			XposedHelpers.findAndHookMethod(mainActivity, "isActivate", XC_MethodReplacement.returnConstant(true));
			//测试
			return;
		}
		if (!modConf.isTargetPackage)return;

		//程序启动后获得上下文
		//attachBaseContext
		XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable{
					ModuleConfigs modConf = ModuleConfigs.getInstance();
					modConf.setContext((Context)param.args[0]);
					XposedBridge.log("attach: " + modConf.getRunPackage() + " Context已获取");
					//获得上下文后刷新模块开关状态
					modConf.upDateSwitch();
				}
			});


		final boolean isWebPackage = lpparam.packageName.equals("org.telegram.messenger.web");

		XposedBridge.log(isWebPackage ?"TG(Web)运行～": "TG运行～" + 
			new SimpleDateFormat("yyyy年MM月dd日 a hh:mm:ss").format(System.currentTimeMillis()));
		//语言包hook(暂时没有hook更改语言包)
		XposedHelpers.findAndHookConstructor("org.telegram.messenger.LocaleController", lpparam.classLoader, new XC_MethodHook(){
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable{
					//实例化之后执行
					Object mLocaleController = param.thisObject;
					HashMap<String,String> mlocaleValues = (HashMap<String,String>)XposedHelpers.getObjectField(mLocaleController, "localeValues");
					String rawStr = mlocaleValues.get("Delete");
					//这代表删除并非删除(防撤回)
					mlocaleValues.put("Delete", "(" + rawStr + ")");
					if (isWebPackage){
						mlocaleValues.put("AppName", "Telegram(Web)");
						mlocaleValues.put("Page1Title", "Telegram(Web)");
					}
				}
			});

		HookModule hm = new UnlockCopySave();
		modConf.addHookModule(hm);
		if (BuildConfig.DEBUG){
			XposedBridge.log("模块:" + hm.TAG + "(" + hm.ModuleName + ")" + (hm.isLoadSuccess() ?"加载成功": "加载失败"));
		}
		hm = new Repeater();
		modConf.addHookModule(hm);
		if (BuildConfig.DEBUG){
			XposedBridge.log("模块:" + hm.TAG + "(" + hm.ModuleName + ")" + (hm.isLoadSuccess() ?"加载成功": "加载失败"));
		}

		//debug
		if (BuildConfig.DEBUG){
			//打印调用堆栈
			XposedHelpers.findAndHookMethod("org.telegram.messenger.LocaleController", lpparam.classLoader, "formatString", String.class , int.class, Object[].class, new XC_MethodHook(){
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable{
						//获取某字符串的调用堆栈
						if ((int)param.args[1] == testId){
							// 获取调用堆栈信息
							XposedBridge.log(getStackTraceInfo().toString());
						}
					}
				});
			Class<?> sendMsgHelper = lpparam.classLoader.loadClass("org.telegram.messenger.SendMessagesHelper");
			XposedBridge.hookAllMethods(sendMsgHelper, "sendMessage", new XC_MethodHook(){
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable{
						Object[] args = param.args;
						StringBuilder sb = new StringBuilder("SendMessages\nsend参数:").append(args.length).append('\n');
						for (Object obj : args){
							sb.append(obj);
							sb.append('\n');
						}
						XposedBridge.log(sb.toString());
					}
				});
		}

	}

	public static StringBuilder objDump(StringBuilder sb, Object obj, int hideModifiers){
        if (sb == null || obj == null){
            throw new IllegalArgumentException("Object parameters cannot be null.");
        }

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields){
			if ((field.getModifiers() & hideModifiers) != 0){
				continue;
			}
            field.setAccessible(true);
			String modifierString = Modifier.toString(field.getModifiers());
			String className = field.getType().getSimpleName();
            String fieldName = field.getName();
            Object fieldValue = null;

            try{
                fieldValue = field.get(obj);
            }catch (IllegalAccessException e){
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
	
	public static StringBuilder getStackTraceInfo(){
		StringBuilder sb = new StringBuilder();
		StackTraceElement[] stackElements = Thread.currentThread().getStackTrace();
		if (stackElements != null){
			for (int i = 0; i < stackElements.length; i++){
				sb.append("\tat ");
				sb.append(stackElements[i].getClassName()).append(".");
				sb.append(stackElements[i].getMethodName());
				sb.append("(").append(stackElements[i].getFileName()).append(":");
				sb.append(stackElements[i].getLineNumber()).append(")\n");
			}
		}
		return sb;
	}

	public static void logStringResHook(int id, String rawStr){
		XposedBridge.log("资源id:" + Integer.toHexString(id) + "(\"" + rawStr + "\")hook成功～\n");
	}

}
