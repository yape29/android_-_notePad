# Android_notePad 期中实验  
## 项目介绍

这是一个基于Android原生[NotePad](https://github.com/llfjfz/NotePad)应用程序进行功能扩展的笔记本应用。在保留原有的笔记创建、编辑、删除等基础功能上，增加了笔记分类、搜索、收藏等实用功能，并进行UI统一设计美化。  
## 项目结构
app/  
├── src/  
│ ├── main/  
│ │ ├── java/com/example/android/notepad/  
│ │ │ ├── NoteEditor.java		       # 笔记编辑界面  
│ │ │ ├── NotesList.java 			# 笔记列表界面  
│ │ │ ├── NotePadProvider.java	      # 数据访问提供者  
│ │ │ └── NotePad.java			  # 常量定义  
│ │ │ └── MyAdapter				# 自定义适配器  
│ │ ├── res/  
│ │ │ ├── layout/ # 界面布局文件  
│ │ │ ├── drawable/ # 图片资源  
│ │ │ ├── values/ # 资源文件  
│ │ │ └── menu/ # 菜单文件  
│ │ └── AndroidManifest.xml # 应用配置文件  
│ └── test/ # 测试代码目录  
└── build.gradle # 项目构建配置  

## 功能特点
- 基础功能
  - 创建笔记
  - 编辑笔记
  - 删除笔记
  - 查看笔记列表

- 扩展功能
  - 添加时间戳显示笔记最后修改时间
  - 支持批量删除笔记
  - 收藏笔记功能
    - 将笔记设置为收藏
    - 查看所有被收藏笔记
    - 查看分类中被收藏笔记
  - 笔记分类管理
    - 创建新分类
    - 为笔记设置分类
    - 按分类查看笔记
    - 删除分类所属笔记以及分类
  - 搜索笔记功能
    - 可以按标题对笔记进行搜索
    - 支持模糊搜索
  - 美化界面
    - 自定义对话框样式
    - 浮动按钮设计功能菜单
    - 简洁化UI设计
## 技术特点
  - 基于Android原生ContentProvider实现数据存储
  - 使用SQLite数据库
  - 使用CardView、FloatingActionButton等视图组件
## 数据库设计
### Notes表
| 列名 | 类型 | 说明 |
|:-----|------|------|
| _id | INTEGER | 主键 |
| title | TEXT | 笔记标题 |
| note | TEXT | 笔记内容 |
| created | INTEGER | 创建时间 |
| modified | INTEGER | 修改时间 |
| star | INTEGER | 收藏 |
| classify_name | TEXT | 分类名称 |

### Classify表
| 列名 | 类型 | 说明 |
|------|------|------|
| _id | INTEGER | 主键 |
| name | TEXT | 分类名称 |

## 使用说明
### 添加笔记  
- 图片演示  
    1. 展开功能栏
    2. 点击添加笔记按钮
    3. 编辑笔记
    4. 添加
    5. 首页  
    ![第一步](.\images\1.png "展开功能栏")  
    ![第二步](.\images\2.png "创建笔记")   
    ![第三步](.\images\3.png "编辑笔记")   
    ![第三步](.\images\4.png "成功添加笔记")   
    ![第三步](.\images\5.png "成功添加笔记")  
### 删除笔记  
- 图片演示
    1. 点击进入笔记
    2. 点击展开功能列，并点击删除
    3. 首页  
    ![](.\images\6.png "")  
    ![](.\images\7.png "")  
### 时间戳
- 图片展示，每个笔记下都有一个时间戳，用来显示最近修改时间  
    ![](.\images\8.png "")  
### 批量删除
- 图片演示
    1. 长按笔记
    2. 多选(当没有笔记被选中时，退出批量删除)
    3. 点击删除  
    ![](.\images\9.png "")  
    ![](.\images\10.png "")  
### 搜索笔记
- 图片演示
    1. 点击功能列表，点击搜索按钮，会从顶部弹出一个搜索框
    2. 在搜索框输入标题，支持模糊查询，如果输入为空，查询结果为所有笔记
    3. 点击搜索，展示查询到笔记  
    ![](.\images\11.jpg "")  
    ![](.\images\12.jpg "")  
    ![](.\images\13.jpg "")   
### 收藏笔记
- 图片演示
    1. 点击收藏，点击已收藏笔记取消收藏
    2.  操作结果  
    ![](.\images\14.png "")  
    ![](.\images\15.png "")
### 展示所有收藏笔记
- 图片演示
    1. 点击展开功能列表，点击我的收藏按钮
    2. 展示所有收藏笔记  
    ![](.\images\16.png "")  
    ![](.\images\17.png "")  
### 分类功能
- 首页展示分类文件夹
  1. 展开功能栏
  2. 点击展示分类，展示包含：分类名称、包含笔记数量
  3. 点击进入分类夹
  4. 页面展示该分类下所有笔记  
  ![18](.\images\18.jpg)  
  ![](.\images\19.jpg)  
  ![](.\images\20.jpg)  
- 为笔记添加/修改所属分类
  1. 点开笔记
  2. 点开功能列表
  3. 点击分类
  4. 选择现有分类或添加新分类
  5. 保存笔记
  6. 返回分类视图，可以看到刚才添加的分类，里面有刚才的笔记  
  ![](.\images\21.jpg)  
  ![](.\images\24.jpg)   
  ![](.\images\22.jpg)  
  ![](.\images\23.jpg)  
- 删除分类及分类下所属所有笔记
 1. 在分类列表中长按分类
 2. 选中一个或多个分类
 3. 点击删除按钮进行删除分类以及分类下所有笔记  
![](.\images\24.jpg)  
![](.\images\25.jpg)  
![](.\images\26.jpg)  
![](.\images\27.jpg)  
