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
 * Custom tea context that provides access to the local file system including
 * reading, writing, moving, deleting, etc.
 * 
 * @author Scott Jappinen
 */
public class FileSystemContext implements Context {

    private Log mLog;

    /**
     * Initialize the context with the given configuration.
     * 
     * @param config  The configuration to initialize with
     */
    public void init(ContextConfig config) {
        mLog = config.getLog();
    }
    
    /**
     * Delete the file at the given path.
     * 
     * @param filePath The path of the file to delete
     * 
     * @return <code>true</code> if the file was deleted,  
     *         <code>false</code> otherwise
     *         
     * @throws IOException if the file does not exist, cannot be written,
     *         or if an error occurs during deletion
     */
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
                        String msg = 
                            "Can not delete write locked file (" + filePath + ")";
                        
                        mLog.warn(msg);
                        throw new IOException(msg);
                    }
                } else {
                    String msg =
                        "Cannot delete non-existant file (" + filePath + ")";
                    
                    mLog.warn(msg);
                    throw new IOException(msg);
                }
            } catch (IOException e) {
                mLog.error("Error deleting: " + filePath);
                mLog.error(e);
                throw e;
            }
        }
        return result;
    }

    /**
     * Move the file referenced by the srcPath to the dstPath. This will cause
     * the dstPath file to be overwritten if it already exists. If the srcPath
     * does not exist, or the dstPath cannot be written to, or an error occurs
     * during moving, then an {@link IOException} is thrown.
     * 
     * @param srcPath The path of the file to move
     * @param dstPath The path to move the file to
     * 
     * @return <code>true</code> if the file was properly moved,
     *         <code>false</code> otherwise
     *         
     * @throws IOException if an error occurs moving the file
     */
    public boolean moveFile(String srcPath, String dstPath) 
        throws IOException {
        
		return moveFile(srcPath, dstPath, true);
	}

    /**
     * Move the file referenced by the srcPath to the dstPath. If the overwrite
     * flag is <code>true</code>this will cause the dstPath file to be 
     * overwritten if it already exists. If the srcPath does not exist, the 
     * dstPath cannot be written to, the overwrite flag was <code>false</code>
     * and the dstPath already exists, or an error occurs
     * during moving, then an {@link IOException} is thrown.
     * 
     * @param srcPath The path of the file to move
     * @param dstPath The path to move the file to
     * @param overwrite Whether to overwrite the dstPath or not
     * 
     * @return <code>true</code> if the file was properly moved,
     *         <code>false</code> otherwise
     *         
     * @throws IOException if an error occurs moving the file
     */
    public boolean moveFile(String srcPath, String dstPath, boolean overwrite) 
        throws IOException {
        
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
					String message = 
					    "Cannot move non-existant file (" + srcPath + ")";
					mLog.warn(message);
					throw new IOException(message);
				}
				File dstFile = new File(dstPath);
				synchronized (dstPath) {
					if (dstFile.exists() && !overwrite) {
						String message = 
						    "Destination file already exists (" + dstPath + ")";
						mLog.warn(message);
						throw new IOException(message);
					}
					if (dstFile.exists() && !dstFile.canWrite()) {
						String message = 
						    "Can not overwrite write locked file (" + dstPath + ")";
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

    /**
     * Get the file at the given file path.
     * 
     * @param filePath The path of the file
     * 
     * @return The file for the associated file path
     */
    public File getFile(String filePath) {
        return new File(filePath);
    }
    
    /**
     * Get the list of files within the given directory. This only returns the
     * files in the immediate directory and is not recursive.
     * 
     * @param directoryPath The path of the directory
     * 
     * @return The list of files in the directory
     * 
     * @see File#listFiles()
     */
    public File[] listFiles(String directoryPath) {
        File[] result = null;
        File directory = null;
        directory = new File(directoryPath);
        if (directory.isDirectory()) {
            result = directory.listFiles();
        }
        return result;
    }

    /**
     * Read the contents of the given file and return the value as a string.
     * 
     * @param filePath The path to the file to read
     * 
     * @return The contents of the file as a string
     * 
     * @throws IOException if an error occurs reading the file
     */
	public String readFileAsString(String filePath) 
	    throws IOException {
	    
		String result = null;
		Reader reader = new BufferedReader(new FileReader(filePath));

		StringBuilder stringBuffer = new StringBuilder(4096);
		int charRead = -1;
		while ((charRead = reader.read()) >= 0) {
			stringBuffer.append((char) charRead);
		}

		reader.close();
		result = stringBuffer.toString();
		return result;
	}

	/**
     * Write the contents of the given file data to the file at the given path.
     * This will replace any existing data.
     * 
     * @param filePath The path to the file to write to
     * @param fileData The data to write to the given file
     * 
     * @throws IOException if an error occurs writing the data
     * 
     * @see #writeToFile(String, String, boolean)
     */
    public void writeToFile(String filePath, String fileData)
        throws IOException {
        
        writeToFile(filePath, fileData, false);
    }
    
	/**
	 * Write the contents of the given file data to the file at the given path.
	 * If the append flag is <code>true</code>, the file data will be appended
	 * to the end of the file if it already exists. Note that this will attempt
	 * to create any and all parent directories if the path does not exist.
	 * 
	 * @param filePath The path to the file to write to
	 * @param fileData The data to write to the given file
	 * @param append Whether to append the file data or replace existing data
	 * 
	 * @throws IOException if an error occurs writing the data
	 */
    public void writeToFile(String filePath, String fileData, boolean append)
        throws IOException {

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
                    fileWriter = new FileWriter(file, append);
                    fileWriter.write(fileData);
                    fileWriter.flush();
                } else {
                    String msg =
                        "File could not be created. (" + filePath + ")";
                    mLog.error(msg);
                    throw new IOException(msg);
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

    /**
     * Write the contents of the given file data to the file at the given path.
     * This will replace any existing data.
     * 
     * @param filePath The path to the file to write to
     * @param fileData The data to write to the given file
     * 
     * @throws IOException if an error occurs writing the data
     * 
     * @see #writeToFile(String, String, boolean)
     */
    public void writeToFile(String filePath, byte[] fileData)
        throws IOException {
        
        writeToFile(filePath, fileData, false);
    }
    
    /**
     * Write the contents of the given file data to the file at the given path.
     * If the append flag is <code>true</code>, the file data will be appended
     * to the end of the file if it already exists. Note that this will attempt
     * to create any and all parent directories if the path does not exist.
     * 
     * @param filePath The path to the file to write to
     * @param fileData The data to write to the given file
     * @param append Whether to append the file data or replace existing data
     * 
     * @throws IOException if an error occurs writing the data
     */
    public void writeToFile(String filePath, byte[] fileData, boolean append)
        throws IOException {
        
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
                    fileOutputStream = new FileOutputStream(file, append);
                    fileOutputStream.write(fileData);
                    fileOutputStream.flush();
                } else {
                    String msg =
                        "File could not be created. (" + filePath + ")";
                    mLog.error(msg);
                    throw new IOException(msg);
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

    /**
     * Read the contents of the file at the given path and return the associated
     * byte array.
     * 
     * @param filePath The path to the file to read
     * 
     * @return The contents of the file
     * 
     * @throws IOException if an error occurs reading the file
     */
	public byte[] getFileBytes(String filePath)
		throws IOException {
	    
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
				    String msg = 
				        "File does not exist. (" + filePath + ")";
					mLog.error(msg);
					throw new IOException(msg);
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
