/*
 * ©Copyright 2024 BMC Software. Inc.
 *
 */
package org.pentaho.pms.bmc.entrypoint.workers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.pentaho.pms.bmc.entrypoint.AuthenticationMonitor;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogLevel;

import org.pentaho.pms.bmc.entrypoint.exceptions.IMSAuthenticationException;
import org.pentaho.pms.bmc.entrypoint.exceptions.RMSAuthorizationException;
import org.pentaho.pms.bmc.entrypoint.exceptions.RMStudioException;
import org.pentaho.pms.ui.locale.Messages;
import org.pentaho.pms.ui.util.Const;

public class IMSTokenProcessor {

	private ConcurrentMap<String, String> tenantIdToTokenMap = new ConcurrentHashMap<>();
	private LogChannelInterface log;

	public String fetchIMSToken(String imsUrl, String key, String secret, String tenantId) throws Exception {
	    this.log = new LogChannel( "[IMS]" );
	    log.setLogLevel(LogLevel.ERROR);

		String token = null;
		try {
			JSONObject jsonBody = new JSONObject();
			jsonBody.put("access_key", key);
			jsonBody.put("access_secret_key", secret);
			jsonBody.put("tenant_id", tenantId);

	    	String outputValue = null;
			try {
				URL url = new URIBuilder( imsUrl ).build().toURL();

				final HttpClient httpClient = HttpClientBuilder.create().build();
				final HttpPost httpPost = new HttpPost(url.toURI());

				StringEntity entity = new StringEntity( jsonBody.toString()  );
				entity.setContentType( ContentType.APPLICATION_JSON.toString() );
				httpPost.setEntity( entity );

				HttpResponse response = httpClient.execute( httpPost );
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					String errorCodeString = "ERROR.IMS-002.AUTH_FAILURE.UNKNOWN_ERROR";
					String errorCode = "100";

                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {//401, cred/tenant url path
                    	errorCode = "001";
						errorCodeString = "ERROR.IM-001.AUTH_FAILURE.CHECK_TENANT_URL_CREDENTIAL";
					} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {//404, tenant id wrong
						errorCode = "002";
						errorCodeString = "ERROR.IM-002.AUTH_FAILURE.CHECK_TENANT_ID";
					}
					throw new IMSAuthenticationException( Messages.getString(errorCodeString, IMSAuthenticationException.IM_PREFIX + errorCode) );
				}

				BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
				while ((outputValue = br.readLine()) != null) {
					token = outputValue;
				}
				br.close();
			} catch (IMSAuthenticationException e) {
				throw e ;
			} catch (Exception e) {
				throw e ;
			}
		} catch (IMSAuthenticationException e) {
            throw e;
		} catch (IOException e) {
            throw new RMStudioException( Messages.getString("ERROR.RS-002.TENANT_URL_VALIDATION", RMStudioException.RS_PREFIX + "002" ));
		} catch (Exception e) {
            throw new Exception( Messages.getString("ERROR.RS-100.UNKNOWN_SYSTEM_ERROR", "RS-100", e.getMessage() ), e);
		}
		tenantIdToTokenMap.put(tenantId, new JSONObject(token).getString("token"));
		return new JSONObject(token).getString("json_web_token");
	}

	public String fetchRefreshIMSToken(String imsRefreshUrl, String tenantId) throws Exception {
		String token = null;
		try {
			URL url = new URIBuilder( imsRefreshUrl ).build().toURL();

			JSONObject jsonBody = new JSONObject();
			jsonBody.put("token", tenantIdToTokenMap.get(tenantId));

			final HttpClient httpClient = HttpClientBuilder.create().build();
			final HttpPost httpPost = new HttpPost(url.toURI());
			httpPost.addHeader("Authorization", "Bearer " + tenantIdToTokenMap.get(tenantId) );

			StringEntity entity = new StringEntity( jsonBody.toString(), ContentType.APPLICATION_JSON);
			entity.setContentType( ContentType.APPLICATION_JSON.toString() );
			httpPost.setEntity( entity );

			HttpResponse response = httpClient.execute( httpPost );
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String errorCodeString = "ERROR.IMS-002.AUTH_FAILURE.UNKNOWN_ERROR";
				String errorCode = "100";

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {//401, cred/tenant url path
                	errorCode = "001";
					errorCodeString = "ERROR.IM-001.AUTH_FAILURE.CHECK_TENANT_URL_CREDENTIAL";
				} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {//404, tenant id wrong
					errorCode = "002";
					errorCodeString = "ERROR.IM-002.AUTH_FAILURE.CHECK_TENANT_ID";
				}
				throw new IMSAuthenticationException( Messages.getString(errorCodeString, IMSAuthenticationException.IM_PREFIX + errorCode) );
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

	    	String outputValue = null;
			while ((outputValue = br.readLine()) != null) {
		        token = outputValue;
	        }
		} catch (IOException e) {
            throw new RMStudioException( Messages.getString("ERROR.RS-002.TENANT_URL_VALIDATION", RMStudioException.RS_PREFIX + "002"));
		} catch (Exception e) {
            throw new Exception( Messages.getString("ERROR.RS-100.UNKNOWN_SYSTEM_ERROR", "RS-100", e.getMessage() ), e);
		}
		return new JSONObject(token).getString("json_web_token");
	}
}