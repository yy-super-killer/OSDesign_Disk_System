package view;

import javafx.fxml.FXMLLoader;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import model.*;
//import model.Folder;
import util.FATUtil;
//import ui.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class MainView {
    private FlowPane flowPane;
    private Scene scene;
    private HBox workBox, mainBox, locBox, chartBox;
    private VBox fullBox, rightBox;
    private ContextMenu contextMenu, contextMenu2;
    private MenuItem createFileItem, createFolderItem, openItem, renameItem, delItem, propItem;
    private TableView<File> openedTable;
    private TableView<DiskBlock> blockTable;
    private FXMLLoader loader,loader1;
    private Pane root,root1;
    private AnchorPane diskblockview;//锚点对象；
    private FAT fat;
    private List<DiskBlock> blockList;
    private Label[] icons;//标签图标数组
    private int index;
    private String recentPath;
    private Label locLabel,searchLabel;//设置路径名
    private TextField locField,searchField;
    private GridPane fatview;//用于diskblock视图；

    private Map<Path, TreeItem<String>> pathMap;//哈希表

    private Button gotoButton, backButton, stateButton,formatButton,searchButton;


    private TreeView<String> treeView;//显示文件树状试图控件；
    private TreeItem<String> rootNode, recentNode;
    private static final String DURATION_PATH = "disk";

    //饼图
    private PieChart romChart;
    private PieChart.Data remainArc;
    private PieChart.Data useArc;
    //显示磁盘空间信息
    private Label totalLabel;
    private Label remainLabel;
    private Label usedLabel;

    private Alert okAlert;

    private ObservableList<DiskBlock> dataBlock;//监听数组类的数据
    private ObservableList<File> dataOpened;


    public MainView(Stage stage) {
        pathMap = new HashMap<Path, TreeItem<String>>();
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(DURATION_PATH))) {
            fat = (FAT) inputStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (fat == null) {
            fat = new FAT();
        }
        recentPath = "C:";
        initFrame(stage);
    }


    private void initFrame(Stage stage) {
        //舞台布局；
        flowPane = new FlowPane();
        flowPane.setPrefSize(600, 100);
//        flowPane.setPrefHeight(150);

        flowPane.setStyle("-fx-background-color: #ffffff;" + "-fx-border-color: #ffffff;" + "-fx-border-width:0.5px;");//白色背景。
        flowPane.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent me) -> {//me是鼠标的位置，展示右键菜单栏。在空白处显示右键菜单栏。
            if (me.getButton() == MouseButton.SECONDARY && !contextMenu2.isShowing()) {
                contextMenu.show(flowPane, me.getScreenX(), me.getScreenY());
            }
            else {
                contextMenu.hide();
            }
        });
        loader = new FXMLLoader(getClass().getResource("/ui/chart.fxml"));
        try {
            root = loader.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
        romChart = (PieChart) root.lookup("#pieChart");//用于调用使用界面中的东西；
        blockTable=(TableView<DiskBlock>) root.lookup("#fatTable");

        initContextMeun();
        menuItemSetOnAction();
        initChart(stage);
        initTopBox();
        initTables();
        initTreeView();
        initDiskblockview();


        workBox = new HBox(diskblockview,flowPane, treeView);//横着往右边放一个
        rightBox = new VBox(workBox, openedTable);//竖着往下面放一个
//        mainBox = new HBox(blockTable, rightBox);
        mainBox = new HBox(rightBox);
        fullBox = new VBox(locBox, mainBox);//locBox是HBox类型的，放在HBOX布局的上面。
        chartBox = new HBox(fullBox, root);
        chartBox.setPrefSize(1500,700);//设置大小
        scene = new Scene(chartBox);
        stage.setScene(scene);
        stage.setResizable(false);//能否拉伸窗口。
        stage.getIcons().add(new Image(FATUtil.ICO));
        stage.setTitle("模拟磁盘文件系统");
        stage.show();
        stage.setOnCloseRequest(e -> {//写入fat进本地缓存。保存fat文件分配表。
            try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(DURATION_PATH))) {
                System.out.println("正在写入中。。。");
                outputStream.writeObject(fat);
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });


    }

    private void initContextMeun() {//右键菜单栏

        createFileItem = new MenuItem("新建文件");
        createFolderItem = new MenuItem("新建文件夹");
        //空白处右键显示的

        openItem = new MenuItem("打开");
        delItem = new MenuItem("删除");
        renameItem = new MenuItem("重命名");
        propItem = new MenuItem("属性");
        //对着文件夹显示的。
        contextMenu = new ContextMenu(createFileItem, createFolderItem);
        contextMenu2 = new ContextMenu(openItem, delItem, renameItem, propItem);
    }

    private void menuItemSetOnAction() { //右键选择点击触发事件。
        createFileItem.setOnAction(ActionEvent -> {//创建文件夹。
            int no = fat.createFile(recentPath);
            if (no == FATUtil.ERROR) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setHeaderText("磁盘容量已满，无法创建");
                alert.showAndWait();
            } else {
                flowPane.getChildren().removeAll(flowPane.getChildren());
                addIcon(fat.getBlockList(recentPath), recentPath);//获取该路径下的所有文件和文件夹
            }
            fat.freeBlocksCount();//让饼形图同步;
            updateDiskblockView();//同步磁盘图

        });
        createFolderItem.setOnAction(ActionEvent -> {//创建文件夹
            int no = fat.createFolder(recentPath);
            if (no == FATUtil.ERROR) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setHeaderText("磁盘容量已满，无法创建");
                alert.showAndWait();
            } else {
                Folder newFolder = (Folder) fat.getBlock(no).getObject();//按盘块号获取盘块。
                Path newPath = newFolder.getPath();
                flowPane.getChildren().removeAll(flowPane.getChildren());
                addIcon(fat.getBlockList(recentPath), recentPath);//位置其实是没有用的，获取这个路径的所有文件和文件
                addNode(recentNode, newPath);
            }
            fat.freeBlocksCount();
            updateDiskblockView();
        });

        openItem.setOnAction(ActionEvent -> {
            onOpen();
            fat.freeBlocksCount();
            updateDiskblockView();
        });

        delItem.setOnAction(ActionEvent -> {
            DiskBlock thisBlock = blockList.get(index);//通过block和index告知在哪里。哪个文件夹里的哪一个。
            if (fat.isOpenedFile(thisBlock)) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("文件未关闭");
                alert.showAndWait();
            } else {
                new delView(thisBlock, fat, MainView.this);
                flowPane.getChildren().removeAll(flowPane.getChildren());
                addIcon(fat.getBlockList(recentPath), recentPath);
            }
            fat.freeBlocksCount();
            updateDiskblockView();//是图片变化非数值变化，要手动加

        });
        renameItem.setOnAction(ActionEvent -> {
            DiskBlock thisBlock = blockList.get(index);
            new RenameView(thisBlock, fat, icons[index], pathMap);
            fat.freeBlocksCount();
            updateDiskblockView();
        });

        propItem.setOnAction(ActionEvent -> {
            DiskBlock thisBlock = blockList.get(index);
            new PropertyView(thisBlock, fat, icons[index], pathMap);
            fat.freeBlocksCount();
            updateDiskblockView();
        });


    }

    private void initTables()//初始化表格
    {
//        blockTable = new TableView<DiskBlock>();
        openedTable = new TableView<File>();

        blockTable.setStyle("-fx-background-color: #ffffff;" + "-fx-border-color: #9932CD;" + "-fx-border-width:0.5px;");
        dataBlock = FXCollections.observableArrayList(fat.getDiskBlocks());//数据监听//其实就是diskblocks数组
        dataOpened=fat.getOpenedFiles();
        TableColumn diskblocks=(TableColumn)blockTable.getColumns().get(0);

        diskblocks.setCellValueFactory(new PropertyValueFactory<DiskBlock,String>("noP"));
        diskblocks.setSortable(false);//不可排序
//        diskblocks.setMaxWidth(70);
//        diskblocks.setResizable(false);//可适应的；

        TableColumn value =blockTable.getColumns().get(1);
        value.setSortable(false);
        value.setCellValueFactory(new PropertyValueFactory<DiskBlock,String>("indexP"));
//        value.setMaxWidth(70);
//        value.setResizable(false);

        TableColumn type =blockTable.getColumns().get(2);
        type.setSortable(false);
        type.setCellValueFactory(new PropertyValueFactory<DiskBlock,String>("objectP"));
//        type.setMaxWidth(70);
//        type.setResizable(false);
        TableColumn nameCol = new TableColumn("文件名");
        nameCol.setCellValueFactory(new PropertyValueFactory<File, String>("fileNameP"));
        nameCol.setSortable(false);
        nameCol.setMinWidth(200);
        nameCol.setResizable(false);


        TableColumn flagCol = new TableColumn("打开方式");
        flagCol.setCellValueFactory(new PropertyValueFactory<File, String>("flagP"));
        flagCol.setSortable(false);
        flagCol.setResizable(false);
        flagCol.setMinWidth(230);

//        TableColumn diskCol = new TableColumn("起始盘块号");
//        diskCol.setCellValueFactory(new PropertyValueFactory<File, String>("diskNumP"));
//        diskCol.setSortable(false);
//        diskCol.setResizable(false);

        TableColumn pathCol = new TableColumn("路径");
        pathCol.setCellValueFactory(new PropertyValueFactory<File, String>("locationP"));
        pathCol.setSortable(false);
        pathCol.setMinWidth(420);
        pathCol.setResizable(false);

        TableColumn lengthCol = new TableColumn("文件长度");
        lengthCol.setCellValueFactory(new PropertyValueFactory<File, String>("lengthP"));
        lengthCol.setSortable(false);
        lengthCol.setResizable(false);
        lengthCol.setMinWidth(230);



//        TableColumn name=blockTable.getColumns().get(2);
//        name.setSortable(false);
//        name.setMaxWidth(70);
//        name.setResizable(false);
        blockTable.setItems(dataBlock);
        blockTable.setEditable(false);
//        blockTable.setPrefWidth(300);

        openedTable.setItems(dataOpened);
        openedTable.getColumns().addAll(nameCol,flagCol,pathCol,lengthCol);
//        openedTable.setPrefHeight(200);


    }

    private void initDiskblockview(){
        loader1 = new FXMLLoader(getClass().getResource("/ui/diskblock.fxml"));
        try {
            root1 = loader1.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
        diskblockview = (AnchorPane) root1.lookup("#diskblock");//用于调用使用界面中的东西；
        fatview=(GridPane) root1.lookup("#fatView");
        updateDiskblockView();
//        blockTable=(TableView<DiskBlock>) root.lookup("#fatTable");

    }

    public void updateDiskblockView() {
        for(int i=0;i<dataBlock.size();i++){
            Pane pane=(Pane) fatview.getChildren().get(i);
            if(dataBlock.get(i).isFree()){
                pane.setStyle("-fx-background-color:Magenta ;-fx-border-color: #EEEEBB");
            }
            else{
                pane.setStyle("-fx-background-color:DeepSkyBlue;-fx-border-color: #EEEEBB");
            }

        }
    }

    private void initTreeView() {//初始话treeView;
        rootNode = new TreeItem<>("C:", new ImageView(FATUtil.DISK_IMG));
        rootNode.setExpanded(true);
        recentNode = rootNode;//最近节点就是根节点。
        pathMap.put(fat.getPath("C:"), rootNode);//在路径图中加入节点；

        treeView = new TreeView<String>(rootNode);
        treeView.setPrefWidth(250);
        treeView.setCellFactory((TreeView<String> p) -> new TextFieldTreeCellImpl());
        treeView.setStyle("-fx-background-color: #9932CD;" + "-fx-border-color:  #FF6EC7  ;" + "-fx-border-width:0.5px;");
        for (Path path : fat.getPaths()) {
            System.out.println(path);
            if (path.hasParent() && path.getParent().getPathName().equals(rootNode.getValue())) {
                initTreeNode(path, rootNode);
            }
        }
        addIcon(fat.getBlockList(recentPath), recentPath);


    }
    private void initTreeNode(Path newPath, TreeItem<String> parentNode) {
        TreeItem<String> newNode = addNode(parentNode, newPath);
        if (newPath.hasChild()) {
            for (Path child : newPath.getChildren()) {
                initTreeNode(child, newNode);//递归加节点。
            }
        }
    }


    private void initTopBox() {//顶上按纽栏目的信息。
        //可以在此处添加横栏的按钮。//加点空label
        locLabel = new Label("文件目录：");
        locLabel.setStyle("-fx-font-weight: bold;" + "-fx-font-size: 16px");
        searchLabel = new Label("文件查找：");
        searchLabel.setStyle("-fx-font-weight: bold;" + "-fx-font-size: 16px");
        locField = new TextField("C:");
        locField.setPrefWidth(400);
        searchField=new TextField();

        backButton = new Button();
        stateButton = new Button();
        gotoButton = new Button();
        searchButton=new Button();
        locBox = new HBox(stateButton, backButton, locLabel, locField, gotoButton,searchLabel,searchField,searchButton);

//        stateButton.setText("作者信息");


        stateButton.setGraphic(new ImageView(FATUtil.INTRODUCTION_IMG));

        stateButton.setOnAction(ActionEvent->{
            new IntroductionView();
        });
        stateButton.setStyle("-fx-background-color: #ffffff;");
        stateButton.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                stateButton.setStyle("-fx-background-color:  #9370DB");
            }
        });
        stateButton.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                stateButton.setStyle("-fx-background-color: #ffffff;");
            }
        });



        backButton.setOnAction(ActionEvent -> {
            Path backPath = fat.getPath(recentPath).getParent();//返回该路径的父文件夹
            if (backPath != null) {
                List<DiskBlock> blocks = fat.getBlockList(backPath.getPathName());//通过路径返回所有文件和文件夹
                flowPane.getChildren().removeAll(flowPane.getChildren());//清空
                addIcon(blocks, backPath.getPathName());//第二个变量无用
                recentPath = backPath.getPathName();
                recentNode = pathMap.get(backPath);
                locField.setText(recentPath);
            }
        });
        backButton.setGraphic(new ImageView(FATUtil.BACK_IMG));
        backButton.setStyle("-fx-background-color: #ffffff;");
        backButton.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                backButton.setStyle("-fx-background-color:  #9370DB");
            }
        });
        backButton.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                backButton.setStyle("-fx-background-color: #ffffff;");
            }
        });

        gotoButton.setOnAction(ActionEvent -> {
            String textPath = locField.getText();
            Path gotoPath = fat.getPath(textPath);//从路径名获取路径对象
            if (gotoPath != null) {
                List<DiskBlock> blocks = fat.getBlockList(textPath);//输入的路径名
                flowPane.getChildren().removeAll(flowPane.getChildren());
                addIcon(blocks, textPath);
                recentPath = textPath;//更新路径；
                recentNode = pathMap.get(gotoPath);//修改最近节点。
            } else {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setContentText("目录不存在");
                alert.setHeaderText(null);
                alert.show();
                locField.setText(recentPath);
            }
        });
        gotoButton.setGraphic(new ImageView(FATUtil.FORWARD_IMG));
        gotoButton.setStyle("-fx-background-color: #ffffff;");
        gotoButton.setOnMouseEntered(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                gotoButton.setStyle("-fx-background-color: #9370DB;");
            }
        });
        gotoButton.setOnMouseExited(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                gotoButton.setStyle("-fx-background-color: #ffffff;");
            }
        });


        searchButton.setGraphic(new ImageView(FATUtil.SEARCH_IMG));

        searchButton.setOnAction(ActionEvent->{
            String Filename=searchField.getText();

            if(Filename!=null){
                List<DiskBlock> blocks = fat.getSamename(Filename);//输入的路径名

                if(!blocks.isEmpty())
                {
                    flowPane.getChildren().removeAll(flowPane.getChildren());
                    addIcon(blocks, null);
                }
                else{
                    Alert erralert=new Alert(AlertType.ERROR);
                    erralert.setHeaderText(null);
                    erralert.setContentText("该文件不存在！");
                    erralert.showAndWait();
                }
//                recentPath = textPath;//更新路径；
//                recentNode = pathMap.get(gotoPath);//修改最近节点。
            }
            else{
                Alert erralert=new Alert(AlertType.ERROR);
                erralert.setHeaderText(null);
                erralert.setContentText("请输入！");
                erralert.showAndWait();
            }


        });
        searchButton.setStyle("-fx-background-color: #ffffff;");
        searchButton.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                searchButton.setStyle("-fx-background-color:  #9370DB");
            }
        });
        searchButton.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                searchButton.setStyle("-fx-background-color: #ffffff;");
            }
        });

        locBox.setStyle("-fx-background-color: #ffffff;" + "-fx-border-color: #d3d3d3;" + "-fx-border-width:0.5px;");
        locBox.setSpacing(20);//空间间隔
        locBox.setPadding(new Insets(5, 5, 5, 5));//上下间隔

    }

    private void initChart(Stage stage) {
//        AtomicReference<Alert> okAlert= new AtomicReference<>(new Alert(AlertType.INFORMATION));
        okAlert = new Alert(Alert.AlertType.INFORMATION);
        okAlert.setTitle("成功");
        okAlert.setHeaderText(null);


        remainArc = new PieChart.Data("剩余空间", fat.freeBlocksCount());
        useArc = new PieChart.Data("已用空间", fat.usedBlocksCount());

        totalLabel=(Label) root.lookup("#total");
        remainLabel=(Label) root.lookup("#remain");
        usedLabel=(Label) root.lookup("#used");

        formatButton=(Button) root.lookup("#formatBtn");
        fat.freeBlocksCount();

        formatButton.setOnAction(ActionEvent->{


            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("磁盘格式化");
            alert.setHeaderText(null);
            alert.setContentText("确认格式化磁盘？");
            ButtonType confirmType = new ButtonType("确认");
            ButtonType noType = new ButtonType("取消");
            ButtonType cancelType = new ButtonType("退出", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(confirmType, noType, cancelType);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == confirmType) {
                fat.fotmatDiskblocks();
                updateDiskblockView();
                fat.freeBlocksCount();
                rootNode.getChildren().removeAll(rootNode.getChildren());
                pathMap=null;

                flowPane.getChildren().removeAll(flowPane.getChildren());
                locField.setText("C:");
                fat=null;
                okAlert.setContentText("格式化完成，请重启系统！");

                ButtonType conType=new ButtonType("确认");
                okAlert.getButtonTypes().setAll(conType);

                Optional<ButtonType> okresult = okAlert.showAndWait();
                if(okresult.get()==conType){
                    stage.close();
                    try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(DURATION_PATH))) {
                        System.out.println("正在写入中。。。");
                        outputStream.writeObject(fat);
                    } catch (FileNotFoundException fileNotFoundException) {
                            fileNotFoundException.printStackTrace();
                    } catch (IOException ioException) {
                            ioException.printStackTrace();
                    }


                }



            } else if (result.get() == cancelType) {
//                isCancel = true;
            }
//            initChart();
//            initTopBox();
//            initTreeView();


        });



        remainArc.pieValueProperty().bind(fat.getRemainProperty());
        useArc.pieValueProperty().bind(fat.getUsedProperty());
        totalLabel.textProperty().bind(fat.getTotalProperty().multiply(64).asString().concat("KB"));
        remainLabel.textProperty().bind(fat.getRemainProperty().multiply(64).asString().concat("KB"));
        usedLabel.textProperty().bind(fat.getUsedProperty().multiply(64).asString().concat("KB"));
        ObservableList<PieChart.Data> list = FXCollections.observableArrayList(remainArc, useArc);//加入监听列表。
        romChart.setData(list);
        romChart.setTitle("磁盘空间使用情况");

