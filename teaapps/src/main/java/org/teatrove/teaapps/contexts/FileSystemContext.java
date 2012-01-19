/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teatrove.teaapps.contexts;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.Utils;

import org.teatrove.teaapps.Context;
import org.teatrove.teaapps.ContextConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.RandomAccessFile;

/**
 * @author Scott Jappinen
 */
public class FileSystemContext implements Context {

    private Log mLog;

    public void init(ContextConfig config) {
        mLog = config.getLog();
    }

    public boolean deleteFile(String filePath) throws IOException {
        boolean result = false;
        // synchronized on an interned string makes has all the
        // makings for exclusive write access as far as this
        // process is concerned.
        String path = (String) Utils.intern(filePath);
        synchronized (path) {
            File file = null;
            try {
                file = new File(path);
                if (file.exists()) {
                    if (file.canWrite()) {
                        result = file.delete();
                    } else {
                        mLog.warn("Can not delete write locked file (" + filePath + ")");
                        throw new IOException("Cannot delete non-existant file (" + filePath + ")");
                    }
                } else {
                    mLog.warn("Cannot delete non-existant file (" + filePath + ")");
                    throw new IOException("Cannot delete non-existant file (" + filePath + ")");
                }
            } catch (IOException e) {
                mLog.error("Error writing: " + filePath);
                mLog.error(e);
                throw e;
            }
        }
        return result;
    }

    public boolean moveFile(String srcPath, String dstPath) throws IOException {
		return moveFile(srcPath, dstPath, true);
	}

    public boolean moveFile(String srcPath, String dstPath, boolean overwrite) throws IOException {
		boolean result = false;
		srcPath = (String) Utils.intern(srcPath);
		dstPath = (String) Utils.intern(dstPath);

		// synchronized on an interned strings makes has all the
		// makings for exclusive write access as far as this
		// process is concerned.

		try {
			synchronized (srcPath) {
				File srcFile = new File(srcPath);
				if (!srcFile.exists()) {
					String message = "Cannot move non-existant file (" + srcPath + ")";
					mLog.warn(message);
					throw new IOException(message);
				}
				File dstFile = new File(dstPath);
				synchronized (dstPath) {
					if (dstFile.exists() && !overwrite) {
						String message = "Destination file already exists (" + dstPath + ")";
						mLog.warn(message);
						throw new IOException(message);
					}
					if (dstFile.exists() && !dstFile.canWrite()) {
						String message = "Can not overwrite write locked file (" + dstPath + ")";
						mLog.warn(message);
						throw new IOException(message);
					}
					result = srcFile.renameTo(dstFile);
				}
			}
		}
		catch (IOException e) {
			mLog.error("Error moving from: " + srcPath + " to " + dstPath);
			mLog.error(e);
			throw e;
		}
		return result;
    }

    public File[] listFiles(String directoryPath) {
        File[] result = null;
        File directory = null;
        directory = new File(directoryPath);
        if (directory.isDirectory()) {
            result = directory.listFiles();
        }
        return result;
    }

	public String readFileAsString(String filePath) throws IOException {
		String result = null;
		Reader reader = new BufferedReader(new FileReader(filePath));

		StringBuffer stringBuffer = new StringBuffer(1000);
		int charRead = -1;
		while ((charRead = reader.read()) >= 0) {
			stringBuffer.append((char) charRead);
		}

		reader.close();
		result = stringBuffer.toString();
		return result;
	}

    public void writeToFile(String filePath, String fileData)
        throws IOException
    {

        // synchronized on an interned string makes has all the
        // makings for exclusive write access as far as this
        // process is concerned.
        String path = (String) Utils.intern(filePath);
        synchronized (path) {
            File file = null;
            FileWriter fileWriter = null;
            try {
                file = new File(path);
                boolean exists = file.exists();
                if (!exists) {
                    exists = file.getParentFile().mkdirs();
                    exists = file.createNewFile();
                }
                if (exists) {
                    fileWriter = new FileWriter(file);
                    fileWriter.write(fileData);
                    fileWriter.flush();
                } else {
                    mLog.error("Error writing: " + filePath);
                    throw new IOException("File could not be created. (" + filePath + ")");
                }
            } catch (IOException e) {
                mLog.error("Error writing: " + filePath);
                mLog.error(e);
                throw e;
            } finally {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
        }
    }

    public void writeToFile(String filePath, byte[] fileData)
        throws IOException
    {
        // synchronized on an interned string makes has all the
        // makings for exclusive write access as far as this
        // process is concerned.
        String path = (String) Utils.intern(filePath);
        synchronized (path) {
            File file = null;
            FileOutputStream fileOutputStream = null;
            try {
                file = new File(path);
                boolean exists = file.exists();
                if (!exists) {
                    exists = file.getParentFile().mkdirs();
                    exists = file.createNewFile();
                }
                if (exists) {
                    fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(fileData);
                    fileOutputStream.flush();
                } else {
                    mLog.error("Error writing: " + filePath);
                    throw new IOException("File could not be created. (" + filePath + ")");
                }
            } catch (IOException e) {
                mLog.error("Error writing: " + filePath);
                mLog.error(e);
                throw e;
            } finally {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            }
        }
    }

	public byte[] getFileBytes(String filePath)
		throws IOException
	{
		byte[] out = null;
		String path = (String) Utils.intern(filePath);
		RandomAccessFile raf = null;
		synchronized (path) {
			File file = null;
			try {
				file = new File(path);
				boolean exists = file.exists();
				if (exists) {
					byte[] fileBytes = new byte[(int)file.length()];
					raf = new RandomAccessFile(file,"r");
					raf.readFully(fileBytes);
					out = fileBytes;
				} else {
					mLog.error("Error writing: " + filePath);
					throw new IOException("File could not be created. (" + filePath + ")");
				}
			} catch (IOException e) {
				mLog.error("Error writing: " + filePath);
				mLog.error(e);
				throw e;
			} finally {
				if (raf != null) {
					raf.close();
				}
			}
		}
		return out;
    }

}
