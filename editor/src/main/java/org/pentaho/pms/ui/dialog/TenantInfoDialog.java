package org.pentaho.pms.ui.dialog;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.ui.core.widget.TableViewTwoRows;

import org.pentaho.pms.bmc.entrypoint.beans.TenantInfoList.TenantInfo;
import org.pentaho.pms.ui.locale.Messages;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.pms.bmc.entrypoint.BMCAuthenticatorForPentaho;
import org.pentaho.pms.bmc.entrypoint.beans.TenantInfoList;
import org.pentaho.pms.bmc.entrypoint.beans.UserSessionObject;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.pms.bmc.entrypoint.exceptions.IMSAuthenticationException;
import org.pentaho.pms.bmc.entrypoint.common.StaticMethodsUtility;

public class TenantInfoDialog extends Dialog {
	private static Class<?> PKG = TenantInfoDialog.class;
	private Button checkBox, okButton, cancelButton;
	private Listener lsOK, lsCancel;
	private Shell shell;
	private PropsUI props;
	private int rights;
	private String title;
	private String message;
	private boolean readOnly;
	private FormData fdFields;
	private RowMetaAndData publishConfigArgs, arConfigArgs;
	private TableViewTwoRows tableViewARConfig, tableViewPublishConfig;
	private Image shellImage;
	public Table table;
	private int rows;
	private ColumnInfo[] columns;
	private TableColumn[] tablecolumn;
	protected Shell shlConfiguredata;
	private CCombo envActionType;
	private CCombo tenantAliasDropDown;
	private TenantInfoList tenantInfoList;
	private LogChannelInterface logBMC = null;
	private Text userKey, userSecret;
	private Label keyLabel, secretLabel, information;
	private SashForm sash;
	private String currentKeyValue;
	private String currentSecretValue;
	private Group tableGroup, buttonGroup;

	public TenantInfoDialog(Shell parent, int style, RowMetaAndData publishConfigArgs, RowMetaAndData arConfigArgs) {
		super(parent, style);

		logBMC = new LogChannel( "[" + this.getClass().getName() + "]" );
		props = PropsUI.getInstance();
		readOnly = false;
		this.publishConfigArgs = publishConfigArgs;
		this.arConfigArgs = arConfigArgs;
		this.tenantInfoList = BMCAuthenticatorForPentaho.getInstance().getAllTenantInfo();
		title = BaseMessages.getString(PKG, "EnterStringsDialog.Title");
		message = BaseMessages.getString(PKG, "EnterStringsDialog.Message");
	}

	enum ACTIONS {
		ADD, EDIT, DELETE;
	}

	public RowMetaAndData open() {
		Shell parent = getParent();
		Display display = parent.getDisplay();
		createContents();
		shlConfiguredata.open();
		shlConfiguredata.pack();

		while (!shlConfiguredata.isDisposed()) {
		    if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return null;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	private void createContents() {
		shlConfiguredata = new Shell();
		shlConfiguredata.setSize(500, 500);
		sash = new SashForm(shlConfiguredata, SWT.HORIZONTAL);
		shlConfiguredata.setText("Configure Tenant Details");
		shlConfiguredata.setLayout(new GridLayout(1, false));

		createControls();

		tableGroup = new Group(sash, SWT.SHADOW_ETCHED_IN);
		tableGroup.setText(Messages.getString("TenantInfoDialog.UI.CONFIG_DETAILS_TITLE"));
		tableGroup.setLayout(new GridLayout());

		createARConfigDataDisplayWidget();
		createPublishConfigDisplayWidget();

		createButtonGroupControls(tableGroup);

		sash.setWeights(new int[] {1, 2});

		createEnvActionTypeListener();
		createEnvUrlSelectionListener();
		createCheckboxListener();
		createButtonListeners();

		getDataIntoARConfigWidget();
		getDataIntoPublishConfigWidget();
	}

	private void createEnvUrlSelectionListener() {
		information.setText("");
		tenantAliasDropDown.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected( SelectionEvent selectionEvent ) {
				TenantInfo chosenInfo = tenantInfoList.getTenantInfoList().stream()
						.filter(chosen -> chosen.getEnvAlias().equalsIgnoreCase(tenantAliasDropDown.getText())).findFirst()
						.get();
				populateRowFieldData(chosenInfo);
			}
		});
	}

