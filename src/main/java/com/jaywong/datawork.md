数仓开发流程：
1、数据、需求调研：需求了解沟通，业务调研，需求分析，确定数据源
2、明确数据域：根据业务需求进行主题域的划分，每条业务线所有的核心业务拆解出来，拆解成一个个不可拆分的事件行为，比如。。。。，罗列出来后开始划分主题域，把相同，相似，有联系的业务过程归纳总结出来，
归到不同的主题域下面。
3、构建总线矩阵：做完主题域和业务过程的归纳总结之后，做了一个总线矩阵设计，把每个主题域下的业务过程抽象出维度，每个业务过程分别和哪些维度相关联，做了一个映射匹配，比如 **业务过程是和**ID，
**场景等等维度有关
4、维度模型设计：建设完主题域，业务过程和总线矩阵之后，开始维度建模的一个设计，包括维度建模的分层，首先是ODS层，这一层不会做任何的数据处理，原始数据是什么样的，ODS层的数据就是什么样的，
起到一个原始数据备份的作用，这一层主要分成了三个方面进行数据的接入，包括: 1.日志行为，2.业务库的业务数据，3.行外外部数据。然后是明细DWD层，这一层我们的设计原则是尽量以最细粒度去设计，
比如用户的每一条行为日志最为一行数据，尽量把维度最细粒度化，比如用户以天或者以小时为单位的日志行为，都放到明细表里。汇总层DWS的话，一般是把主题域下所有业务过程相关的指标都汇总到一张表里，
让维度也尽量最细粒度，这样后续用起来也会更加灵活一些，ADS层，是比较个性化的层，一般是与报表做关联，没有公共共享的能力
5、上线部署优化：设计完主题域，业务过程，维度分层以后，敲定下来需要建设哪些表以后，会做一个物理模型的开发，也就是ETL开发，这一侧包括需要去相应的平台注册表Schema，维度schema等，然后平台做ETL的开发，
之后就是例行调度任务部署上线，以及后续的验证维护优化。



划分主题域过程：
1、梳理业务过程，理解业务需求
2、把业务过程，包括用户的一些日志行为，以最细粒度的拆分出来，拆解成一个个不可拆分的事件行为
3、最后会与业务同事一起，把相同相似或者有联系的事件归纳在一起，然后归结到已命名完成的主题域下


零售集市8大主题域：客户、资产、贷款、产品、营销、交易、内部管理、外部数据



优化了运行效率低的ETL任务：具体量化（优化了多少的任务，提升了多少的效率，减少了多少的存储空间），如何去优化（技术层面、模型层面）
1、模型层面: 有的数据使用的是底层的ODS表，或者DWD明细表，但此时是已经有了轻度汇总表DWS，所以会使用DWS表进行对他们的替换，数据量就会得到大幅度的减少，以达到节约效率的目的
2、代码层面: select * 更换为具体字段，数据持久化，cache 到内存当中
3、参数层面: 调整spark参数，调整executor memory, executor core, driver memory等等
4、小文件处理: 如果没有设置文件输出个数的话，最后的文件会以很多小文件的方式写出，导致后面需要读取该文件处理数据的时候可能会造成IO异常。这时就可以用coalesce或者repartition
                去设置文件最后输出的结果个数


数仓调优：

1、调度优化（生产测试环境分离、任务优先级划分、提前跑、减少层级依赖）

2、模型优化 （模型选择、拆表、合表、中间层建设、合理分区、拉链表）

3、计算优化（减少输入数据、避免数据倾斜）

4、同步优化（合理参数设置）


spark调优：
1、参数调优：
    Executor core, Executor memory

2、高性能算子调优：
    mapPartition
    foreachPartition
    filter后使用coalsesce减少分区数量
    persist / cache 对数据持久化
    reduceByKey和aggrateByKey取代groupByKey
    repartition解决sparksql低并行度的性能问题

3、开发调优：
    广播变量(broadcast) 结合业务说明
    尽量避免复用同一个RDD，如果需要，则进行cache持久化
    尽量避免shuffle类算子(reduceByKey, join, distinct, repartition)
    使用kryo优化序列化性能


交易业务流程：
1、首先我们会跟据用户的一个风险等级，不同的产品在不同的渠道进行展示宣传，比如手机银行APP展示，线下网点客户经理VIP宣传，网点柜面，招呼，短信，988888等等对客户介绍产品的功能点，此时就会记录
    一些类似像渠道的数据
