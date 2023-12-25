# TgPlus

[![Xposed](https://img.shields.io/badge/-Xposed-green?style=flat&logo=Android&logoColor=white)](#)
[![GitHub all releases](https://img.shields.io/github/downloads/EasyAuthorize/TgPlus/total?label=Downloads)](https://github.com/EasyAuthorize/TgPlus/releases)  
Xposed-Modules-Repo  
[![GitHub all releases](https://img.shields.io/github/downloads/Xposed-Modules-Repo/com.easy.tgPlus/total?label=Downloads)](https://github.com/Xposed-Modules-Repo/com.easy.tgPlus/releases)

## 模块简介
这是一个Xposed模块  
***此项目使用GPLv3许可证***

## 食用方法
请提前准备好Xposed环境  
激活此模块后根据实际情况  
勾选Telegram为作用域并  
重新启动Telegram

## 功能介绍
* 1.解除内容保护  
允许复制，保存消息内容

* 2.复读机  
弹出菜单增加"+1"选项  
选择后将重复发送该消息的文本内容

* 3.防撤回  
对方删除消息时阻止删除  
并标记消息为已删除  
*删除标记实现为自定义flag*  
```public static final int FLAG_DELETED = 1 << 28;```  
***版本更新时可能会丢失全部聊天记录***

## 写在后面
模块完全开源
