/*
 * ©Copyright 2024 BMC Software. Inc.
 *
 */
package org.pentaho.pms.bmc.entrypoint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.pentaho.pms.bmc.entrypoint.beans.TenantInfoList;
import org.pentaho.pms.bmc.entrypoint.beans.TenantInfoList.TenantInfo;
import org.pentaho.pms.bmc.entrypoint.beans.UserSessionObject;
import org.pentaho.pms.bmc.entrypoint.common.StaticMethodsUtility;
import org.pentaho.pms.bmc.entrypoint.exceptions.RMStudioException;
import org.pentaho.pms.ui.locale.Messages;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.LogLevel;

public class BMCAuthenticatorForPentaho {
	private static BMCAuthenticatorForPentaho authenticatorForPentaho = null;
	private AuthenticationMonitor authTokenCreator = null;
    private LogChannelInterface logBMC = null;
	public TenantInfoList tenantInfoList = null;
	public static boolean editorOpened = Boolean.FALSE;
	
	public enum TENANT_ENV_OPERATION {
		ADD,DELETE,EDIT;
	}

	public static BMCAuthenticatorForPentaho getInstance() {
		if (authenticatorForPentaho == null) {
			authenticatorForPentaho = new BMCAuthenticatorForPentaho();
			authenticatorForPentaho.loadProperties();
			authenticatorForPentaho.startFileMonitor();
		}
		return authenticatorForPentaho;
	}

	public void startFileMonitor() {
	    final Runnable runnable = new Runnable() {
	        public void run() {
				try {
					WatchService watchService = FileSystems.getDefault().newWatchService();
					Path path = Paths.get( TenantInfoList.CONFIG_BASE_PATH );
					path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
					boolean poll = true;

					while (poll) {
					  WatchKey key = watchService.take();
					  Thread.sleep( 50 );
					  for (WatchEvent<?> event : key.pollEvents()) {
						  if (event.context().toString().endsWith( TenantInfoList.ENV_FILE_NAME ) || event.context().toString().equalsIgnoreCase( TenantInfoList.ENV_FILE_NAME )) {
                              if (StandardWatchEventKinds.ENTRY_MODIFY.name().equalsIgnoreCase( event.kind().name() )) {
                            	  if ((TenantInfoList.CONFIG_BASE_PATH + Paths.get( event.context().toString() ).toString()).length() > 0 ) {
    			        	          createEditTenantInfoList( TenantInfoList.CONFIG_BASE_PATH + Paths.get( event.context().toString() ).toString() );
                            	  }
                              }
                              if (StandardWatchEventKinds.ENTRY_DELETE.name().equalsIgnoreCase( event.kind().name() )) {
                            	  if ((TenantInfoList.CONFIG_BASE_PATH + Paths.get( event.context().toString() ).toString()).length() > 0 ) {
                            	      removeTenantInfoFromList(tenantInfoList.getTenantIdFromNonDefaultEnvFileName( event.context().toString() ));
                            	  }
                              }
						  }
					  }
					  poll = key.reset();
					}
				}catch(Exception e) {

				}
	        }
	      };
	      new Thread( runnable ).start();
	}

	private BMCAuthenticatorForPentaho() {
		logBMC = new LogChannel( "[" + this.getClass().getName() + "]" );
		tenantInfoList = new TenantInfoList();
	}

	private void loadProperties() {
		StaticMethodsUtility utility = new StaticMethodsUtility();
		try (Stream<Path> walk = utility.walk(utility.getFilePath( TenantInfoList.CONFIG_BASE_PATH ))) {
		    List<String> result = walk.filter(Files::isRegularFile)
				.map(x -> x.toString()).filter(name -> name.endsWith( TenantInfoList.ENV_FILE_NAME ))
				.collect(Collectors.toList());
			result.forEach(each -> {
				createEditTenantInfoList(each);
	        });
		} catch (Exception e) {
			throw new ExceptionInInitializerError( e );
		}
	}

	private void removeTenantInfoFromList(String tenantId) throws ExceptionInInitializerError {
    	tenantInfoList.removeTenantInfoFromList( tenantId );
	}
	
	private void createEditTenantInfoList(String each) throws ExceptionInInitializerError {
		Path envPropFilePath = new StaticMethodsUtility().getFilePath( each );
		Properties envProperties = new Properties();

		try (InputStream inStream = Files.newInputStream( envPropFilePath )) {
			envProperties.load( inStream );
		} catch (IOException e) {
			throw new ExceptionInInitializerError( e );
		}
		tenantInfoList.addToList( envPropFilePath.getFileName().toString(), envProperties );
	}

	public TenantInfoList getAllTenantInfo() {
		return tenantInfoList;
	}

	public TenantInfo getTenantInfoBasedOnTenantId(String tenantId) {
		return tenantInfoList.getTenantInfoList().stream().filter(tenantInfo -> tenantInfo.getTenantId().equalsIgnoreCase(tenantId)).findFirst().orElse(null);
	}

	public TenantInfo getDefaultTenantInfo() {
		return tenantInfoList.getTenantInfoList().isEmpty() ? null :
			tenantInfoList.getTenantInfoList().stream().filter( tenantInfo -> tenantInfo.isDefaultTenant() ).findFirst().orElse(null);
	}

	public void monitorAuthentication(TenantInfo tenantInfo, UserSessionObject authObj, boolean forceReAuth) throws Exception {
		denyEmptyAuthObj(authObj);

		try {
			authTokenCreator = new AuthenticationMonitor( tenantInfo );

			authTokenCreator.startMonitoringAuthenticationAndAuthorization( authObj, forceReAuth );
            logBMC.setLogLevel(LogLevel.BASIC);
            if (editorOpened) {
            	logBMC.logBasic(Messages.getString("INFO.AUTH_SUCCESS.METADATA_LAUNCHED", tenantInfo.getTenantId()));
            } else {
	            logBMC.logBasic(Messages.getString("INFO.AUTH_SUCCESS.METADATA_LAUNCHING", tenantInfo.getTenantId()));
            }
            editorOpened = Boolean.TRUE;
		} catch (Exception e) {
            logBMC.setLogLevel(LogLevel.ERROR);
            if (!editorOpened) {
    	    	logBMC.logError(Messages.getString("ERROR.METADATA_WINDOW.LAUNCH_FAILED", "BMC auth failed"), e);

    	    	logBMC.setLogLevel(LogLevel.BASIC);
    	    	logBMC.logBasic(Messages.getString("ERROR.METADATA_WINDOW.LAUNCH_FAILED", e.getMessage()));
            } else {
    	    	logBMC.logError(Messages.getString("ERROR.METADATA_WINDOW.REAUTH_FAILED", "BMC auth failed"), e);

    	    	logBMC.setLogLevel(LogLevel.BASIC);
    	    	logBMC.logBasic(Messages.getString("ERROR.METADATA_WINDOW.REAUTH_FAILED", e.getMessage()));
            }
	    	throw e;
		}
    }

	private void denyEmptyAuthObj(UserSessionObject authObj) {
		if (authObj.getKey() == null || authObj.getSecret() ==null) {
            logBMC.setLogLevel(LogLevel.ERROR);
	    	logBMC.logError(Messages.getString("ERROR.RS-001.MISSING_KEY_OR_SECRET", "RS-001"));

	    	logBMC.setLogLevel(LogLevel.BASIC);
	    	logBMC.logBasic(Messages.getString("ERROR.RS-001.MISSING_KEY_OR_SECRET", "RS-001"));

			throw new RMStudioException("001", Messages.getString("ERROR.RS-001.MISSING_KEY_OR_SECRET"));
		}
	}
}