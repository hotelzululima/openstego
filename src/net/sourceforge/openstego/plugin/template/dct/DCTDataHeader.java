/*
 * Steganography utility to hide messages into cover files
 * Author: Samir Vaidya (mailto:syvaidya@gmail.com)
 * Copyright (c) 2007-2008 Samir Vaidya
 */

package net.sourceforge.openstego.plugin.template.dct;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import net.sourceforge.openstego.OpenStegoConfig;
import net.sourceforge.openstego.OpenStegoException;

/**
 * This class holds the header data for the data that needs to be embedded in the image.
 * First, the header data gets written inside the image, and then the actual data is written.
 */
public class DCTDataHeader
{
    /**
     * Magic string at the start of the header to identify OpenStego embedded data
     */
    public static final byte[] DATA_STAMP = "OSDCT".getBytes();

    /**
     * Header version to distinguish between various versions of data embedding. This should be changed to next
     * version, in case the structure of the header is changed.
     */
    public static final byte[] HEADER_VERSION = new byte[] { (byte) 1 };

    /**
     * Length of the fixed portion of the header
     */
    private static final int FIXED_HEADER_LENGTH = 7;

    /**
     * Length of the data embedded in the image (excluding the header data)
     */
    private int dataLength = 0;

    /**
     * Name of the file being embedded in the image (as byte array)
     */
    private byte[] fileName = null;

    /**
     * OpenStegoConfig instance to hold the configuration data
     */
    private OpenStegoConfig config = null;

    /**
     * This constructor should normally be used when writing the data.
     * @param dataLength Length of the data embedded in the image (excluding the header data)
     * @param fileName Name of the file of data being embedded
     * @param config OpenStegoConfig instance to hold the configuration data
     */
    public DCTDataHeader(int dataLength, String fileName, OpenStegoConfig config)
    {
        this.dataLength = dataLength;
        this.config = config;

        if(fileName == null)
        {
            this.fileName = new byte[0];
        }
        else
        {
            try
            {
                this.fileName = fileName.getBytes("UTF-8");
            }
            catch(UnsupportedEncodingException unEx)
            {
                this.fileName = fileName.getBytes();
            }
        }
    }

    /**
     * This constructor should be used when reading embedded data from an InputStream.
     * @param dataInStream Data input stream containing the embedded data
     * @param config OpenStegoConfig instance to hold the configuration data
     * @throws OpenStegoException
     */
    public DCTDataHeader(InputStream dataInStream, OpenStegoConfig config) throws OpenStegoException
    {
        int stampLen = 0;
        int versionLen = 0;
        int fileNameLen = 0;
        byte[] header = null;
        byte[] stamp = null;
        byte[] version = null;

        stampLen = DATA_STAMP.length;
        versionLen = HEADER_VERSION.length;
        header = new byte[FIXED_HEADER_LENGTH];
        stamp = new byte[stampLen];
        version = new byte[versionLen];

        try
        {
            dataInStream.read(stamp, 0, stampLen);
            if(!(new String(stamp)).equals(new String(DATA_STAMP)))
            {
                throw new OpenStegoException(DCTPluginTemplate.NAMESPACE, DCTErrors.INVALID_STEGO_HEADER, null);
            }

            dataInStream.read(version, 0, versionLen);
            if(!(new String(version)).equals(new String(HEADER_VERSION)))
            {
                throw new OpenStegoException(DCTPluginTemplate.NAMESPACE, DCTErrors.INVALID_HEADER_VERSION, null);
            }

            dataInStream.read(header, 0, FIXED_HEADER_LENGTH);
            dataLength = (byteToInt(header[0]) + (byteToInt(header[1]) << 8) + (byteToInt(header[2]) << 16) + (byteToInt(header[3]) << 32));
            fileNameLen = header[4];
            config.setUseCompression(header[5] == 1);
            config.setUseEncryption(header[6] == 1);

            if(fileNameLen == 0)
            {
                fileName = new byte[0];
            }
            else
            {
                fileName = new byte[fileNameLen];
                dataInStream.read(fileName, 0, fileNameLen);
            }
        }
        catch(OpenStegoException osEx)
        {
            throw osEx;
        }
        catch(Exception ex)
        {
            throw new OpenStegoException(ex);
        }

        this.config = config;
    }

    /**
     * This method generates the header in the form of byte array based on the parameters provided in the constructor.
     * @return Header data
     */
    public byte[] getHeaderData()
    {
        byte[] out = null;
        int stampLen = 0;
        int versionLen = 0;
        int currIndex = 0;

        stampLen = DATA_STAMP.length;
        versionLen = HEADER_VERSION.length;
        out = new byte[stampLen + versionLen + FIXED_HEADER_LENGTH + fileName.length];

        System.arraycopy(DATA_STAMP, 0, out, currIndex, stampLen);
        currIndex += stampLen;

        System.arraycopy(HEADER_VERSION, 0, out, currIndex, versionLen);
        currIndex += versionLen;

        out[currIndex++] = (byte) ((dataLength & 0x000000FF));
        out[currIndex++] = (byte) ((dataLength & 0x0000FF00) >> 8);
        out[currIndex++] = (byte) ((dataLength & 0x00FF0000) >> 16);
        out[currIndex++] = (byte) ((dataLength & 0xFF000000) >> 32);
        out[currIndex++] = (byte) fileName.length;
        out[currIndex++] = (byte) (config.isUseCompression() ? 1 : 0);
        out[currIndex++] = (byte) (config.isUseEncryption() ? 1 : 0);

        if(fileName.length > 0)
        {
            System.arraycopy(fileName, 0, out, currIndex, fileName.length);
            currIndex += fileName.length;
        }

        return out;
    }

    /**
     * Get Method for dataLength
     * @return dataLength
     */
    public int getDataLength()
    {
        return dataLength;
    }

    /**
     * Get Method for fileName
     * @return fileName
     */
    public String getFileName()
    {
        String name = null;

        try
        {
            name = new String(fileName, "UTF-8");
        }
        catch(UnsupportedEncodingException unEx)
        {
            name = new String(fileName);
        }
        return name;
    }

    /**
     * Method to get size of the current header
     * @return Header size
     */
    public int getHeaderSize()
    {
        return DATA_STAMP.length + HEADER_VERSION.length + FIXED_HEADER_LENGTH + fileName.length;
    }

    /**
     * Method to get the maximum possible size of the header
     * @return Maximum possible header size
     */
    public static int getMaxHeaderSize()
    {
        // Max file name length assumed to be 256
        return DATA_STAMP.length + HEADER_VERSION.length + FIXED_HEADER_LENGTH + 256;
    }

    /**
     * Byte to Int converter
     * @param b Input byte value
     * @return Int value
     */
    public static int byteToInt(int b)
    {
        int i = (int) b;
        if(i < 0)
        {
            i = i + 256;
        }
        return i;
    }
}
