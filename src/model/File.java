package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import util.FATUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class File implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;//文件名
    private String type;// 类型
    private int diskNum;// 起始盘块号
    private int flag;//读写标记
    private int length;// 占用盘块数
    private String content;// 内容

    private Folder parent;//父文件夹

    private String location; // 位置
    private double size; // 大小
    private String space; // 占用空间
    private Date createTime;//创建时间

    private boolean isOpen;//打开标志

    private transient StringProperty fileNameP = new SimpleStringProperty();//读写的属性，读写属性fileNameP;属性是以用对象，更像是一种规范。就是把字符变对象。
    private transient StringProperty flagP = new SimpleStringProperty();
    private transient StringProperty diskNumP = new SimpleStringProperty();
    private transient StringProperty locationP = new SimpleStringProperty();
    private transient StringProperty lengthP = new SimpleStringProperty();

    //让UI获取property;
    public StringProperty fileNamePProperty() {
        return fileNameP;
    }

    public StringProperty flagPProperty() {
        return flagP;
    }

    public StringProperty diskNumPProperty() {
        return diskNumP;
    }

    public StringProperty locationPProperty() {
        return locationP;
    }

    public StringProperty lengthPProperty() {
        return lengthP;
    }

    //设置property；
    private void setFileNameP() {
        this.fileNameP.set(fileName);
    }

    private void setFlagP() {
        this.flagP.set(flag == FATUtil.FLAGREAD ? "只读" : "读写");
    }

    private void setDiskNumP() {
        this.diskNumP.set(String.valueOf(diskNum));
    }

    private void setLocationP() {
        this.locationP.set(location);
    }

    private void setLengthP() {
        this.lengthP.set(String.valueOf(length));
    }

    public File(String fileName) {
        this.fileName = fileName;
        this.setOpened(false);//一开始默认不打开。
        setFileNameP();
    }

    public File(String fileName, String location, int diskNum, Folder parent) {
        this.fileName = fileName;
        this.type = FATUtil.FILE;
        this.diskNum = diskNum;
        this.length = 1;
        this.content = "";

        this.location = location;
        this.size = FATUtil.getSize(content.length());
        this.space = size + "KB";
        this.createTime = new Date();//记录创建时间;

        this.parent = parent;

        this.setOpened(false);

        setFileNameP();
        setDiskNumP();
        setFlagP();
        setLocationP();
        setLengthP();
    }

    public boolean isOpened() {
        return isOpen;
    }

    public void setOpened(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        setFileNameP();//同步设置属性；
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDiskNum() {
        return diskNum;
    }

    public void setDiskNum(int diskNum) {
        this.diskNum = diskNum;
        setDiskNumP();
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
        setFlagP();
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
        setLengthP();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        setLocationP();
    }

    public double getSize() {
        return size;
    }

    public void setSize(double KBcount) {
        this.size = KBcount;
        this.setSpace(size + "KB");//同时设置space空间;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getCreateTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日  HH:mm:ss");
        return format.format(createTime);
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Folder getParent() {
        return parent;
    }

    public void setParent(Folder parent) {
        this.parent = parent;
    }

    public boolean hasParent() {
        return (parent == null) ? false : true;
    }

    private void readObject(ObjectInputStream s)throws IOException,ClassNotFoundException{
        s.defaultReadObject();
        fileNameP=new SimpleStringProperty(fileName);
        flagP=new SimpleStringProperty(flag==FATUtil.FLAGREAD?"只读":"读写");
        diskNumP = new SimpleStringProperty(String.valueOf(type));
        locationP = new SimpleStringProperty(location);
        lengthP = new SimpleStringProperty(String.valueOf(length));
    }
    @Override
    public String toString(){
        return fileName;
    }
}


