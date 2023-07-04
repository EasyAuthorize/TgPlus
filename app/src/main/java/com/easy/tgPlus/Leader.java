package com.easy.tgPlus;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import de.robv.android.xposed.XC_MethodReplacement;
import java.util.List;
import java.util.Arrays;
import android.widget.LinearLayout;
import android.view.View;
import android.content.Context;
import android.widget.Toast;
import android.view.ViewGroup;
import android.text.SpannableStringBuilder;
import java.util.ArrayList;

public class Leader implements IXposedHookLoadPackage,IXposedHookInitPackageResources {

	public static final List hookPackages = Arrays.asList("org.telegram.messenger", "org.telegram.messenger.web", "org.telegram.messenger.beta", "nekox.messenger", "com.cool2645.nekolite", "org.telegram.plus", "com.iMe.android", "org.telegram.BifToGram", "ua.itaysonlab.messenger", "org.forkclient.messenger", "org.forkclient.messenger.beta", "org.aka.messenger", "ellipi.messenger", "org.nift4.catox", "it.owlgram.android");

	boolean isTargetPackage;

	int res_msg_send_Id = 0;

	//Context con = AndroidAppHelper.currentApplication();

	@Override
	public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

		if (!hookPackages.contains(resparam.packageName))return;

		String resPackageName = resparam.res.getPackageName();
		XposedBridge.log("资源包:" + resPackageName + "已处理");
		int delete = resparam.res.getIdentifier("Delete", "string", resPackageName);
		String rawStr = resparam.res.getString(delete);
		resparam.res.setReplacement(delete, "(删除)");
		logStringResHook(delete, rawStr);

		int en = resparam.res.getIdentifier("EditName", "string", resPackageName);
		rawStr = resparam.res.getString(en);
		resparam.res.setReplacement(en, "(编辑名称)");
		logStringResHook(en, rawStr);

		res_msg_send_Id = resparam.res.getIdentifier("msg_send", "drawable", resPackageName);