2、然后用户会根据不同的渠道点击外链进入手机银行APP的产品详情页，此时就会生成一些点击记录，这其实就可以分析出一些基础指标，比如客户对于产品的粘度
3、然后就是进入购买流程，此时会自动选卡，判断卡号是否在用户的卡列表，然后选择分仓号，是属于组合分仓还是大湾区分仓，然后判断出交易是否被允许
4、交易被允许后会判断是否购买产品有白名单控制，账户是否为白名单，如果用户为黑名单，则不允许被购买
5、用户可购买的话，产品会需要签署合同或者协议，并且判断用户风评是否有效，是否购买产品为超风险产品
6、进入购买主视图，用户输入金额，输入金额是否高于账户余额，首次购买是否达到购买下限
7、用户提交购买申请，签署电子合同，执行购买，购买成功


缓慢变化维处理方式：
    1、直接覆盖原始值：最简单粗暴，但无法做历史分析
    2、增加属性列：新增一列，用来记录变化。适合变化较少的情况，如果经常变化，增加无限量字段明显不合适
    3、增加维度行：新增一条新纪录，并且用一个专门的字段（时间、版本、是否生效等）进行标识，区分哪个数据是最新的，绝大部分适用


3种数据来源：
1、业务系统数据，存放在mysql, db2, oracle等
2、日志系统
3、外部数据源：SFTP  .DAT格式存放

数据同步方式：
1、直连数据库：sqoop数据库同步
2、数据库日志解析：mysql数据库 binlog日志同步
3、数据文件同步：SFTP数据同步

维度建模基本原则（也可作为判断模型是否好坏的依据）
1）高内聚低耦合：数据业务特性和访问特特性两个角度来考虑 ：将业务相近或者相关、粒度相同的数据设计为一个逻辑或者物理模型：将高概率同时访问的数据放一起 ，将低概率同时访问的数据分开存储。
--分享解读：请注意，是两个角度，业务特性和访问特性兼顾。这里在数据治理里专门有人做模型离散度治理，这个是保障模型好用健康的一个度量方式。

2）核心模型与扩展模型分离：建立核心模型与扩展模型体系，核心模型包括的宇段支持常用的核心业务，扩展模型包括的字段支持个性化或少量应用的需要 ，不能让扩展模型的宇段过度侵人核心模型，以免破坏核心模型的架构简洁性与可维护性。
--分享解读：我们重构业务数据中间层的时候，订单事件表就是遵循的这个原则，同时还能保障故障的隔离和SLA能力。

3）公共处理逻辑下沉及单一：越是底层公用的处理逻辑越应该在数据调度依赖的底层进行封装与实现，不要让公用的处理逻辑暴露给应用层实现，不要让公共逻辑多处同时存在。
--分享解读：具体到dd我们惯用的说法是“口径收口”，这个一定要在dwd层和dwm层完成，不允许在应用层做。指标体系治理是做业务支持的数据开发t团队最痛的点，最有效的办法就是"下沉且单一"

4）成本与性能平衡：适当的数据冗余可换取查询和刷新性能，不宜过度冗余与数据复制。
--分享解读：我们业务数据中间层重构前的dwd_order_event_d就太多的照顾了业务的个性化需求，而没有遵循这一点，导致平均每个订单数据行数膨胀6倍。极大的影响了数据运算和产出效率。

5）数据可回滚：处理逻辑不变，在不同时间多次运行数据结果确定不变。

6）一致性：具有相同含义的字段在不同表中的命名必须相同，必须使用规范定义中的名称。
--分享解读：说起来容易做起来难，做到全局一致性是不可能的，但是局部一致性是我们要努力做的事情。只有规范很难落地，需要结合具体的管控工具和考核办法去协力完成。

7）命名清晰、可理解：表命名需清晰、一致，表名需易于消费者理解和使用。


维度设计基本方法：
1）选择或者定义一个维度，维度必须是唯一的。
2）确定主维表
3）确定相关维度表
4）确定维度属性：
​ 1、尽可能生成丰富的维度属性：字段要多
​ 2、尽可能多地给出包括一些富有意义的文字性描述：ID和名称都要有。
​ 3、区分数值型属性和事实，数值型宇段是作为事实还是维度属性，可以参考字段的一般用途。
​ 4、尽量沉淀出通用的维度属性：有些维度属性获取需要进行比较复杂的逻辑处理，有些需要通过多表关联得到，或者通过单表的不同宇段混合处理得到，或者通过对单表的某个字段进行解析得到。
    此时，需要将尽可能多的通用的维度属性进行沉淀。一方面，可以提高下游使用的方便性，减少复杂度；另一方面，可以避免下游使用解析时由于各自逻辑不同而导致口径不一致。


