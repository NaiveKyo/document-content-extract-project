package io.naivekyo.support.word;

import org.apache.poi.xwpf.usermodel.Document;

/**
 * 图片类型枚举, 适配 Word
 * @see org.apache.poi.xwpf.usermodel.Document
 * @see org.apache.poi.sl.usermodel.PictureData.PictureType
 * @author NaiveKyo
 * @since 1.0
 */
public enum ImageType {

    /**
     * Extended windows meta file
     */
    EMF(Document.PICTURE_TYPE_EMF, "emf", "image/x-emf", ".emf"),

    /**
     * Windows Meta File
     */
    WMF(Document.PICTURE_TYPE_WMF, "wmf", "image/x-wmf", ".wmf"),

    /**
     * Mac PICT format, or image/x-pict (for HSLF) ???
     */
    PICT(Document.PICTURE_TYPE_PICT, "pict", "image/pict", ".pict"),

    /**
     * JPEG format
     */
    JPEG(Document.PICTURE_TYPE_JPEG, "jpeg", "image/jpeg", ".jpg"),

    /**
     * PNG format
     */
    PNG(Document.PICTURE_TYPE_PNG, "png", "image/png", ".png"),

    /**
     * Device independent bitmap
     */
    DIB(Document.PICTURE_TYPE_DIB, "dib", "image/dib", ".dib"),

    /**
     * GIF image format
     */
    GIF(Document.PICTURE_TYPE_GIF, "gif", "image/gif", ".gif"),

    /**
     * Tag Image File (.tif is a proprietary file format owned by Adobe, while .tiff is an open standard file format that can be used on any platform)
     */
    TIFF(Document.PICTURE_TYPE_TIFF, "tiff", "image/tiff", ".tiff"),

    /**
     * Encapsulated Postscript (.eps)
     */
    EPS(Document.PICTURE_TYPE_EPS, "eps", "image/x-eps", ".eps"),

    /**
     * Windows Bitmap (.bmp)
     */
    BMP(Document.PICTURE_TYPE_BMP, "bmp", "image/x-ms-bmp", ".bmp"),

    /**
     * WordPerfect graphics (.wpg)
     */
    WPG(Document.PICTURE_TYPE_WPG, "wpg", "image/x-wpg", ".wpg"),

    /**
     * unknown type
     */
    UNKNOWN(-1, "unknown", "image/unknown", "unknown");

    private final int typeId;
    
    private final String name;
    
    private final String mimeType;
    
    private final String extension;

    ImageType(int typeId, String name, String mimeType, String extension) {
        this.typeId = typeId;
        this.name = name;
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * lookup ImageType by type id
     * @param typeId {@link Document#PICTURE_TYPE_PNG} etc.
     * @return {@link ImageType} instances, or {@link ImageType#UNKNOWN} if no match
     */
    public static ImageType lookupByTypeId(int typeId) {
        if (UNKNOWN.getTypeId() == typeId)
            return UNKNOWN;

        for (ImageType value : values()) {
            if (value.getTypeId() == typeId)
                return value;
        }

        return UNKNOWN;
    }
    
    /**
     * lookup ImageType by mime-type
     * @param mimeType the MIME type for the image
     * @return {@link ImageType} instances, or {@link ImageType#UNKNOWN} if no match
     */
    public static ImageType lookupByMimeType(String mimeType) {
        if (mimeType == null || UNKNOWN.mimeType.equals(mimeType))
            return UNKNOWN;
        
        for (ImageType value : values()) {
            if (value.getMimeType().equals(mimeType))
                return value;
        }
        
        return UNKNOWN;
    }
}
