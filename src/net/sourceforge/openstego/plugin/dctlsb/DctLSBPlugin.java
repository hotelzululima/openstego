/*
 * Steganography utility to hide messages into cover files
 * Author: Samir Vaidya (mailto:syvaidya@gmail.com)
 * Copyright (c) 2007-2008 Samir Vaidya
 */

package net.sourceforge.openstego.plugin.dctlsb;

import java.awt.image.BufferedImage;
import java.io.IOException;

import net.sourceforge.openstego.OpenStegoException;
import net.sourceforge.openstego.plugin.template.dct.DCT;
import net.sourceforge.openstego.plugin.template.dct.DCTDataHeader;
import net.sourceforge.openstego.plugin.template.dct.DCTPluginTemplate;
import net.sourceforge.openstego.util.ImageUtil;
import net.sourceforge.openstego.util.LabelUtil;

/**
 * Plugin for OpenStego which implements the DCT based Least-significant bit algorithm
 */
public class DctLSBPlugin extends DCTPluginTemplate
{
    /**
     * LabelUtil instance to retrieve labels
     */
    private static LabelUtil labelUtil = LabelUtil.getInstance(DctLSBPlugin.NAMESPACE);

    /**
     * Constant for Namespace to use for this plugin
     */
    public final static String NAMESPACE = "DCTLSB";

    /**
     * Default constructor
     */
    public DctLSBPlugin()
    {
        LabelUtil.addNamespace(NAMESPACE, "net.sourceforge.openstego.resource.DctLSBPluginLabels");
        new DctLSBErrors(); // Initialize error codes
    }

    /**
     * Gives the name of the plugin
     * @return Name of the plugin
     */
    public String getName()
    {
        return "DctLSB";
    }

    /**
     * Gives a short description of the plugin
     * @return Short description of the plugin
     */
    public String getDescription()
    {
        return labelUtil.getString("plugin.description");
    }

    /**
     * Method to embed the message into the cover data
     * @param msg Message to be embedded
     * @param msgFileName Name of the message file. If this value is provided, then the filename should be
     *                    embedded in the cover data
     * @param cover Cover data into which message needs to be embedded
     * @param coverFileName Name of the cover file
     * @param stegoFileName Name of the output stego file
     * @return Stego data containing the message
     * @throws OpenStegoException
     */
    public byte[] embedData(byte[] msg, String msgFileName, byte[] cover, String coverFileName, String stegoFileName)
            throws OpenStegoException
    {
        BufferedImage image = null;
        DctLSBOutputStream os = null;

        try
        {
            // Generate random image, if input image is not provided
            if(cover == null)
            {
                image = ImageUtil.generateRandomImage((DCTDataHeader.getMaxHeaderSize() + msg.length) * 8 * DCT.NJPEG
                        * DCT.NJPEG);
            }
            else
            {
                image = ImageUtil.byteArrayToImage(cover, coverFileName);
            }
            os = new DctLSBOutputStream(image, msg.length, msgFileName, this.config);
            os.write(msg);
            os.close();

            return ImageUtil.imageToByteArray(os.getImage(), stegoFileName, this);
        }
        catch(IOException ioEx)
        {
            throw new OpenStegoException(ioEx);
        }
    }

    /**
     * Method to extract the message file name from the stego data
     * @param stegoData Stego data containing the message
     * @param stegoFileName Name of the stego file
     * @return Message file name
     * @throws OpenStegoException
     */
    public String extractMsgFileName(byte[] stegoData, String stegoFileName) throws OpenStegoException
    {
        String fileName = null;
        DctLSBInputStream is = null;

        try
        {
            is = new DctLSBInputStream(ImageUtil.byteArrayToImage(stegoData, stegoFileName), this.config);
            fileName = is.getDataHeader().getFileName();
            is.close();
        }
        catch(IOException ioEx)
        {
            throw new OpenStegoException(ioEx);
        }

        return fileName;
    }

    /**
     * Method to extract the message from the stego data
     * @param stegoData Stego data containing the message
     * @param stegoFileName Name of the stego file
     * @return Extracted message
     * @throws OpenStegoException
     */
    public byte[] extractData(byte[] stegoData, String stegoFileName) throws OpenStegoException
    {
        byte[] msg = null;
        DCTDataHeader header = null;
        DctLSBInputStream is = null;
        int bytesRead = 0;

        try
        {
            is = new DctLSBInputStream(ImageUtil.byteArrayToImage(stegoData, stegoFileName), this.config);
            header = is.getDataHeader();
            msg = new byte[header.getDataLength()];

            bytesRead = is.read(msg, 0, msg.length);
            if(bytesRead != msg.length)
            {
                throw new OpenStegoException(NAMESPACE, DctLSBErrors.ERR_IMAGE_DATA_READ, null);
            }
            is.close();
        }
        catch(IOException ioEx)
        {
            throw new OpenStegoException(ioEx);
        }

        return msg;
    }

    /**
     * Method to get the usage details of the plugin
     * @return Usage details of the plugin
     * @throws OpenStegoException
     */
    public String getUsage() throws OpenStegoException
    {
        return labelUtil.getString("plugin.usage");
    }
}
