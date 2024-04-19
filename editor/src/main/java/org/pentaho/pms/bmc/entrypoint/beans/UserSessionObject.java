package org.pentaho.pms.bmc.entrypoint.beans;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class UserSessionObject {

	private String key;
	private String secret; 
	private boolean authenticated;

	@Override
	public String toString() {
		return "AuthenticatedObject [key=" + key + ", secret=" + secret + "]";
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}
	private void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	public void setAttribute(String key, String value) {
		for (java.lang.reflect.Field field : UserSessionObject.class.getDeclaredFields()) {
	        if (!Modifier.isStatic(field.getModifiers())) {
	        	if (field.getName().equalsIgnoreCase(key)) {
	        		List<Method> methodList =  Arrays.asList( UserSessionObject.class.getDeclaredMethods() );
	        		methodList.stream().filter(m -> m.getName().equalsIgnoreCase("set" + field.getName())).forEach(method -> {
	        		Method instanceMethod;
					try {
						instanceMethod = UserSessionObject.class.getDeclaredMethod(method.getName(), String.class);
						instanceMethod.invoke(this, value);
					} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}
	        		});
	        	}
	        }
		}
	}
	
}