		if (resparam.packageName.equals("org.telegram.messenger.web")) {
			int id = resparam.res.getIdentifier("AppName", "string", resPackageName);
			rawStr = resparam.res.getString(id);
			resparam.res.setReplacement(en, "Telegram(Web)");
			logStringResHook(id, rawStr);

			id = resparam.res.getIdentifier("Page1Title", "string", resPackageName);
			rawStr = resparam.res.getString(id);
			resparam.res.setReplacement(en, "Telegram(Web)");
			logStringResHook(id, rawStr);
			return;
		}

	}


	@Override
	public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("com.easy.tgPlus")) {
			Class<?> mainActivity = lpparam.classLoader.loadClass("com.easy.tgPlus.MainActivity");
			XposedHelpers.findAndHookMethod(mainActivity, "isActivate", XC_MethodReplacement.returnConstant(true));
			return;
		}

		if (!hookPackages.contains(lpparam.packageName))return;

		final boolean isWebPackage = lpparam.packageName.equals("org.telegram.messenger.web");

		XposedBridge.log(isWebPackage ?"TG(Web)运行～": "TG运行～" + new SimpleDateFormat("yyyy年MM月dd日 a hh:mm:ss").format(System.currentTimeMillis()) + "\n");
		XposedBridge.log("Hook～\n");
		XposedHelpers.findAndHookConstructor("org.telegram.messenger.LocaleController", lpparam.classLoader, new XC_MethodHook(){
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					//实例化之后执行
					Object mLocaleController = param.thisObject;
					HashMap<String,String> mlocaleValues = (HashMap<String,String>)XposedHelpers.getObjectField(mLocaleController, "localeValues");
					mlocaleValues.put("Delete", "(删除)");
					if (isWebPackage) {
						mlocaleValues.put("AppName", "Telegram(Web)");
						mlocaleValues.put("Page1Title", "Telegram(Web)");
					}
				}
			});

		//解锁复制保存
		XposedHelpers.findAndHookMethod("org.telegram.messenger.MessagesController", lpparam.classLoader, "isChatNoForwards", lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$Chat"), XC_MethodReplacement.returnConstant(false));

		//复读机
		final Class<?> AndroidUtilities = lpparam.classLoader.loadClass("org.telegram.messenger.AndroidUtilities");
		final Class<?> abm = lpparam.classLoader.loadClass("org.telegram.ui.ActionBar.ActionBarMenuSubItem");
		XposedHelpers.findAndHookMethod("org.telegram.ui.Components.ChatScrimPopupContainerLayout", lpparam.classLoader, "setPopupWindowLayout", lpparam.classLoader.loadClass("org.telegram.ui.ActionBar.ActionBarPopupWindow$ActionBarPopupWindowLayout"), new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param) {
					Object t = param.thisObject;
					XposedBridge.log(t.toString());
					final Object main = XposedHelpers.getObjectField(t, "this$0");
					final Context con = (Context)XposedHelpers.callMethod(main, "getParentActivity");
					Object popupLayout = param.args[0];
					Object fd = XposedHelpers.newInstance(abm, new Class[]{Context.class,boolean.class,boolean.class}, new Object[]{con,false,false});
					//如果dp函数在回调外面调用，emoji会缩成一坨
					int dp200 = XposedHelpers.callStaticMethod(AndroidUtilities, "dp", new Class[]{float.class}, new Object[]{200f});
					XposedHelpers.callMethod(fd, "setMinimumWidth", new Class[]{int.class}, new Object[]{dp200});
					XposedHelpers.callMethod(fd, "setTextAndIcon", new Class[]{String.class,int.class}, new Object[]{"+1",res_msg_send_Id});
					XposedHelpers.callMethod(fd, "setOnClickListener", new Class[]{View.OnClickListener.class}, new Object[]{new View.OnClickListener(){

													 @Override
													 public void onClick(View v) {
														 XposedHelpers.callMethod(main, "closeMenu", new Class[]{boolean.class}, new Object[]{true});
														 Object selectedObject = XposedHelpers.getObjectField(main, "selectedObject");
														 Object selectedObjectGroup = XposedHelpers.getObjectField(main, "selectedObjectGroup");
														 Object threadMessageObject = XposedHelpers.getObjectField(main, "threadMessageObject");
														 SpannableStringBuilder content = (SpannableStringBuilder)XposedHelpers.callMethod(main, "getMessageContent", new Class[]{selectedObject.getClass(),long.class,boolean.class}, new Object[]{selectedObject,0,false});
														 Object sendMsgHelper = XposedHelpers.callMethod(main, "getSendMessagesHelper");

														 try {
															 Class<?> msgObj = lpparam.classLoader.loadClass("org.telegram.messenger.MessageObject");
															 Class<?> rpcWebPage = lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$WebPage");
															 Class<?> rpcReplyMarkup = lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$ReplyMarkup");
															 Class<?> msgObjSendData = lpparam.classLoader.loadClass("org.telegram.messenger.MessageObject$SendAnimationData");
															 //这里还需要判断消息类型调用对应的send方法
															 XposedHelpers.callMethod(sendMsgHelper, "sendMessage", 
																					  new Class[]{String.class,long.class,msgObj,msgObj,rpcWebPage,boolean.class,ArrayList.class,rpcReplyMarkup,HashMap.class,boolean.class,int.class,msgObjSendData,boolean.class}, 
																					  new Object[]{content.toString(),
																						  //XposedHelpers.callMethod(selectedObject,"getDialogId"),
																						  XposedHelpers.getObjectField(main, "dialog_id"),
																						  threadMessageObject, threadMessageObject, null, true, null, null, null, true, 0, null, false});
															 Toast.makeText(con, "复读机:" + content.toString() , Toast.LENGTH_SHORT).show();
														 } catch (ClassNotFoundException e) {
															 XposedBridge.log(e);
														 }

													 }
												 }});
					XposedHelpers.callMethod(popupLayout, "addView", new Class[]{View.class}, new Object[]{fd});
				}
			});

		
		if (!BuildConfig.DEBUG)return;

		//debug
		if (isWebPackage) {
			//打印getString调用堆栈
			XposedHelpers.findAndHookMethod("org.telegram.messenger.LocaleController", lpparam.classLoader, "getString", String.class , int.class, new XC_MethodHook(){
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						//获取某字符串的调用堆栈 这里应该是字符串ID而不是可绘制的图像
						if ((int)param.args[1] == res_msg_send_Id) {
							// 获取调用堆栈信息
							StringBuilder sb = new StringBuilder();
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
							XposedBridge.log(sb.toString());
						}
					}
				});
		} else {
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

	public static void logStringResHook(int id, String rawStr) {
		XposedBridge.log("资源id:" + Integer.toHexString(id) + "(\"" + rawStr + "\")hook成功～\n");
	}

}
