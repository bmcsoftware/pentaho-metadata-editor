package org.pentaho.pms.bmc.entrypoint.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.pentaho.pms.ui.locale.Messages;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.LogLevel;

public class TenantInfoList {

	public final static String CONFIG_BASE_PATH = "./bmcauthconfiguration/";
	public final static String ENV_FILE_NAME = "env.properties";
	public final static String PROFILE_FILE_NAME = "profile.properties";
	public final static String DEFAULT_CONTEXT = "DEFAULT";

	public final static String AR_HOST = "AR_HOST";
	public final static String AR_RPC_PORT = "AR_RPC_PORT";
	public final static String REST_ENDPOINT = "REST_ENDPOINT";
	public final static String TENANT_URL = "TENANT_URL";
	public final static String TENANT_ID = "TENANT_ID";
	public final static String ENV_ALIAS = "ENV_ALIAS";
	
	private List<TenantInfoList.TenantInfo> tenantInfoList;
	private LogChannelInterface logBMC = null;
	
	public TenantInfoList() {
		tenantInfoList = new ArrayList<>();
		logBMC = new LogChannel( "[" + this.getClass().getName() + "]" );
	}
	
	public List<TenantInfo> getTenantInfoList() {
		return tenantInfoList;
	}

	public String getTenantIdFromNonDefaultEnvFileName(String fileName) {
		if (!fileName.equalsIgnoreCase( ENV_FILE_NAME ) && fileName.endsWith( "-" + ENV_FILE_NAME ) ) {
			return fileName.substring(0, fileName.indexOf("-" + ENV_FILE_NAME));
		}
		return "";
	}
	
	public void addToList(String envFileName, Properties envProperties) {
		String host = envProperties.getProperty( AR_HOST );
		String port = envProperties.getProperty( AR_RPC_PORT );
		String restEndPoint = envProperties.getProperty( REST_ENDPOINT );
		String tenantUrl = envProperties.getProperty( TENANT_URL );
		String tenantId = envProperties.getProperty( TENANT_ID );
		String envAlias = envProperties.getProperty( ENV_ALIAS );
		
		boolean isDefault = true;
		if (!envFileName.equalsIgnoreCase( ENV_FILE_NAME ) && envFileName.contains( "-" + ENV_FILE_NAME ) ) {
			String prefix = envFileName.substring(0, envFileName.indexOf("-" + ENV_FILE_NAME));
			if (!prefix.equals(tenantId)) {
				logBMC.setLogLevel(LogLevel.ERROR);
				logBMC.logError(Messages.getString("ERROR.RS-005.CONFIG_FILE_NAME_PREFIX_MISMATCH", "RS-005", tenantId));
			}
			isDefault = false;
		}
		if (isTenantInfoNew( envAlias, tenantId, tenantUrl )) {
			try {
			    TenantInfo tenantInformation = new TenantInfo( host, port, restEndPoint, tenantId, tenantUrl, isDefault, envAlias );
			    tenantInfoList.add( tenantInformation );
			} catch(Exception t) {
				t.printStackTrace();
			}
		} else {
			//old tenant info, check isDefault, check exact 3 attr match
			if (isTenantInfoOldWithDifferentIsDefault( isDefault, envAlias, tenantId, tenantUrl ) ) {
				tenantInfoList.parallelStream().filter( info -> (info.envAlias.equalsIgnoreCase(envAlias) 
						&& info.tenantId.equalsIgnoreCase(tenantId) 
						&& info.tenantUrl.equalsIgnoreCase(tenantUrl)) )
				          .forEach(existingInfo -> {
				        	  if (existingInfo.isDefault) {
				        		  existingInfo.isDefault = false;
				        	  } else {
				        		  existingInfo.isDefault = true;
				        	  }
				          });
			} else {
				tenantInfoList.parallelStream()
				    .filter(info -> (info.envAlias.equalsIgnoreCase(envAlias) && info.tenantId.equalsIgnoreCase(tenantId) && info.tenantUrl.equalsIgnoreCase(tenantUrl)) )
				    .forEach(a -> {
				    	a.arHost = host;
				    	a.arRestEndpoint = restEndPoint;
				    	a.arRpcPort = port;
				    });
			}
		}
	}

	public void removeTenantInfoFromList(String tenantId) {
		List<TenantInfo> tenantInfoListCollection = tenantInfoList.stream().filter(f -> f.tenantId.equals(tenantId) && !f.isDefault).collect(Collectors.toList());
		if (tenantInfoListCollection.size() == 1) {
			TenantInfo infoForDeletion = tenantInfoListCollection.get(0);
			tenantInfoList.remove( infoForDeletion );
		}
	}

	private boolean isTenantInfoOldWithDifferentIsDefault(boolean isDefault, String envAlias, String tenantId, String tenantUrl) {
		return tenantInfoList.parallelStream()
				.filter(info -> (info.envAlias.equalsIgnoreCase(envAlias) && info.tenantId.equalsIgnoreCase(tenantId) && info.tenantUrl.equalsIgnoreCase(tenantUrl)) && info.isDefault != isDefault)
				.collect(Collectors.toList()).isEmpty() ? Boolean.FALSE : Boolean.TRUE;
	}

	public boolean isTenantInfoNew(String envAlias, String tenantId, String tenantUrl) {
		return tenantInfoList.parallelStream()
				.filter(info -> (info.envAlias.equalsIgnoreCase(envAlias) || info.tenantId.equalsIgnoreCase(tenantId) || info.tenantUrl.equalsIgnoreCase(tenantUrl)) ).collect(Collectors.toList()).isEmpty() 
				? Boolean.TRUE : Boolean.FALSE;
	}
	
	public class TenantInfo {
		private String arHost;
		private String arRpcPort;
		private String arRestEndpoint;
		private String tenantId;
		private String tenantUrl;
		private String connectionUrl;
		private boolean isDefault;
		private String envAlias;
		
		public TenantInfo(String arHost, String arRpcPort, String arRestEndpoint, String tenantId, String tenantUrl, boolean isDefault, String envAlias) {
			this.connectionUrl = (arHost != null && !arHost.isEmpty() && arRpcPort != null && !arRpcPort.isEmpty() && arRestEndpoint != null && !arRestEndpoint.isEmpty()) 
					? "jdbc:arserver://" + arHost + ":" + arRpcPort + ";mode=new;JdbcRestEndpointURL=" + arRestEndpoint	: "";
			this.arHost = arHost;
			this.arRpcPort = arRpcPort;
			this.arRestEndpoint = arRestEndpoint;
			this.tenantId = tenantId;
			this.tenantUrl = tenantUrl;
			this.isDefault = isDefault;
			this.envAlias = envAlias;
		}
		
		public boolean isDefaultTenant() {
			return isDefault;
		}
		public String getConnectionUrl() {
			return connectionUrl;
		}
		public String getArHost() {
			return arHost;
		}
		public String getArRpcPort() {
			return arRpcPort;
		}
		public String getArRestEndpoint() {
			return arRestEndpoint;
		}
		public String getTenantId() {
			return tenantId;
		}
		public String getTenantUrl() {
			return tenantUrl;
		}
		public String getEnvAlias() {
			return envAlias;
		}
	}
}
