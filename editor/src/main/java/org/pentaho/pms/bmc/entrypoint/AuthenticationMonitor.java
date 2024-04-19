/*
 * ©Copyright 2024 BMC Software. Inc.
 */
package org.pentaho.pms.bmc.entrypoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.pms.bmc.entrypoint.beans.TenantInfoList;
import org.pentaho.pms.bmc.entrypoint.beans.TenantInfoList.TenantInfo;
import org.pentaho.pms.bmc.entrypoint.beans.UserSessionObject;
import org.pentaho.pms.bmc.entrypoint.exceptions.IMSAuthenticationException;
import org.pentaho.pms.bmc.entrypoint.workers.IMSTokenProcessor;
import org.pentaho.pms.bmc.entrypoint.workers.RMSTokenProcessor;
import org.pentaho.pms.ui.locale.Messages;
import org.pentaho.di.core.logging.LogLevel;

public class AuthenticationMonitor {
	private IMSTokenProcessor imsTokenProcessor = new IMSTokenProcessor();
	private UserSessionObject authObj;
	private LogChannelInterface log;
	private TenantInfo tenantInfo;

	public static ConcurrentMap<String, String> envToIMSJwtTokenMap = new ConcurrentHashMap<>();

	public AuthenticationMonitor(TenantInfoList.TenantInfo tenantInfo) {
		this.tenantInfo = tenantInfo;
	    this.log = new LogChannel( "[BMC MONITOR]" );
	}

	public void startMonitoringAuthenticationAndAuthorization(UserSessionObject authObj, boolean forceReAuth) throws Exception {
		this.authObj = authObj;
		forcedReAuth.set( forceReAuth );
		if ( forcedReAuth.get() ) {
			while ( !whileLoopExecutionCompleted.get() ) {
				TimeUnit.SECONDS.sleep(1);//during waiting time, the existing executor service is expected to terminate if the forceReAuth flag is set to true
			}
		}
		authenticateAuthorizeAndMonitor();
	}

	private void authenticateAuthorizeAndMonitor() throws Exception {
	    log.setLogLevel(LogLevel.BASIC);

		String imsJwtToken = imsTokenProcessor.fetchIMSToken( tenantInfo.getTenantUrl() + "/ims/api/v1/access_keys/login", authObj.getKey(), authObj.getSecret(), tenantInfo.getTenantId() );
		envToIMSJwtTokenMap.put(tenantInfo.getTenantId(), imsJwtToken);
		log.logBasic(Messages.getString("INFO.IMS_TOKEN_ISSUED", tenantInfo.getTenantId()));

		RMSTokenProcessor rmsTokenProcessor = new RMSTokenProcessor();
		rmsTokenProcessor.validateFromRMS(tenantInfo.getTenantUrl() + "/reportingmetadata/api/v1/authenticate", tenantInfo.getTenantId());

		log.logBasic(Messages.getString("INFO.RMS_TOKEN_VALIDATED", tenantInfo.getTenantId()));

		scheduleIMStokenRefreshCall();
	}

	private volatile AtomicBoolean forcedReAuth = new AtomicBoolean( false );
	private AtomicBoolean whileLoopExecutionCompleted = new AtomicBoolean( true );
	
	private static long REFRESH_TOKEN_THREAD_CONTINUITY_PERIOD = 11 * 60 * 1000;
	private static long REFRESH_TOKEN_TIME_PERIOD = 10 * 60 * 1000;
	
	private void scheduleIMStokenRefreshCall() throws Exception {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		AtomicLong storedTime = new AtomicLong(System.currentTimeMillis());

		executorService.submit(new Runnable() {
			@Override
			public void run() {
				log.logBasic(Messages.getString("INFO.TOKEN_REFRESH_THREAD_STARTED", tenantInfo.getTenantId(), "10 min"));

				forcedReAuth.set( false );
				whileLoopExecutionCompleted.set( false );
				AtomicInteger authAttemptAfterFailure = new AtomicInteger( 0 );
				while ( !whileLoopExecutionCompleted.get() && !forcedReAuth.get() && System.currentTimeMillis() - storedTime.get() < REFRESH_TOKEN_THREAD_CONTINUITY_PERIOD ) {//time difference is less than 11 min
					if (System.currentTimeMillis() - storedTime.get() > REFRESH_TOKEN_TIME_PERIOD) {//after 10 min, make refresh request
						try {
							String refreshedIMSJwtToken = imsTokenProcessor.fetchRefreshIMSToken(  tenantInfo.getTenantUrl() + "/ims/api/v1/auth/tokens", tenantInfo.getTenantId() );

							log.logBasic(Messages.getString("INFO.TOKEN_REFRESHED", tenantInfo.getTenantId(), new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format( new Date() )));
							envToIMSJwtTokenMap.put(tenantInfo.getTenantId(), refreshedIMSJwtToken);
							//reset current time variable, while loop starts all over again for next cycle of refresh
							storedTime.set( System.currentTimeMillis() );
						} catch (IMSAuthenticationException e) {
							//if failed, try again for 3 attempts??
							authAttemptAfterFailure.getAndIncrement();
							log.setLogLevel(LogLevel.BASIC);
							log.logBasic(Messages.getString("WARN.IM-003.REFRESH_TOKEN_FAILED_FOR_ATTEMPT", "IM-003:", String.valueOf(authAttemptAfterFailure.get())));
							long localLoopTime = System.currentTimeMillis();
							while ((System.currentTimeMillis() - localLoopTime) < 10*1000) {
								//wait for 10 seconds for each attempt
							}
							if ( authAttemptAfterFailure.get() == 3) {
							    log.setLogLevel(LogLevel.ERROR);
								log.logError(Messages.getString("ERROR.IM-004.TOKEN_REFRESH_FAILED_FOR_ALL_ATTEMPTS", "IM-004:"));
								
								whileLoopExecutionCompleted.set( true );
								forcedReAuth.set( true );
								envToIMSJwtTokenMap.remove( tenantInfo.getTenantId() );
							}
						} catch (Exception e) {
						    log.setLogLevel(LogLevel.ERROR);
							log.logError(Messages.getString("ERROR.RS-100.UNKNOWN_SYSTEM_ERROR", "RS-100:"), e);

							whileLoopExecutionCompleted.set( true );
							forcedReAuth.set( true );
							envToIMSJwtTokenMap.remove( tenantInfo.getTenantId() );
							log.setLogLevel(LogLevel.BASIC);
							log.logBasic(Messages.getString("ERROR.RS-100.UNKNOWN_SYSTEM_ERROR", "RS-100:"));
						}
					}
				}
				whileLoopExecutionCompleted.set( true );
				log.setLogLevel(LogLevel.BASIC);
				log.logBasic(Messages.getString("INFO.TOKEN_REFRESH_THREAD_STOPPED", tenantInfo.getTenantId(), "10 min"));
			}
		});
	}
}