//        TabPane tabpane=(TabPane)root.lookup("#tabPane");

        final Label caption = new Label("");//caption是这一整块布局里的，0从布局的角落开始算，但是mouse是从整个开始算的
        root.getChildren().add(caption);
        caption.setTextFill(Color.LIME);
        caption.setStyle("-fx-font: 24 arial;");

        for (final PieChart.Data data : romChart.getData()) {
            data.getNode().addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {//有执行但是不显现出来？？？
                @Override
                public void handle(MouseEvent e) {
                    caption.setVisible(true);
//                    caption.setTranslateX(e.getSceneX());
//                    System.out.println(e.getSceneX()+"   "+e.getSceneY());
                    caption.setTranslateX(150);
                    caption.setTranslateY(e.getSceneY());
                    caption.setText(String.format("%.2f", data.getPieValue()*100/128) + "%");
//                    System.out.println("YYYYY");
                    //caption.setText(String.valueOf(data.getPieValue()) + "%");
                }
            });
            data.getNode().addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    caption.setVisible(false);
//                    System.out.println("KKK");

                }
            });
        }




    }

    public Button getFormatButton(){return formatButton;}
    public void setFormatButton(Button formatButton){this.formatButton=formatButton;}

    public FAT getfat(){return fat;}
    public void setFAT(FAT fat){this.fat=fat;}

    private void addIcon(List<DiskBlock> bList, String path) {//path起始是没有用的//获取该地址下所有文件和文件夹，然后加图标以及鼠标活动
        blockList = bList;//在这里设置blocklist
        int n = bList.size();//磁盘块大小的list
        icons = new Label[n];//label类型//图标数组的每一个退保对应一个文件活这文件夹。
        for (int i = 0; i < n; i++) {
            if (bList.get(i).getObject() instanceof Folder) {
                icons[i] = new Label(((Folder) bList.get(i).getObject()).getFolderName(),
                        new ImageView(FATUtil.FOLDER_IMG));//贴文件夹图片,少了后面这半句new的话只有文字标签，没有图标。
            } else {
                icons[i] = new Label(((File) bList.get(i).getObject()).getFileName(), new ImageView(FATUtil.FILE_IMG));//贴文件的图标
            }
            icons[i].setContentDisplay(ContentDisplay.TOP);
            icons[i].setWrapText(true);
            flowPane.getChildren().add(icons[i]);//往流式布局中添加标签。
            icons[i].setOnMouseEntered(new EventHandler<MouseEvent>() {//鼠标点击响应时间。

                @Override
                public void handle(MouseEvent event) {
                    ((Label) event.getSource()).setStyle("-fx-background-color: #FF6EC7 ;");//霓虹粉色特效，
                }
            });//移动鼠标到上面的话，加入灰边特效
            icons[i].setOnMouseExited(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    ((Label) event.getSource()).setStyle("-fx-background-color: #ffffff;");//鼠标离开后
                }
            });
            icons[i].setOnMouseClicked(new EventHandler<MouseEvent>() {//鼠标双击覆盖原来的函数

                @Override
                public void handle(MouseEvent event) {//event是鼠标活动的变量
                    Label src = (Label) event.getSource();//图标活动点击到的图标。
                    for (int j = 0; j < n; j++) {
                        if (src == icons[j]) {
                            index = j;//传给OPEN（）好让他知道打开哪一个。
                        }
                    }//图标数组中寻找该数组。
                    if (event.getButton() == MouseButton.SECONDARY && event.getClickCount() == 1) {//右键单机的话。
                        contextMenu2.show(src, event.getScreenX(), event.getScreenY());//展示右键第二菜单栏，位于src这个图标上。第一个变量是在哪触发，后面是在哪展示
                    } else if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {//双击
                        onOpen();//双击进入图标
                        updateDiskblockView();

                    } else {
                        contextMenu2.hide();//其他鼠标活动，隐藏菜单
                    }
                }
            });
        }
    }

    private void onOpen() {//打开文件或者进入文件夹子，通过blocklist和index让他知道打开哪一个。
        DiskBlock thisBlock = blockList.get(index);//获取磁盘块的值
        for (DiskBlock block : blockList) {//打印磁盘块
            System.out.println(block);//重写了toString，返回这一层的文件和文件夹的名字
        }
        if (thisBlock.getObject() instanceof File) {//如果是文件
            if (fat.getOpenedFiles().size() < 5) {//最多同时打开五个文件
                if (fat.isOpenedFile(thisBlock)) {
                    Alert duplicate = new Alert(AlertType.ERROR, "文件已打开");//警告信息
                    duplicate.showAndWait();
                } else {
                    fat.addOpenedFile(thisBlock);//此处
                    new FileView((File) thisBlock.getObject(), fat, thisBlock,MainView.this);
                }
            } else {
                Alert exceed = new Alert(AlertType.ERROR, "文件打开已到上限");
                exceed.showAndWait();
            }
        } else {//如果是文件夹
            Folder thisFolder = (Folder) thisBlock.getObject();
            String newPath = thisFolder.getLocation() + "\\" + thisFolder.getFolderName();//本来的路径名加上这个名字就是新路径
            flowPane.getChildren().removeAll(flowPane.getChildren());
            addIcon(fat.getBlockList(newPath), newPath);
            locField.setText(newPath);//修改路径名
            recentPath = newPath;
            recentNode = pathMap.get(thisFolder.getPath());//更新目前节点，pathmap路径与节点。
        }
        fat.freeBlocksCount();
        updateDiskblockView();
    }




        private TreeItem<String> addNode(TreeItem<String> parentNode, Path newPath) {//往treeItem里增加节点。
            String pathName = newPath.getPathName();//新路径的名字
            String value = pathName.substring(pathName.lastIndexOf('\\') + 1);//该文件，文件夹的名字
            TreeItem<String> newNode = new TreeItem<String>(value, new ImageView(FATUtil.TREE_NODE_IMG));//添加新节点
            newNode.setExpanded(true);
            pathMap.put(newPath, newNode);//路劲图，路径对应节点。
            parentNode.getChildren().add(newNode);//在父节点上加新的节点。
            return newNode;
        }

        public void removeNode(TreeItem<String> recentNode, Path remPath) {
            recentNode.getChildren().remove(pathMap.get(remPath));
            pathMap.remove(remPath);
        }//从里里面删除节点

        public TreeItem<String> getRecentNode() {
            return recentNode;
        }


    public final class TextFieldTreeCellImpl extends TreeCell<String> {//最终类设置treeview活动

        private TextField textField;

        public TextFieldTreeCellImpl() {

            this.setOnMouseClicked(new EventHandler<MouseEvent>() {//点击节点事件
                @Override
                public void handle(MouseEvent event) {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                        if (getTreeItem() != null) {
                            String pathName = null;
                            for (Map.Entry<Path, TreeItem<String>> entry : pathMap.entrySet()) {//遍历图找到与只对应的路径，返回该treeitem对应的路径名。
                                if (getTreeItem() == entry.getValue()) {
                                    pathName = entry.getKey().getPathName();
                                    break;
                                }
                            }
                            List<DiskBlock> fats = fat.getBlockList(pathName);
                            flowPane.getChildren().removeAll(flowPane.getChildren());
                            addIcon(fats, pathName);
                            recentPath = pathName;
                            recentNode = getTreeItem();
                            locField.setText(recentPath);//页面跳转函数。
                        }
                    }
                }
            });
        }

        @Override
        public void startEdit() {
            super.startEdit();

            if (textField == null) {
                createTextField();
            }
            setText(null);
            setGraphic(textField);
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText((String) getItem());
            setGraphic(getTreeItem().getGraphic());
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(getTreeItem().getGraphic());
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setOnKeyReleased((KeyEvent t) -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(textField.getText());
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });

        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }

    }

}











