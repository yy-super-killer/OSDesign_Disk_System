package model;


import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import util.FATUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FAT implements Serializable {
    private static final long serialVersionUID = 1L;
    private DiskBlock[] diskBlocks;
    private transient ObservableList<File> openedFiles;//用于数据监听；里面存放的是打开的文件。用于绑定前端UI
    private Folder c;//初始文件夹;
    private Path rootPath = new Path("C:", null);//C盘就是根路径，无父文件夹
    private List<Path> paths;//用于存放所有路径。

    //用于饼图的显示
    private transient DoubleProperty totalProperty = new SimpleDoubleProperty();
    private transient DoubleProperty remainProperty = new SimpleDoubleProperty();
    private transient DoubleProperty usedProperty = new SimpleDoubleProperty();




    public FAT() {//文件分配表
        c = new Folder("C:", "root", 0, null);//创建C盘
        diskBlocks = new DiskBlock[128];//一共128块磁盘。
        diskBlocks[0] = new DiskBlock(0, FATUtil.END, FATUtil.DISK, c);//第二个是index,第三个是类型，第四个是文件夹。
        diskBlocks[0].setBegin(true);//设置开始标记
        diskBlocks[1] = new DiskBlock(1, FATUtil.END, FATUtil.DISK, c);//no是记录第几块磁盘号。这个也是C盘
        //第二个index是值
        for (int i = 2; i < 128; i++) {
            diskBlocks[i] = new DiskBlock(i, FATUtil.FREE, FATUtil.EMPTY, null);
            //初始化其他磁盘块，并且标记为可以使用的，类型为空。文件为空。
        }

        openedFiles = FXCollections.observableArrayList(new ArrayList<File>());
        paths = new ArrayList<Path>();//路径数组；动态数组；
        paths.add(rootPath);
        c.setPath(rootPath);//c是文件夹，在此设置该文件架的路径。
        remainProperty.set(128);
        usedProperty.set(0);
        totalProperty.set(128);


    }

    public void addOpenedFile(DiskBlock block) {
        File thisFile = (File) block.getObject();
        openedFiles.add(thisFile);
        thisFile.setOpened(true);
    }//添加打开的文件，获取该磁盘块的文件，加入监听数组里。设置为打开。

    public void removeOpenedFile(DiskBlock block) {
        File thisFile = (File) block.getObject();
        for (int i = 0; i < openedFiles.size(); i++) {
            if (openedFiles.get(i) == thisFile) {
                openedFiles.remove(i);
                thisFile.setOpened(false);
                break;
            }
        }
    }//从监听数组中找到该文件，并移出监听数组。

    public boolean isOpenedFile(DiskBlock block) {
        if (block.getObject() instanceof Folder) {
            return false;
        }//如果这块磁盘对应文件夹的话，说明打开的不是文件。
        return ((File) block.getObject()).isOpened();
    }//判断盘块中的文件是否已打开。



    public int createFolder(String path) {//在指定路径下创建文件夹
        String folderName = null;
        boolean canName = true;//能否命名，防止重名
        int index = 1;
        // 得到文件夹名
        do {
            folderName = "文件夹";
            canName = true;
            folderName += index;
            for (int i = 2; i < diskBlocks.length; i++) {//循环遍历磁盘，若是找到下标一样的，则不可以这么命名
                if (!diskBlocks[i].isFree()) {
                    if (diskBlocks[i].getType().equals(FATUtil.FOLDER)) {
                        Folder folder = (Folder) diskBlocks[i].getObject();
                        if (path.equals(folder.getLocation())) {
                            if (folderName.equals(folder.getFolderName())) {
                                canName = false;
                            }
                        }
                    }
                }
            }
            index++;
        } while (!canName);//知道找到可以命名的下标为止。
        int index2 = searchEmptyDiskBlock();
        if (index2 == FATUtil.ERROR) {
            return FATUtil.ERROR;//返回错误代码；
        } else {
            Folder parent = getFolder(path);//通过路径获得父文件夹
            Folder folder = new Folder(folderName, path, index2, parent);
            if (parent instanceof Folder) {
                parent.addChildren(folder);//往父文件夹的子文件夹动态数组里添加文件。
            }
            diskBlocks[index2].allocBlock(FATUtil.END, FATUtil.FOLDER, folder, true);//设置该磁盘块的值，磁盘块的值，文件类型，文件对象，开始标志。
            Path parentP = getPath(path);//返回路径对象，父文件夹对象
            Path thisPath = new Path(path + "\\" + folderName, parentP);
            if (parentP != null) {
                parentP.addChildren(thisPath);
            }
            paths.add(thisPath);
            folder.setPath(thisPath);
        }
        return index2;//返回磁盘号；
    }


    public int createFile(String path) {//在指定路径下创建文件
        String fileName = null;
        boolean canName = true;
        int index = 1;
        // 得到文件名
        do {
            fileName = "文件";
            canName = true;
            fileName += index;
            for (int i = 2; i < diskBlocks.length; i++) {
                if (!diskBlocks[i].isFree()) {
                    if (diskBlocks[i].getType().equals(FATUtil.FILE)) {
                        File file = (File) diskBlocks[i].getObject();
                        if (path.equals(file.getLocation())) {
                            if (fileName.equals(file.getFileName())) {
                                canName = false;
                            }
                        }
                    }
                }
            }
            index++;
        } while (!canName);
        int index2 = searchEmptyDiskBlock();
        if (index2 == FATUtil.ERROR) {
            return FATUtil.ERROR;
        } else {
            Folder parent = getFolder(path);
            File file = new File(fileName, path, index2, parent);
            file.setFlag(FATUtil.FLAGWRITE);
            if (parent instanceof Folder) {
                parent.addChildren(file);
            }
            diskBlocks[index2].allocBlock(FATUtil.END, FATUtil.FILE, file, true);
        }
        return index2;
    }







    public int searchEmptyDiskBlock(){//寻找第一个空闲的磁盘号码
        for(int i=2;i<diskBlocks.length;i++)//前两个是C盘所以从2开始
        {
            if(diskBlocks[i].isFree()){
                return i;
            }

        }
        return FATUtil.ERROR;

    }

    //计算已经使用的盘块数。
    public int usedBlocksCount() {
        int n = 0;
        for (int i = 2; i < diskBlocks.length; i++) {
            if (!diskBlocks[i].isFree()) {
                n++;
            }
        }
        remainProperty.set(128-n);
        usedProperty.set(n);
        totalProperty.set(128);
        return n;

    }

    //计算空闲的盘块数目
    public int freeBlocksCount() {
        int n = 0;
        for (int i = 2; i < diskBlocks.length; i++) {
            if (diskBlocks[i].isFree()) {
                n++;
            }
        }
        remainProperty.set(n);
        usedProperty.set(128-n);
        totalProperty.set(128);

        return n;

    }
    //是否有该路径
    public boolean hasPath(Path path) {
        for (Path p : paths) {
            if (p.equals(path)) {
                return true;
            }
        }
        return false;
    }




    public Folder getFolder(String path){
        if(path.equals("C:")){
            return c;
        }
        int split=path.lastIndexOf('\\');//记录反斜杠最后出现的位置
        String location=path.substring(0,split);//返回0到最后的斜杠的位置的字符
        String folderName=path.substring(split+1);//斜杠以后的是我文件名
        List<Folder> folders=getFolders(location);
        for (Folder folder:folders)//在指定位置找同名的对应文件夹
        {
            if(folder.getFolderName().equals((folderName))){
                return folder;
            }
        }
        return null;
    }

    public List<Folder> getFolders(String path){//返回指定路径下的所有文件夹，保存在动态数组里面。
        List<Folder> list = new ArrayList<Folder>();//动态数组
        for (int i = 2; i < diskBlocks.length; i++) {
            if (!diskBlocks[i].isFree()) {//磁盘块有内容
                if (diskBlocks[i].getObject() instanceof Folder) {//磁盘块是文件夹
                    if (((Folder) (diskBlocks[i].getObject())).getLocation().equals(path)) {
                        list.add((Folder) diskBlocks[i].getObject());//得到的对象文件夹的对象。把该文件夹加入数组
                    }
                }
            }
        }
        return list;
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {//读取这个对象的时候;
        s.defaultReadObject();
        openedFiles = FXCollections.observableArrayList(new ArrayList<File>());
        remainProperty=new SimpleDoubleProperty();
        usedProperty=new SimpleDoubleProperty();
        totalProperty=new SimpleDoubleProperty();
        //因为反序列化保存，所以在文件中不会有，重新读取的时候，重新new一个；
    }

    public List<DiskBlock> getBlockList(String path) {//通过路径返回所有文件夹和文件的起始盘块
        List<DiskBlock> bList = new ArrayList<DiskBlock>();
        for (int i = 2; i < diskBlocks.length; i++) {
            if (!diskBlocks[i].isFree()) {
                if (diskBlocks[i].getObject() instanceof Folder) {
                    if (((Folder) (diskBlocks[i].getObject())).getLocation().equals(path)
                            && diskBlocks[i].isBegin()) {//begin代表起始盘块
                        bList.add(diskBlocks[i]);
                    }
                }
            }
        }
        for (int i = 2; i < diskBlocks.length; i++) {
            if (!diskBlocks[i].isFree()) {
                if (diskBlocks[i].getObject() instanceof File) {
                    if (((File) (diskBlocks[i].getObject())).getLocation().equals(path)
                            && diskBlocks[i].isBegin()) {//begin代表起始盘块
                        bList.add(diskBlocks[i]);
                    }
                }
            }
        }
        return bList;
    }

    public List<DiskBlock> getSamename(String filename){
        List<DiskBlock> bList=new ArrayList<>();
        for (int i = 2; i < diskBlocks.length; i++) {
            if (!diskBlocks[i].isFree()) {
                if (diskBlocks[i].getObject() instanceof Folder) {
                    if (((Folder) (diskBlocks[i].getObject())).getFolderName().equals(filename)
                            && diskBlocks[i].isBegin()) {//begin代表起始盘块
                        bList.add(diskBlocks[i]);
                    }
                }
            }
        }
        for (int i = 2; i < diskBlocks.length; i++) {
            if (!diskBlocks[i].isFree()) {
                if (diskBlocks[i].getObject() instanceof File) {
                    if (((File) (diskBlocks[i].getObject())).getFileName().equals(filename)
                            && diskBlocks[i].isBegin()) {//begin代表起始盘块
                        bList.add(diskBlocks[i]);
                    }
                }
            }
        }
        return bList;

    }

    public int delete(DiskBlock block) {//删除某个磁盘块中的文件。
        if (block.getObject() instanceof File) {
            if (isOpenedFile(block)) {
                // 文件已打开，不能删除
                return 3;
            }
            File thisFile = (File) block.getObject();
            Folder parent = thisFile.getParent();
            if (parent instanceof Folder) {
                parent.removeChildren(thisFile);
                parent.setSize(FATUtil.getFolderSize(parent));//重新计算父文件夹的值
                while (parent.hasParent()) {
                    parent = parent.getParent();
                    parent.setSize(FATUtil.getFolderSize(parent));//往上重新计算父文件夹的值
                }
            }
            for (int i = 2; i < diskBlocks.length; i++) {
                if (!diskBlocks[i].isFree() && diskBlocks[i].getObject() instanceof File) {
                    System.out.println("yes");
                    if (((File) diskBlocks[i].getObject()).equals(thisFile)) {// 同一个对象,磁盘里放同一个对象。都要删了
                        System.out.println("yes2");
                        diskBlocks[i].clearBlock();
                    }
                }
            }
            return 1;
        } else {
            String folderPath = ((Folder) block.getObject()).getLocation() + "\\"
                    + ((Folder) block.getObject()).getFolderName();
            int index = 0;
            for (int i = 2; i < diskBlocks.length; i++) {
                if (!diskBlocks[i].isFree()) {
                    Object obj = diskBlocks[i].getObject();
                    if (diskBlocks[i].getType().equals(FATUtil.FOLDER)) {
                        if (((Folder) obj).getLocation().equals(folderPath)) {
                            // 文件夹不为空，不能删除
                            return 2;
                        }
                    } else {
                        if (((File) obj).getLocation().equals(folderPath)) {
                            // 文件夹不为空，不能删除
                            return 2;
                        }
                    }
                    if (diskBlocks[i].getType().equals(FATUtil.FOLDER)) {
                        if (((Folder) diskBlocks[i].getObject()).equals(block.getObject())) {
                            index = i;
                        }
                    }
                }
            }
            Folder thisFolder = (Folder) block.getObject();
            Folder parent = thisFolder.getParent();
            if (parent instanceof Folder) {
                parent.removeChildren(thisFolder);
                parent.setSize(FATUtil.getFolderSize(parent));
            }
            paths.remove(getPath(folderPath));
            diskBlocks[index].clearBlock();
            return 0;
        }



    }

    public void fotmatDiskblocks(){
        for (int i=2;i<diskBlocks.length;i++){
            diskBlocks[i].clearBlock();
        }
    }

    public boolean reallocBlocks(int num, DiskBlock block) {//文件长度发生变化，重新分配盘块。
        File thisFile = (File) block.getObject();
        int begin = thisFile.getDiskNum();
        int index = diskBlocks[begin].getIndex();
        int oldNum = 1;
        while (index != FATUtil.END) {
            oldNum++;
            if (diskBlocks[index].getIndex() == FATUtil.END) {
                begin = index;
            }
            index = diskBlocks[index].getIndex();
        }

        if (num > oldNum) {
            // 增加磁盘块
            int n = num - oldNum;
            if (freeBlocksCount() < n) {
                // 超过磁盘容量
                return false;
            }
            int space = searchEmptyDiskBlock();
            diskBlocks[begin].setIndex(space);
            for (int i = 1; i <= n; i++) {
                space = searchEmptyDiskBlock();
                if (i == n) {
                    diskBlocks[space].allocBlock(FATUtil.END, FATUtil.FILE, thisFile, false);
                } else {
                    diskBlocks[space].allocBlock(FATUtil.END, FATUtil.FILE, thisFile, false);// 同一个文件的所有磁盘块拥有相同的对象
                    int space2 = searchEmptyDiskBlock();
                    diskBlocks[space].setIndex(space2);
                }
                System.out.println(thisFile);
            }
        } else if (num < oldNum) {
            // 减少磁盘块
            int end = thisFile.getDiskNum();
            while (num > 1) {
                end = diskBlocks[end].getIndex();
                num--;
            }
            int next = 0;
            for (int i = diskBlocks[end].getIndex(); i != FATUtil.END; i = next) {
                next = diskBlocks[i].getIndex();
                diskBlocks[i].clearBlock();
            }
            diskBlocks[end].setIndex(FATUtil.END);
        } else {
            // 不变
        }
        thisFile.setLength(num);
        return true;
    }
    public DiskBlock[] getDiskBlocks() {
        return diskBlocks;
    }

    public void setDiskBlocks(DiskBlock[] diskBlocks) {
        this.diskBlocks = diskBlocks;
    }
    //从UI获取权限
    public DoubleProperty getRemainProperty(){return remainProperty;}
    public DoubleProperty getUsedProperty(){return usedProperty;}
    public DoubleProperty getTotalProperty(){return totalProperty;}



    public Path getPath(String path) {//给定路径名找对象。
        for (Path p : paths) {
            if (p.getPathName().equals(path)) {
                return p;
            }
        }
        return null;
    }


    public DiskBlock getBlock(int index) {
        return diskBlocks[index];
    }
    public ObservableList<File> getOpenedFiles() {
        return openedFiles;
    }

    public void setOpenedFiles(ObservableList<File> openFiles) {
        this.openedFiles = openFiles;
    }

    public List<Path> getPaths() {
        return paths;
    }
    public void setPaths(List<Path> paths) {
        this.paths = paths;
    }

    public void addPath(Path path) {
        paths.add(path);
    }

    public void removePath(Path path) {
        paths.remove(path);
        if (path.hasParent()) {
            path.getParent().removeChildren(path);
        }
    }



    public boolean hasName(String path, String name) {//判断指定路径下是否有同名文件夹或文件
        Folder thisFolder = getFolder(path);
        for (Object child : thisFolder.getChildren()) {
            if (child.toString().equals(name)) {
                return true;
            }
        }
        return false;
    }
    public void replacePath(Path oldPath, String newName) {
        oldPath.setPathName(newName);
    }








}


