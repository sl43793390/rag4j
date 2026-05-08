
当前这个项目是要实现用户通过浏览器打开页面可以实现各类文件的嵌入，通过选择文本切割方法、嵌入模型等实现嵌入，同时支持嵌入后在页面对嵌入的知识进行相似查询，确认是否嵌入成功。
目前实现以下功能:
1. 使用vaadin24实现登录页面，数据库暂时使用sqllite。实现登录后跳转到嵌入页面。
2. 嵌入页面的布局是上方有输入框可搜索已经创建的知识库，右侧有搜索、新建知识库等按钮，下方是一个表格，展示已经创建好的知识库，表头包含知识库名称、切割方式、嵌入模型、查询等信息，其中查询是个按钮，点击可跳转到新页面，可以针对当前这个知识库进行查询对话
3. 实现查询对话页面功能，使用当前选择的知识库。
4. 提供对外调用的接口，方便其它应用使用接口也能实现rag服务。
5. 如有其他细节未说明，可以提出

修改：
1. 帮我增加模型对话的记忆机制，实现SqlLiteChatMemoryStore类。
2. 帮我增加使用milvus lite 轻量版，能够不需要安装直接运行可以方便测试使用。
3. 帮我增加获取用户信息的方法，在用户登录后保存到当前会话对象中，在其它用到的地方取出使用
4. 检查语法错误，符合jdk21语法


com.sl.config包下的类有些编译错误，帮我修正，此外帮我修改页面上的嵌入模型用户不可选择，只保留配置文件配置项，
帮我增加使用milvus lite 轻量版，能够不需要安装直接运行可以方便测试使用，不使用原来的远程连接方式，但保持兼容

将user表扩展和修改，原表的SQL中user修改为users，增加字段：user_type/flag_status/email/phone/等
在loginView.java中handleLogin方法添加逻辑：在数据库users表中查询用户信息，然后使用sm3加密方法将用户输入的密码和数据库中的比对，登录成功后将用户信息放入到会话中.
帮我修复RagQueryService类中的编译错误


针对MilvusService.java这个类，帮我修复报错，并实现对milvus的增删改查等常见操作。

不需要编译和执行代码测试

帮我把这个类中的jdbcTemplate模板方式构造的sql查询方法全部替换成mybatis-plus方式：src/main/java/com/sl/rag4j/config/SqlLiteChatMemoryStore.java
不需要编译和执行代码测试，完成代码，确保正确即可。

帮我修改这个类：src/main/java/com/sl/rag4j/ui/view/QueryConversationView.java
1. 布局修改为左侧展示当前用户在这个知识库下的所有聊天历史记录的列表，到chat_memory表查询按照memoryId倒序排列，列表名称使用chat_memory表的chat_title字段，查询条件是user_name 和根据知识库ID，也就是当前页面接受到的变量：kbId
2. 点击列表可以切换回话内容
3. 默认加载回话内容为第一条会话，列表样式显示为选中状态，根据用户点击切换状态和会话内容
   不需要编译和执行代码测试，完成代码，确保正确即可。


修改知识库管理和创建的弹窗页面：
1. 知识库管理页面：src/main/java/com/sl/rag4j/ui/view/KnowledgeBaseListView.java，在页面中的表格展示的每一行知识库信息中增加修改按钮，支持修改知识库名称、描述，支持继续上传文件进行嵌入到当前这个知识库，
但是其它内容不支持修改，修改的弹窗复用新增知识库的弹窗，做一些逻辑调整即可。
2. 知识库新增弹窗类：src/main/java/com/sl/rag4j/ui/component/CreateKnowledgeBaseDialog.java
   不需要编译和执行代码测试，完成代码，确保正确即可。

修改知识库对话页面：
1. src/main/java/com/sl/rag4j/ui/view/QueryConversationView.java这个页面顶部rag4j后面增加一个按钮可以返回知识库管理页面。
2. 在对话区域中，根据用户或者AI的聊天内容，在内容前面增加一个圆形图标，中间显示AI或者用户的user_name前两个字母，使用Avatar可以实现

修改知识库对话页面：
1. src/main/java/com/sl/rag4j/ui/view/QueryConversationView.java，让这个页面支持markdown格式展示。
2. 将返回知识库按钮放到和登出按钮一样的高度位置，只不过这个按钮放到页面左侧，而登出按钮在右侧
3. 添加对每个历史会话的删除功能
   不需要编译和执行代码测试，完成代码，确保正确即可。

修改知识库管理页面：
1. src/main/java/com/sl/rag4j/ui/view/QueryConversationView.java，进入这个页面获取顶部的标题栏对象：src/main/java/com/sl/rag4j/ui/component/MainLayout.java
调用其中的getBackToKbBtn（）方法，然后把按钮设置为不显示。进入src/main/java/com/sl/rag4j/ui/view/QueryConversationView.java页面时再调用一次设置为显示。

修改知识库对话页面：
1. src/main/java/com/sl/rag4j/ui/view/QueryConversationView.java第392行返回flux类型。
2. 将flux类型的数据进行展示
3. 通过milvus数据库相似查询后结果默认会有5条，将得分最高的前2条发送给大模型，将模型回复的结果展示到会话中。
   不需要编译和执行代码测试，完成代码，确保正确即可。
























