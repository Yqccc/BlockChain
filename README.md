# BlockChain
实验报告在report文件夹，录屏在录屏文件夹，链端、前端、后端代码在源代码文件夹。<br>
<br>
运行项目的方式为，在linux环境下打开终端，进入 BlockChain/源代码/fisco/nodes/127.0.0.1 文件夹，启动链端节点：./start_all.sh <br>
然后进入 BlockChain/源代码/asset-app 文件夹，调用build指令：./gradlew build <br>
build成功后，进入 BlockChain/源代码/asset-app/dist 文件夹，即可通过命令行前端调用各种指令。<br>
输入 bash asset_run.sh 即可调出Usage帮助文档，可查看指令的格式。(注：要先调用deploy指令部署智能合约)<br> 
<br>
附：用法为<br>
    bash asset_run.sh deploy<br>
    bash asset_run.sh getCredit account<br>
    bash asset_run.sh getBill bill<br>
    bash asset_run.sh registerAccount account credit<br>
    bash asset_run.sh createBill bill from_account to_account amount ddl<br>
    bash asset_run.sh transferBill from_bill new_bill new_account amount<br>
    bash asset_run.sh financing from_bill new_bill amount<br>
    bash asset_run.sh payBill bill time<br>
<br>
其中调用financing向银行融资功能时，要先调用 bash asset_run.sh registerAccount bank 1 创建一个bank账户(已经在链上创好)