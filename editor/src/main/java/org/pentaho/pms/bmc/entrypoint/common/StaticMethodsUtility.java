package org.pentaho.pms.bmc.entrypoint.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

import org.pentaho.pms.bmc.entrypoint.beans.TenantInfoList;
import org.pentaho.pms.ui.locale.Messages;

public class StaticMethodsUtility {

	public Path getFilePath(String path) {
	    return Paths.get( path );
	}

	public Stream<Path> walk(Path path) throws IOException {
	    return Files.walk( path );
	}
	
	public static void deleteFile(File file) throws IOException {
	    if (file != null) {
	        if (file.isDirectory()) {
	            File[] files = file.listFiles();

	            for (File f: files) {
	                deleteFile(f);
	            }
	        }
	        Files.deleteIfExists(file.toPath());
	    }
	}
	
	public static boolean profileFileIsUsed() {
		Properties prop = new Properties();

		try (FileInputStream in = new FileInputStream( TenantInfoList.CONFIG_BASE_PATH + TenantInfoList.PROFILE_FILE_NAME )) {
			prop.load(in);
			if (prop.getProperty("KEY").isEmpty() || prop.getProperty("SECRET").isEmpty()) {
				return false;
			}
		} catch (Exception fe) {
			return false;
		}
		return true;
	}

}