数据分析常用6种方法
1、多维分析:
    需要明确2个方向: 维度和指标
    所谓指标，指的是用来记录关键流程的，衡量目标的单位或方法，如DAU、留存率、转化率等。
    所谓维度，指的是观察指标的角度，如时间、来源渠道、地理位置、产品版本维度等。
    多维分析，就是在多个维度拆解，观察对比维度细分下的指标。实现将一个综合指标细分，从而发现更多问题

2、趋势分析:
    也叫对比分析，最常用的是基于时间的对比分析，主要分为同比，环比，定基比
    环比：与相邻的上一个周期做对比，如本周与上一周，本月与上个月
    同比：俩个个周期同一个时间点的比较，如2022年8月与2021年8月
    定基比：和指定的时间基点对比

3、转化分析（漏斗分析）:
    主要是分析产品流程或关键节点的转化效果，常借助漏斗图展现转化效果。
    漏斗图是一种外形类似漏斗的可视化图表，该方法可以直观追踪产品的整体流程，追踪业务的转化路径，追踪
    不同生命阶段下的用户群体表现
    常用的场景有：
    1）产品流程的关键路径转化追踪，比如电商的购买流程；
    2）业务价值路径的转化流程追踪，如常用的AARRR模型的价值转化追踪；
    3）虚拟流程类指标追踪，如按生命周期区分的不同生命周期阶段的用户流转形态追踪

4、公式拆解法:


5、公式拆解法:


6、结构化分析:





SQL优化：
1、SQL中过滤条件放在where/on后面的区别:
    a. inner join: 两者没有区别
    b. left join : left join的特殊机制就是on后面的条件只对右表起作用, 所以即使在on后面对左表进行过滤, 结果左表的数据还是会存在, 但
                    右表相应的数据会被过滤掉
    c. right join: 同left join
2、避免select * 查询, 改用具体字段, 节省资源, 减少网络开销
3、尽量避免使用or改用union all同表: or可能会使索引失去作用, 比如第一个字段使用了索引, 但or后的字段是非索引字段, 会进行全表扫描, 最后合并
                                    也就是整体分成全表扫描 + 索引扫面 + 合并。 如果使用union all只扫一次全表扫描即可
4、尽量使用数值型替代字符型: 引擎在处理数据时会逐个字符进行比较, 并且增加存储开销
5、使用varchar替代char: varchar按照实际长度进行存储, 存储空间小, 节省空间
                       char按照声明大小存储, 不足补空
                       varchar的查询效率相较于char来说会更高
6、避免在where子句中使用!= 或 <> 操作符: 使用该字符会使索引失效从而进行全表扫描
7、inner left right 三种join优先使用inner: 优化原则, 小表驱动大表
8、提高group by效率: 先过滤，后分组
9、清空表时优先使用truncate: truncate 比 delete使用的资源和事务日志资源少, delete按行删除, 每次都会留下一次事务日志, 但truncate是释放存储表数据
                            所用的数据页来进行删除
                            truncate删除表中所有行值, 新行标识所用的计数值重置为该列的种子
                            delete会保留标识计数值
                            有外键的表不能使用truncate只能用delete
10、操作delete或者update语句的时候, 可以加个limit或者循环批次删除:
    a. 降低误删除代价
    b. 避免长事务, 执行delete时, 会将所有相关行加锁, 所有执行相关操作都会被锁住, 若数据大, 则会导致数据库长时间无法操作
    c. 一次性删除太多数据, 会造成锁表, lock wait timeout错误
11、UNION操作: 尽量使用union all代替union操作(若无重复数据): union会对结果数据进行排序后删除重复项, 资源开销大, union all不会对数据去重, 只是做合并操作
12、避免在索引列上使用内置函数: 会导致索引失效
13、复合索引的最左特性: 索引是两列(id, name):
    a. 查询只出现最左索引(id), 索引生效
    b. 查询只出现非最左索引(name), 索引失效
    c. 复合索引全部使用, 索引生效
14、like模糊匹配优化: 可能会使索引失效
    a. 尽量使用右模糊查询, like '...%'
    b. 左模糊无法使用索引, 但可利用reverse
    c. like全模糊会使索引失效


hive优化