	private void createButtonListeners() {
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
			    dispose();
			}
		});

		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				information.setText("");
				if (envActionType.getText().equalsIgnoreCase(ACTIONS.DELETE.name())) {
					checkBox.setEnabled(false);
					if ( checkBox.getSelection() ) {
						logBMC.setLogLevel(LogLevel.BASIC);
						logBMC.logBasic(Messages.getString("ERROR.RS-006.CANNOT_DEL_DEF_CONFIGURATION", "RS-006", BMCAuthenticatorForPentaho.getInstance().getDefaultTenantInfo().getTenantUrl()));
						//prevent deletion as this tenant is default
						information.setText( Messages.getString("TenantInfoDialog.UI.CANNOT_DEL_DEF_CONFIGURATION") );
					} else {
						TenantInfo tenantInfo = tenantInfoList.getTenantInfoList().stream()
						                        .filter(chosen -> chosen.getEnvAlias().equalsIgnoreCase(tenantAliasDropDown.getText()))
						                        .findFirst().get();

						String enteredKey = userKey.getText();
						String enteredSecret = userSecret.getText();
						String fileToBeDeleted = TenantInfoList.CONFIG_BASE_PATH + tenantInfo.getTenantId() + "-" + TenantInfoList.ENV_FILE_NAME;

						try {
							if ( authenticateUser(tenantInfo.getTenantId(), enteredKey, enteredSecret) ) {
								StaticMethodsUtility.deleteFile( new File( fileToBeDeleted ) );
								//delete as this is non-default
							    information.setText( Messages.getString("TenantInfoDialog.UI.TENANT_DELETE_SUCCESS", tenantInfo.getEnvAlias()) );
							    try {
							        Thread.sleep( 1000 );
							    } catch (Exception ie) {

							    }
								dispose();
							} else {
								information.setText( Messages.getString("TenantInfoDialog.UI.AUTH_FAILED") );
							}
						} catch(IMSAuthenticationException e) {
							information.setText( Messages.getString("TenantInfoDialog.UI.AUTH_FAILED") );
						} catch(Exception e) {
							logBMC.setLogLevel(LogLevel.ERROR);
							logBMC.logBasic(Messages.getString("ERROR.RS-007.CANNOT_DEL_CONFIGURATION_FILE", fileToBeDeleted, e.getMessage()));
						    information.setText( Messages.getString("TenantInfoDialog.UI.TENANT_DELETE_FAILED", tenantInfo.getEnvAlias()) );
						}
					}//delete ends
				} else {//edit/add
					checkBox.setEnabled(true);
					// TO-DO, if checkbox is selected, then do enable key/secret
					String tenantId = "";
					String tenantUrl = "";
					String arHost = "";
					String arRestEndpoint = "";
					String arRpcPort = "";
					String envAlias = "";

					for (int i = 0; i < publishConfigArgs.getRowMeta().size(); i++) {
						ValueMetaInterface valueMeta = publishConfigArgs.getRowMeta().getValueMeta(i);
						Object valueData = publishConfigArgs.getData()[i];

						TableItem[] items = tableViewPublishConfig.table.getItems();
						for (int r = 0; r < items.length; r++) {
							if (valueMeta.getName().equals(items[r].getText(1))) {
								if ( TenantInfoList.TENANT_ID.equalsIgnoreCase(valueData + "") ) {
									tenantId = items[r].getText(2).trim();
								}
								if ( TenantInfoList.TENANT_URL.equalsIgnoreCase(valueData + "") ) {
									tenantUrl = items[r].getText(2).trim();
								}
								if ( TenantInfoList.ENV_ALIAS.equalsIgnoreCase(valueData + "") ) {
									envAlias = items[r].getText(2).trim();
								}
							}
						}
					}

					for (int i = 0; i < arConfigArgs.getRowMeta().size(); i++) {
						ValueMetaInterface valueMeta = arConfigArgs.getRowMeta().getValueMeta(i);
						Object valueData = arConfigArgs.getData()[i];

						TableItem[] items = tableViewARConfig.table.getItems();
						for (int r = 0; r < items.length; r++) {
							if (valueMeta.getName().equals(items[r].getText(1))) {
								if ( TenantInfoList.AR_HOST.equalsIgnoreCase(valueData + "") ) {
									arHost = items[r].getText(2).trim();
								}
								if ( TenantInfoList.REST_ENDPOINT.equalsIgnoreCase(valueData + "") ) {
									arRestEndpoint = items[r].getText(2).trim();
								}
								if ( TenantInfoList.AR_RPC_PORT.equalsIgnoreCase(valueData + "") ) {
									arRpcPort = items[r].getText(2).trim();
								}
							}
						}
					}

					String enteredKey = userKey.getText();
					String enteredSecret = userSecret.getText();
					AtomicBoolean keepProgressing = new AtomicBoolean( false );
					if ( (envActionType.getText().equalsIgnoreCase(ACTIONS.EDIT.name()) ) ) {
						//edit a default env, first step is to add to file, then java object is used for authentication
						try {
						    editEnvironment(tenantId, tenantUrl, arHost, arRestEndpoint, arRpcPort, envAlias, keepProgressing);
						    if ( keepProgressing.get() ) {
							    information.setText( Messages.getString("TenantInfoDialog.UI.TENANT_EDIT_SUCCESS", envAlias) );
						    }
						} catch (IMSAuthenticationException e) {
							information.setText( Messages.getString("TenantInfoDialog.UI.AUTH_FAILED") );
						} catch (Exception e) {
							logBMC.setLogLevel(LogLevel.ERROR);
							logBMC.logError( Messages.getString("ERROR.RS-100.UNKNOWN_SYSTEM_ERROR", "RS-100:"), e );
							information.setText( Messages.getString("TenantInfoDialog.UI.UNKNOWN_SYSTEM_ERROR") );
						}
					} else {
						//Add action
						if (tenantId.isEmpty() || tenantUrl.isEmpty() || envAlias.isEmpty()) {
							information.setText( Messages.getString("TenantInfoDialog.UI.TENANT_MISSING_FIELDS", envAlias) );
						} else {
							try {
							    addEnvironment(tenantId, tenantUrl, arHost, arRestEndpoint, arRpcPort, envAlias, keepProgressing);
							    if ( keepProgressing.get() ) {
								    Thread.sleep( 2000 );
									keepProgressing.set( authenticateUser(tenantId, enteredKey, enteredSecret) );
							    }
							} catch (IMSAuthenticationException e) {
								keepProgressing.set( false );
								String fileToBeDeleted = TenantInfoList.CONFIG_BASE_PATH + tenantId + "-" + TenantInfoList.ENV_FILE_NAME;
								try {
						            StaticMethodsUtility.deleteFile( new File ( fileToBeDeleted ) );
						            information.setText( Messages.getString( "TenantInfoDialog.UI.AUTH_FAILED" ) );
								} catch(IOException ee) {
									keepProgressing.set( false );
									information.setText(Messages.getString("ERROR.RS-007.CANNOT_DEL_CONFIGURATION_FILE", fileToBeDeleted, e.getMessage()));
								}
							} catch (Exception e) {
								keepProgressing.set( false );
								String fileToBeDeleted = TenantInfoList.CONFIG_BASE_PATH + tenantId + "-" + TenantInfoList.ENV_FILE_NAME;
								try {
						            StaticMethodsUtility.deleteFile( new File ( fileToBeDeleted ) );
									information.setText( Messages.getString( "TenantInfoDialog.UI.UNKNOWN_SYSTEM_ERROR" ) );
								} catch(IOException ee) {
									keepProgressing.set( false );
									information.setText(Messages.getString("ERROR.RS-007.CANNOT_DEL_CONFIGURATION_FILE", fileToBeDeleted, e.getMessage()));
								}
							}
							try {
							    if (keepProgressing.get() && checkBox.getSelection()) {
									if (arHost.isEmpty() || arRestEndpoint.isEmpty() || arRpcPort.isEmpty()) {
										keepProgressing.set( Boolean.TRUE );
										information.setText( Messages.getString("TenantInfoDialog.UI.NON_DEF_ADDED") );
									} else {
								        editNonDefaultToADefaultEnv(tenantId, tenantUrl, arHost, arRestEndpoint, arRpcPort, envAlias, keepProgressing);
									    information.setText( Messages.getString("TenantInfoDialog.UI.TENANT_ADD_SUCCESS", envAlias) );
									}
								}
							    if (keepProgressing.get() && !checkBox.getSelection()) {
									information.setText( Messages.getString("TenantInfoDialog.UI.TENANT_ADD_SUCCESS", envAlias) );
								}
							} catch (Exception e) {
								keepProgressing.set( false );
							}
						}
					}

					if ( keepProgressing.get() ) {
						try {
						Thread.sleep( 2000 );//wait for 2 seconds, user gets this time to read the message printed on the popup dialog
						} catch(Exception e) {
							//nothing to do
						}
					    dispose();
					}
				}
			}

			private void addEnvironment(String tenantId, String tenantUrl, String arHost, String arRestEndpoint,
					String arRpcPort, String envAlias, AtomicBoolean keepProgressing) throws Exception {

				if ( tenantInfoList.isTenantInfoNew( envAlias, tenantId, tenantUrl ) ) {//adding a new config
					try (FileOutputStream fileOutputStream = new FileOutputStream( TenantInfoList.CONFIG_BASE_PATH + tenantId + "-" + TenantInfoList.ENV_FILE_NAME ) ) {
						Properties properties = new Properties();
						properties.setProperty( TenantInfoList.AR_HOST, arHost );
						properties.setProperty( TenantInfoList.AR_RPC_PORT, arRpcPort );
						properties.setProperty( TenantInfoList.REST_ENDPOINT, arRestEndpoint );
						properties.setProperty( TenantInfoList.TENANT_ID, tenantId );
						properties.setProperty( TenantInfoList.TENANT_URL, tenantUrl );
						properties.setProperty( TenantInfoList.ENV_ALIAS, envAlias );

						properties.store(fileOutputStream, "Tenant configuration for non-default tenant");
						keepProgressing.set( true );
					} catch(Exception f) {
						keepProgressing.set( false );
						information.setText( Messages.getString("TenantInfoDialog.UI.WRITE_FAILED_FOR_NON_DEF_ENV") );
					}
				} else {
					keepProgressing.set( false );
					information.setText( Messages.getString("TenantInfoDialog.UI.FOUND_COMBO_ALIAS_ID_URL") );
				}
			}

			private void editNonDefaultToADefaultEnv(String tenantId, String tenantUrl, String arHost, String arRestEndpoint,
					String arRpcPort, String envAlias, AtomicBoolean keepProgressing) {
				String enteredKey = userKey.getText();
				String enteredSecret = userSecret.getText();

				boolean recreateProfileEntry = StaticMethodsUtility.profileFileIsUsed();
				Properties newDefaultProps = new Properties();

				try (FileOutputStream out = new FileOutputStream( TenantInfoList.CONFIG_BASE_PATH + TenantInfoList.PROFILE_FILE_NAME )) {
					if ( recreateProfileEntry ) {
						newDefaultProps.setProperty("KEY", enteredKey);
						newDefaultProps.setProperty("SECRET", enteredSecret);
						newDefaultProps.store(out, null);
					} else {
						information.setText( Messages.getString("TenantInfoDialog.UI.FILE_EXCEPTION", (TenantInfoList.CONFIG_BASE_PATH + TenantInfoList.PROFILE_FILE_NAME)) );
					}

					tenantInfoList.getTenantInfoList().stream().filter(f -> f.isDefaultTenant() ).forEach(defaultTenantInfo -> {
							//copy current default configuration and make it non-default, <tenantid>-env.properties
						String mvNameOfOrigDefConfigFile = TenantInfoList.CONFIG_BASE_PATH + defaultTenantInfo.getTenantId() + "-"+ TenantInfoList.ENV_FILE_NAME;

						try (FileOutputStream fileOutputStream = new FileOutputStream( mvNameOfOrigDefConfigFile )) {
							setNewEnvPropFileWithExistingDefaultProp(defaultTenantInfo,	fileOutputStream);

									//Overwrite the existing default configuration file
						    overwriteExistingEnvFileWithNewDefault(tenantId, tenantUrl, arHost,
											arRestEndpoint, arRpcPort, envAlias, keepProgressing);
						} catch (Exception e1) {
							keepProgressing.set( Boolean.FALSE );
						}
					});
				} catch(Exception fe) {
					keepProgressing.set( Boolean.FALSE );
					information.setText( Messages.getString("TenantInfoDialog.UI.WRITE_FAILED_FOR_PROFILE") );
				}
			}

			private void editEnvironment(String tenantId, String tenantUrl, String arHost, String arRestEndpoint,
					String arRpcPort, String envAlias, AtomicBoolean keepProgressing) {

				if ( tenantInfoList.isTenantInfoNew(envAlias, tenantId, tenantUrl) ) {//something changed between envAlias, tenantId, tenantUrl
					keepProgressing.set( false );
					information.setText( Messages.getString("TenantInfoDialog.UI.MISSING_COMBO_ALIAS_ID_URL") );
				} else {
					//authenticate
					try {
						if (authenticateUser(tenantId, userKey.getText(), userSecret.getText()) ) {
							if (checkBox.getSelection() && !BMCAuthenticatorForPentaho.getInstance().getDefaultTenantInfo().getTenantId().equalsIgnoreCase( tenantId ) ) {
							    editNonDefaultToADefaultEnv(tenantId, tenantUrl, arHost, arRestEndpoint, arRpcPort, envAlias, keepProgressing);
							} else if (checkBox.getSelection() && BMCAuthenticatorForPentaho.getInstance().getDefaultTenantInfo().getTenantId().equalsIgnoreCase( tenantId ) ) {
								try (FileOutputStream out = new FileOutputStream( TenantInfoList.CONFIG_BASE_PATH + TenantInfoList.ENV_FILE_NAME )) {
								    Properties properties = new Properties();
									properties.setProperty( TenantInfoList.AR_HOST, arHost );
									properties.setProperty( TenantInfoList.AR_RPC_PORT, arRpcPort );
									properties.setProperty( TenantInfoList.REST_ENDPOINT, arRestEndpoint);
									properties.setProperty( TenantInfoList.TENANT_ID, tenantId );
									properties.setProperty( TenantInfoList.TENANT_URL, tenantUrl );
									properties.setProperty( TenantInfoList.ENV_ALIAS, envAlias );

									properties.store(out, null);

									keepProgressing.set( Boolean.TRUE );
								} catch(Exception fe) {
									keepProgressing.set( false );
									information.setText( Messages.getString("TenantInfoDialog.UI.WRITE_FAILED_FOR_NON_DEF_ENV") );
								}
							} else {//edit the base env file
								try (FileOutputStream out = new FileOutputStream( TenantInfoList.CONFIG_BASE_PATH + tenantId + "-" + TenantInfoList.ENV_FILE_NAME )) {
								    Properties properties = new Properties();
									properties.setProperty( TenantInfoList.AR_HOST, arHost );
									properties.setProperty( TenantInfoList.AR_RPC_PORT, arRpcPort );
									properties.setProperty( TenantInfoList.REST_ENDPOINT, arRestEndpoint);
									properties.setProperty( TenantInfoList.TENANT_ID, tenantId );
									properties.setProperty( TenantInfoList.TENANT_URL, tenantUrl );
									properties.setProperty( TenantInfoList.ENV_ALIAS, envAlias );

									properties.store(out, null);

									keepProgressing.set( Boolean.TRUE );
								} catch(Exception fe) {
									keepProgressing.set( false );
									information.setText( Messages.getString("TenantInfoDialog.UI.WRITE_FAILED_FOR_NON_DEF_ENV") );
								}
							}
						}
					} catch (IMSAuthenticationException e) {
						keepProgressing.set( false );
						information.setText( Messages.getString("TenantInfoDialog.UI.AUTH_FAILED") );
					} catch(Exception e) {
						keepProgressing.set( false );
						logBMC.setLogLevel(LogLevel.ERROR);
						logBMC.logBasic(Messages.getString("TenantInfoDialog.UI.TENANT_EDIT_FAILED", envAlias));
					    information.setText( Messages.getString("TenantInfoDialog.UI.TENANT_EDIT_FAILED", envAlias ) );
					}
				}
			}

			private boolean authenticateUser(String tenantId, String enteredKey, String enteredSecret) throws Exception {
				boolean authenticated = false;
				UserSessionObject authObj = new UserSessionObject();
				authObj.setAttribute( "key", enteredKey );
				authObj.setAttribute( "secret", enteredSecret );
				//get tenantid, url for further call
				BMCAuthenticatorForPentaho bmcAuthenticator = BMCAuthenticatorForPentaho.getInstance();
				bmcAuthenticator.monitorAuthentication( bmcAuthenticator.getTenantInfoBasedOnTenantId( tenantId ), authObj, false );
				authenticated = true;
				return authenticated;
			}

			private void overwriteExistingEnvFileWithNewDefault(String tenantId, String tenantUrl, String arHost,
					String arRestEndpoint, String arRpcPort, String envAlias, AtomicBoolean keepProgressing) {
				Properties newDefaultProps = new Properties();
				String configFileName = TenantInfoList.ENV_FILE_NAME;
				try (FileOutputStream out = new FileOutputStream( TenantInfoList.CONFIG_BASE_PATH + configFileName )) {
					newDefaultProps.setProperty( TenantInfoList.AR_HOST, arHost );
					newDefaultProps.setProperty( TenantInfoList.AR_RPC_PORT, arRpcPort );
					newDefaultProps.setProperty( TenantInfoList.REST_ENDPOINT, arRestEndpoint );
					newDefaultProps.setProperty( TenantInfoList.TENANT_ID, tenantId );
					newDefaultProps.setProperty( TenantInfoList.TENANT_URL, tenantUrl );
					newDefaultProps.setProperty( TenantInfoList.ENV_ALIAS, envAlias );

					newDefaultProps.store(out, null);

					//delete the file with that tenant extension
			        StaticMethodsUtility.deleteFile( new File ( TenantInfoList.CONFIG_BASE_PATH + tenantId + "-" + configFileName ) );
			        keepProgressing.set( true );
				} catch (Exception e1) {
					keepProgressing.set( false );
				}
			}

			private void setNewEnvPropFileWithExistingDefaultProp(TenantInfo defaultTenantInfo,
					FileOutputStream fileOutputStream) throws IOException {
				Properties existingDefaultProps = new Properties();
				existingDefaultProps.setProperty( TenantInfoList.AR_HOST, defaultTenantInfo.getArHost() );
				existingDefaultProps.setProperty( TenantInfoList.AR_RPC_PORT, defaultTenantInfo.getArRpcPort() );
				existingDefaultProps.setProperty( TenantInfoList.REST_ENDPOINT, defaultTenantInfo.getArRestEndpoint() );
				existingDefaultProps.setProperty( TenantInfoList.TENANT_ID, defaultTenantInfo.getTenantId() );
				existingDefaultProps.setProperty( TenantInfoList.TENANT_URL, defaultTenantInfo.getTenantUrl() );
				existingDefaultProps.setProperty( TenantInfoList.ENV_ALIAS, defaultTenantInfo.getEnvAlias() );

				existingDefaultProps.store(fileOutputStream, "Tenant configuration for non-default tenant");
			}
		});
	}

	private void createEnvActionTypeListener() {
		envActionType.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				information.setText("");
				if (tenantAliasDropDown.getSelectionIndex() != -1) {
					tenantAliasDropDown.deselectAll();
				}
				populateRowFieldData(null);
			}
		});
	}

	private void createCheckboxListener() {
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				information.setText("");
				enableKeySecretFields();
			}
		});
	}

	private void createButtonGroupControls(Group tableGroup) {
		buttonGroup = new Group(tableGroup, SWT.NONE);
	    GridLayout buttonlayout = new GridLayout();
	    buttonlayout.numColumns = 3;
	    buttonGroup.setLayout( buttonlayout );
	    GridData buttondata = new GridData(GridData.FILL_BOTH);
	    buttondata.horizontalSpan = 3;
	    buttonGroup.setLayoutData( buttondata );

		okButton = new Button(buttonGroup, SWT.PUSH);
		//shlConfiguredata.setDefaultButton(btnAction);
		okButton.setText(Messages.getString( "General.USER_OK" ));
		okButton.setLayoutData(new GridData());

		cancelButton = new Button(buttonGroup, SWT.PUSH);
		cancelButton.setText(Messages.getString( "General.USER_CANCEL" ));
		cancelButton.setLayoutData(new GridData());

		information = new Label(buttonGroup, SWT.LEFT);
		information.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	private void createPublishConfigDisplayWidget() {
		int FieldsRows = publishConfigArgs.getRowMeta().size();
		ColumnInfo[] colinf = new ColumnInfo[] {
				new ColumnInfo(Messages.getString( "TenantInfoDialog.PUBLISH.CONFIG_NAME" ),
						ColumnInfo.COLUMN_TYPE_TEXT, false, true),
				new ColumnInfo(Messages.getString( "TenantInfoDialog.CONFIG_VAL" ),
						ColumnInfo.COLUMN_TYPE_TEXT, false, readOnly, 250) };

		tableViewPublishConfig = new TableViewTwoRows(Variables.getADefaultVariableSpace(), tableGroup,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colinf, FieldsRows, null, props);
		tableViewPublishConfig.setReadonly(readOnly);
	}

	private void createARConfigDataDisplayWidget() {
		int FieldsRows = arConfigArgs.getRowMeta().size();
		ColumnInfo[] colinf = new ColumnInfo[] {
				new ColumnInfo(Messages.getString( "TenantInfoDialog.AR.CONFIG_NAME" ),
						ColumnInfo.COLUMN_TYPE_TEXT, false, true),
				new ColumnInfo(Messages.getString( "TenantInfoDialog.CONFIG_VAL" ),
						ColumnInfo.COLUMN_TYPE_TEXT, false, readOnly, 250) };

		tableViewARConfig = new TableViewTwoRows(Variables.getADefaultVariableSpace(), tableGroup,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colinf, FieldsRows, null, props);
		tableViewARConfig.setReadonly(readOnly);
	}

	private void createControls() {
		Group envActionGroup = new Group(sash, SWT.NONE);
		envActionGroup.setText(Messages.getString("TenantInfoDialog.UI.ENV_ACTION"));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		envActionGroup.setLayout(layout);

		envActionType = new CCombo(envActionGroup, SWT.BORDER);
		envActionType.setItems(new String[] { ACTIONS.ADD.name(), ACTIONS.DELETE.name(), ACTIONS.EDIT.name() });
		envActionType.select(0);
		GridData parentLayoutGrid = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		parentLayoutGrid.horizontalSpan = 1;
		envActionType.setLayoutData( parentLayoutGrid );

		Label dropDownLabel = new Label( envActionGroup, SWT.LEFT );
		dropDownLabel.setText(Messages.getString("TenantInfoDialog.UI.ENV"));

		checkBox = new Button(envActionGroup, SWT.CHECK);
		checkBox.setText("Is Default?");

		tenantAliasDropDown = new CCombo(envActionGroup, SWT.BORDER);
		List<String> list = tenantInfoList.getTenantInfoList().stream()
				.map(info -> info.getEnvAlias())
				.collect(Collectors.toList());
		tenantAliasDropDown.setItems(list.toArray(new String[list.size()]));
		GridData tenantUrlGridData = new GridData(GridData.FILL_HORIZONTAL);
		tenantAliasDropDown.setLayoutData( tenantUrlGridData );
		tenantAliasDropDown.setEnabled( false );

		keyLabel = new Label( envActionGroup, SWT.LEFT );
		keyLabel.setText(Messages.getString("TenantInfoDialog.UI.USER_KEY"));

		userKey = new Text( envActionGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
		GridData keyData = new GridData( GridData.FILL_HORIZONTAL );
		keyData.horizontalSpan = 2;
		keyData.grabExcessHorizontalSpace = true;
		userKey.setLayoutData( keyData );

		secretLabel = new Label( envActionGroup, SWT.LEFT );
		secretLabel.setText( Messages.getString("TenantInfoDialog.UI.USER_SECRET") );

		userSecret = new Text( envActionGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
		userSecret.setLayoutData( keyData );

		enableKeySecretFields();
	}

	public void dispose() {
		props.setScreen(new WindowProperty(shlConfiguredata));
		shlConfiguredata.dispose();
	}

	private void enableKeySecretFields() {
		userKey.setEnabled( Boolean.TRUE );
		userSecret.setEnabled( Boolean.TRUE );
	}

	private void disableKeySecretFields() {
		userKey.setEnabled( Boolean.FALSE );
		userSecret.setEnabled( Boolean.FALSE );
	}

	private void cleanUpKeySecretFields() {
		userKey.setText( "" );
		userSecret.setText( "" );
	}

	private void deSelectEnabledCheckbox() {
		checkBox.setSelection( Boolean.FALSE );
		checkBox.setEnabled( Boolean.TRUE );
	}

	private void selectEnabledCheckbox() {
		checkBox.setSelection( Boolean.TRUE );
		checkBox.setEnabled( Boolean.TRUE );
	}

	private void deSelectDisabledCheckbox() {
		checkBox.setSelection( Boolean.FALSE );
		checkBox.setEnabled( Boolean.FALSE );
	}

	private void selectDisabledCheckbox() {
		checkBox.setSelection( Boolean.TRUE );
		checkBox.setEnabled( Boolean.FALSE );
	}

	private void populateRowFieldData(TenantInfo chosenInfo) {
		information.setText("");

		if (envActionType.getText().equals(ACTIONS.ADD.name())) {
			deSelectEnabledCheckbox();
			cleanUpKeySecretFields();
			enableKeySecretFields();

			tenantAliasDropDown.setEnabled( Boolean.FALSE );

		    tableViewARConfig.table.setEnabled( Boolean.TRUE );
		    tableViewPublishConfig.table.setEnabled( Boolean.TRUE );

			getDataIntoARConfigWidget();
			getDataIntoPublishConfigWidget();
		}
		if (envActionType.getText().equals(ACTIONS.DELETE.name())) {
			tenantAliasDropDown.setEnabled( Boolean.TRUE );
			if (chosenInfo != null) {
				if (chosenInfo.isDefaultTenant()) {
					selectDisabledCheckbox();
					cleanUpKeySecretFields();
					disableKeySecretFields();
				} else {
					deSelectDisabledCheckbox();
					cleanUpKeySecretFields();
					enableKeySecretFields();
				}
			} else {
				deSelectDisabledCheckbox();
				cleanUpKeySecretFields();
				disableKeySecretFields();
			}

			tableViewARConfig.table.setEnabled( Boolean.FALSE );
		    tableViewPublishConfig.table.setEnabled( Boolean.FALSE );
		    
			getDataIntoARConfigWidget();
			getDataIntoPublishConfigWidget();
		}
		if (envActionType.getText().equals(ACTIONS.EDIT.name())) {
			tenantAliasDropDown.setEnabled( Boolean.TRUE );
			if (chosenInfo != null) {
				if (chosenInfo.isDefaultTenant()) {
					selectDisabledCheckbox();
					cleanUpKeySecretFields();
					enableKeySecretFields();
				} else {
					deSelectEnabledCheckbox();
					cleanUpKeySecretFields();
					enableKeySecretFields();
				}
			    tableViewARConfig.table.setEnabled( Boolean.TRUE );
			    tableViewPublishConfig.table.setEnabled( Boolean.FALSE );
			} else {
				deSelectDisabledCheckbox();
				cleanUpKeySecretFields();
				disableKeySecretFields();

			    tableViewARConfig.table.setEnabled( Boolean.FALSE );
			    tableViewPublishConfig.table.setEnabled( Boolean.FALSE );
			}
			getDataIntoARConfigWidget();
			getDataIntoPublishConfigWidget();
		}

		setPublishConfigFields( chosenInfo );
		setARConfigFields( chosenInfo );
	}

	private void setARConfigFields( TenantInfo chosenInfo ) {
		for (int i = 0; i < arConfigArgs.getRowMeta().size(); i++) {
			ValueMetaInterface valueMeta = arConfigArgs.getRowMeta().getValueMeta(i);
			Object valueData = arConfigArgs.getData()[i];

			TableItem[] items = tableViewARConfig.table.getItems();
			for (int r = 0; r < items.length; r++) {
				if (valueMeta.getName().equals(items[r].getText(1))) {
					if ( TenantInfoList.AR_HOST.equalsIgnoreCase(valueData + "") ) {
						items[r].setText(2, chosenInfo != null ? chosenInfo.getArHost() : "");
					}
					if ( TenantInfoList.AR_RPC_PORT.equalsIgnoreCase(valueData + "") ) {
						items[r].setText(2, chosenInfo != null ? chosenInfo.getArRpcPort() : "");
					}
					if ( TenantInfoList.REST_ENDPOINT.equalsIgnoreCase(valueData + "") ) {
						items[r].setText(2, chosenInfo != null ? chosenInfo.getArRestEndpoint() : "");
					}
				}
			}
		}
	}

	private void setPublishConfigFields(TenantInfo chosenInfo) {
		for (int i = 0; i < publishConfigArgs.getRowMeta().size(); i++) {
			ValueMetaInterface valueMeta = publishConfigArgs.getRowMeta().getValueMeta(i);
			Object valueData = publishConfigArgs.getData()[i];

			TableItem[] items = tableViewPublishConfig.table.getItems();
			for (int r = 0; r < items.length; r++) {
				if (valueMeta.getName().equals(items[r].getText(1))) {
					if ( TenantInfoList.TENANT_ID.equalsIgnoreCase(valueData + "") ) {
						items[r].setText(2, chosenInfo != null ? chosenInfo.getTenantId() : "");
					}
					if ( TenantInfoList.TENANT_URL.equalsIgnoreCase(valueData + "") ) {
						items[r].setText(2, chosenInfo != null ? chosenInfo.getTenantUrl() : "");
					}
					if ( TenantInfoList.ENV_ALIAS.equalsIgnoreCase(valueData + "") ) {
						items[r].setText(2, chosenInfo != null ? chosenInfo.getEnvAlias() : "");
					}
				}
			}
		}
	}

	public void getDataIntoPublishConfigWidget() {
		if (publishConfigArgs != null) {
			for (int i = 0; i < publishConfigArgs.getRowMeta().size(); i++) {
				ValueMetaInterface valueMeta = publishConfigArgs.getRowMeta().getValueMeta(i);
				Object valueData = publishConfigArgs.getData()[i];
				String string;
				try {
					string = valueMeta.getString(valueData);
				} catch (KettleValueException e) {
					string = "";
				}
				TableItem item = tableViewPublishConfig.table.getItem(i);
				if ( TenantInfoList.TENANT_ID.equalsIgnoreCase(valueData + "") || TenantInfoList.TENANT_URL.equalsIgnoreCase(valueData + "") || TenantInfoList.ENV_ALIAS.equalsIgnoreCase(valueData + "")) {
					item.setText(1, valueMeta.getName());
					if (!Utils.isEmpty(string)) {
						item.setText(2, string);
					}
				}
			}
		}

		tableViewPublishConfig.sortTable(1);
		tableViewPublishConfig.setRowNums();
		tableViewPublishConfig.optWidth(true);
	}

	public void getDataIntoARConfigWidget() {
		if (arConfigArgs != null) {
			for (int i = 0; i < arConfigArgs.getRowMeta().size(); i++) {
				ValueMetaInterface valueMeta = arConfigArgs.getRowMeta().getValueMeta(i);
				Object valueData = arConfigArgs.getData()[i];
				String string;
				try {
					string = valueMeta.getString(valueData);
				} catch (KettleValueException e) {
					string = "";
				}
				TableItem item = tableViewARConfig.table.getItem(i);
				if ( TenantInfoList.AR_HOST.equalsIgnoreCase(valueData + "") || TenantInfoList.AR_RPC_PORT.equalsIgnoreCase(valueData + "") || TenantInfoList.REST_ENDPOINT.equalsIgnoreCase(valueData + "")) {
					item.setText(1, valueMeta.getName());
					if (!Utils.isEmpty(string)) {
						item.setText(2, string);
					}
				}
			}
		}
		tableViewARConfig.sortTable(1);
		tableViewARConfig.setRowNums();
		tableViewARConfig.optWidth(true);
	}
}