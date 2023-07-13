package com.easy.tgPlus.HookImpl;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.Toast;
import com.easy.tgPlus.BuildConfig;
import com.easy.tgPlus.HookLeader;
import com.easy.tgPlus.HookModule;
import com.easy.tgPlus.ModuleConfigs;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

public class Repeater extends HookModule{

    //模块标签
    public static final String ModuleId = "Repeater";
	//模块名称
	public static final String ModuleName = "复读机";
	//模块描述
	public static final String ModuleDoc = "点击消息的弹出菜单增加+1选项，点击后自动发送该消息的文本内容";

	public Repeater(){
		super(ModuleId, ModuleName, ModuleDoc);
	}

    @Override
	protected boolean init(){
		//解锁复制保存
		final ModuleConfigs modConf = ModuleConfigs.getInstance();
		final XC_LoadPackage.LoadPackageParam lpparam = modConf.getLoadPackageParam();
		try{
			//复读机
			final Class<?> AndroidUtilities = lpparam.classLoader.loadClass("org.telegram.messenger.AndroidUtilities");
			final Class<?> abm = lpparam.classLoader.loadClass("org.telegram.ui.ActionBar.ActionBarMenuSubItem");
			XposedHelpers.findAndHookMethod("org.telegram.ui.Components.ChatScrimPopupContainerLayout", lpparam.classLoader, "setPopupWindowLayout", lpparam.classLoader.loadClass("org.telegram.ui.ActionBar.ActionBarPopupWindow$ActionBarPopupWindowLayout"), new XC_MethodHook(){
					@Override
					protected void beforeHookedMethod(MethodHookParam param){
						if (!isSwitchOn()){return;}
						Resources res = modConf.getContext().getResources();
						final Object t = param.thisObject;
						XposedBridge.log(t.toString());
						final Object main = XposedHelpers.getObjectField(t, "this$0");
						final Context con = (Context)XposedHelpers.callMethod(main, "getParentActivity");
						Object popupLayout = param.args[0];
						//构造方法参数为:上下文，是否第一位，是否末位(影响点按渐变效果的圆角)
						Object fd = XposedHelpers.newInstance(abm, new Class[]{Context.class,boolean.class,boolean.class}, new Object[]{con,false,false});
						//如果dp函数在回调外面调用，emoji会缩成一坨
						int dp200 = XposedHelpers.callStaticMethod(AndroidUtilities, "dp", new Class[]{float.class}, new Object[]{200f});
						XposedHelpers.callMethod(fd, "setMinimumWidth", new Class[]{int.class}, new Object[]{dp200});
						XposedHelpers.callMethod(fd, "setTextAndIcon", new Class[]{String.class,int.class}, new Object[]{"+1",res.getIdentifier("msg_send", "drawable", modConf.getRunPackage())});
						XposedHelpers.callMethod(fd, "setOnClickListener", new Class[]{View.OnClickListener.class}, new Object[]{new View.OnClickListener(){

									@Override
									public void onClick(View v){
										//这句貌似没必要，也不需要参数
										XposedHelpers.callMethod(main, "closeMenu", new Class[]{boolean.class}, new Object[]{true});
										Object selectedObject = XposedHelpers.getObjectField(main, "selectedObject");
										Object selectedObjectGroup = XposedHelpers.getObjectField(main, "selectedObjectGroup");
										Object threadMessageObject = XposedHelpers.getObjectField(main, "threadMessageObject");
										SpannableStringBuilder content = (SpannableStringBuilder)XposedHelpers.callMethod(main, "getMessageContent", new Class[]{selectedObject.getClass(),long.class,boolean.class}, new Object[]{selectedObject,0,false});
										Object sendMsgHelper = XposedHelpers.callMethod(main, "getSendMessagesHelper");

										try{
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

										}catch (ClassNotFoundException e){
											XposedBridge.log(e);
										}

									}
								}});
						XposedHelpers.callMethod(popupLayout, "addView", new Class[]{View.class}, new Object[]{fd});

						if (BuildConfig.DEBUG){
							fd = XposedHelpers.newInstance(abm, new Class[]{Context.class,boolean.class,boolean.class}, new Object[]{con,false,false});
							//如果dp函数在回调外面调用，emoji会缩成一坨
							//int dp200 = XposedHelpers.callStaticMethod(AndroidUtilities, "dp", new Class[]{float.class}, new Object[]{200f});
							XposedHelpers.callMethod(fd, "setMinimumWidth", new Class[]{int.class}, new Object[]{dp200});
							XposedHelpers.callMethod(fd, "setTextAndIcon", new Class[]{String.class,int.class}, new Object[]{"显示MessageObject",res.getIdentifier("msg_message", "drawable", modConf.getRunPackage())});
							XposedHelpers.callMethod(fd, "setOnClickListener", new Class[]{View.OnClickListener.class}, new Object[]{new View.OnClickListener(){

										@Override
										public void onClick(View v){
											XposedHelpers.callMethod(main, "closeMenu", new Class[]{boolean.class}, new Object[]{true});

											final Object selectedObject = XposedHelpers.getObjectField(main, "selectedObject");
											Object selectedObjectGroup = XposedHelpers.getObjectField(main, "selectedObjectGroup");
											Object threadMessageObject = XposedHelpers.getObjectField(main, "threadMessageObject");


											final AlertDialog.Builder adBuilder = new AlertDialog.Builder(con){
												private CharSequence msg;
												public CharSequence getMessage(){
													return msg;
												}
												@Override
												public AlertDialog.Builder setMessage(CharSequence msg){
													this.msg = msg;
													return super.setMessage(msg);
												}
												//抱歉，我的IDE并不能识别内部匿名类(adBuilder.getMessage())
												//所以需要这个实现
												@Override
												public String toString(){
													return this.msg.toString();
												}
											};
											adBuilder.setTitle("MessageObject")
												.setMessage(HookLeader.objDump(new StringBuilder(selectedObject.toString()).append('\n'), selectedObject, 0))
												.setPositiveButton("确认", new DialogInterface.OnClickListener(){
													@Override
													public void onClick(DialogInterface dialog, int which){
														Object msg = XposedHelpers.getObjectField(selectedObject,"messageOwner");
														//XposedHelpers.setBooleanField(msg, "edit_hide", false);
														XposedHelpers.setIntField(msg, "flags", XposedHelpers.getIntField(msg,"flags") | 0x10000000);
														Class<?> AndroidUtil;
														try{
															AndroidUtil = lpparam.classLoader.loadClass("org.telegram.messenger.AndroidUtilities");
														}catch (ClassNotFoundException e){e.printStackTrace();return;}
														XposedHelpers.callStaticMethod(AndroidUtil, "runOnUIThread", new Class[]{Runnable.class}, new Object[]{
																new Runnable(){
																	@Override
																	public void run(){
																		try{
																			Object chatListView = XposedHelpers.getObjectField(main, "chatListView");
																			Object chatAdapter = XposedHelpers.getObjectField(main, "chatAdapter");
																			ArrayList msgs;
																			if (XposedHelpers.getBooleanField(chatAdapter, "isFrozen")){
																				msgs = (ArrayList)XposedHelpers.getObjectField(chatAdapter, "frozenMessages");
																			}else{
																				msgs = (ArrayList)XposedHelpers.getObjectField(main, "messages");
																			}
																			int index = msgs.indexOf(selectedObject);
																			if (index == -1){
																				XposedBridge.log("找不到对象");
																			}else{
																				View cell = (View) XposedHelpers.callMethod(XposedHelpers.callMethod(chatListView,"getLayoutManager"),"findViewByPosition",new Class<?>[]{int.class},new Object[]{index});
																				if(cell == null){
																					XposedBridge.log("View cell查找失败");
																					return;
																				}
																				//XposedHelpers.callMethod(cell,"measureTime",selectedObject);
																				//XposedHelpers.callMethod(XposedHelpers.getObjectField(main,"chatAdapter"),"notifyDataSetChanged",new Class[]{boolean.class},new Object[]{false});
																				//这行是最初的刷新，带动画，没有函数比他更好用，经常刷不动
																				XposedHelpers.callMethod(XposedHelpers.getObjectField(main,"chatAdapter"),"updateRowWithMessageObject",new Class<?>[]{selectedObject.getClass(),boolean.class},new Object[]{selectedObject,false});
																				XposedHelpers.callMethod(cell,"setMessageContent",new Class<?>[]{
																						selectedObject.getClass(),
																						lpparam.classLoader.loadClass("org.telegram.messenger.MessageObject$GroupedMessages"),
																						boolean.class,
																						boolean.class
																					},new Object[]{
																						selectedObject,
																						XposedHelpers.getObjectField(cell,"groupedMessagesToSet"),
																						false,
																						false
																					});
																				XposedHelpers.callMethod(chatAdapter, "notifyItemChanged", new Class[]{int.class}, new Object[]{index});
																			}
																		}catch(Exception e){e.printStackTrace();}
																	}
															}
														});

													}
												})
												.setNegativeButton("复制", new DialogInterface.OnClickListener(){
													@Override
													public void onClick(DialogInterface dialog, int which){
														ClipboardManager clipboard = (ClipboardManager) con.getSystemService(Context.CLIPBOARD_SERVICE);
														ClipData clip = ClipData.newPlainText("消息", adBuilder.toString());
														clipboard.setPrimaryClip(clip);
														//Toast.makeText(con,"",Toast.LENGTH_SHORT).show();
													}
												})
												.setNeutralButton("隐藏final", new DialogInterface.OnClickListener(){
													@Override
													public void onClick(DialogInterface dialog, int which){
														adBuilder.setNeutralButton("显示messageOwner", new DialogInterface.OnClickListener(){
																@Override
																public void onClick(DialogInterface p1, int p2){
																	adBuilder.setNeutralButton(null,null);
																	Object msg = XposedHelpers.getObjectField(selectedObject,"messageOwner");
																	adBuilder.setMessage(HookLeader.objDump(new StringBuilder(msg.toString()).append('\n'), msg, 0));
																	adBuilder.show();
																}
														});
														adBuilder.setMessage(HookLeader.objDump(new StringBuilder(selectedObject.toString()).append('\n'), selectedObject, Modifier.FINAL));
														adBuilder.show();
													}
												});
											adBuilder.show();

										}
									}});
							XposedHelpers.callMethod(popupLayout, "addView", new Class[]{View.class}, new Object[]{fd});
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
