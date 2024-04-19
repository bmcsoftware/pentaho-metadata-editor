package org.pentaho.pms.bmc.entrypoint.workers;

import java.net.URL;

import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.pentaho.pms.bmc.entrypoint.AuthenticationMonitor;
import org.pentaho.pms.bmc.entrypoint.BMCAuthenticatorForPentaho;
import org.pentaho.pms.bmc.entrypoint.exceptions.IMSAuthenticationException;
import org.pentaho.pms.bmc.entrypoint.exceptions.RMSAuthorizationException;
import org.pentaho.pms.ui.locale.Messages;

public class RMSTokenProcessor {

	public void validateFromRMS( String rmsUrl, String context ) throws Exception {
		try {
	    	URL url = new URIBuilder( rmsUrl ).build().toURL();

			final HttpClient httpClient = HttpClientBuilder.create().build();
			HttpGet getRequest = new HttpGet( url.toURI() );

			getRequest.addHeader("Authorization", "Bearer " + AuthenticationMonitor.envToIMSJwtTokenMap.get( context ) );
			HttpResponse response = httpClient.execute( getRequest );

			if ( response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED ) {//404=url path ,401=token issue, 405=method not allowed
				String errorCodeString = "ERROR.IMS-002.AUTH_FAILURE.UNKNOWN_ERROR";
				String errorCode = "100";

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {//401, token/tenant url path
                	errorCode = "001";
					errorCodeString = "ERROR.RM-001.AUTH_FAILURE.CHECK_TENANT_URL_CREDENTIAL";
				}
				throw new RMSAuthorizationException( Messages.getString(errorCodeString, RMSAuthorizationException.RM_PREFIX + errorCode) );
			}
		} catch (Exception e) {
			throw e;
		}
	}

}