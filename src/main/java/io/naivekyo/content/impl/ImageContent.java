package io.naivekyo.content.impl;

import io.naivekyo.content.ContentHelper;
import io.naivekyo.content.ContentType;
import io.naivekyo.content.DocContent;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * 图片内容, thread-safe
 * @author NaiveKyo
 * @version 1.0
 * @since 2023/7/10 22:27
 */
public class ImageContent implements DocContent {
    
    /**
     * 存储当前图片的所有字节
     */
    private final byte[] rawData;

    /**
     * 当前图片的 mime 类型, image/unknown 表示未知的类型
     */
    private final String mimeType;

    /**
     * 图片的类型, 比如 png、jpeg 等等, 要求为全小写字母
     */
    private final String fileType;

    public ImageContent(byte[] rawData, String mimeType, String fileType) {
        this.rawData = rawData;
        this.mimeType = mimeType;
        this.fileType = fileType.toLowerCase();
    }

    public ImageContent(byte[] rawData, String fileType) {
        this(rawData, "image/" + fileType, fileType);
    }

    @Override
    public String getContent() {
        return ContentHelper.base64Encode(getRawData());
    }

    @Override
    public String getHTMLWrapContent() {
        return ContentHelper.convertImageToHtml(getRawData(), getFileType());
    }

    @Override
    public ContentType getType() {
        return ContentType.IMAGE;
    }

    /**
     * 将当前图片的所有字节写入到指定的字节输出流中
     * @param os 指定的字节输出流
     */
    public void writeImageContent(OutputStream os) throws IOException {
        byte[] content = getRawData();
        if (content != null && content.length > 0)
            os.write(content, 0, content.length);
    }

    /**
     * 获得图片原始字节数组
     * @return
     */
    public byte[] getRawData() {
        return rawData;
    }

    /**
     * 获取图片的 mime 类型
     * @return
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * 获取图片的文件类型
     * @return
     */
    public String getFileType() {
        return fileType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageContent that = (ImageContent) o;
        return Arrays.equals(rawData, that.rawData) && Objects.equals(mimeType, that.mimeType) && Objects.equals(fileType, that.fileType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mimeType, fileType);
        result = 31 * result + Arrays.hashCode(rawData);
        return result;
    }

    @Override
    public String toString() {
        return "ComplexImageContent{" +
                "bytes length=" + rawData.length +
                ", mime type='" + mimeType + '\'' +
                ", file type='" + fileType + '\'' +
                '}';
    }
}
