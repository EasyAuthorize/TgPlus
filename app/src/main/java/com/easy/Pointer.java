package com.easy;

public class Pointer {
    
    public static final String TAG = "Pointer";
    
    public Object obj;
	
	public Pointer(Object obj){
		this.obj = obj;
	}
	
	public Pointer() {}

	public void setObj(Object obj) {
		this.obj = obj;
	}

	public Object getObj() {
		return obj;
	}
    
}
