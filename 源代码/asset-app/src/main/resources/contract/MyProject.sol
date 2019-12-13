pragma solidity ^0.4.24;

import "./Table.sol";

contract MyProject{
    //添加用户：返回值，用户名
    event RegisterEvent(int256 ret, string account, int256 credit);
    //功能一 签发应收账款：返回值，账单号，借出钱的用户，欠钱的用户，欠款总额，还款时间(多少天后)
    event IssueEvent(int256 ret, string bill, string from_account, string to_account,  uint256 amount, uint256 ddl);
    //功能二 转让应收账款：返回值，分解的账单号，新的账单号，新的借出钱的用户，转让总额
    event TransferEvent(int256 ret, string from_bill, string new_bill, string new_account, uint256 amount);
    //功能三 向银行融资：返回值，融资用户，融资总额，还款时间
    //event FinancingEvent(int256 ret, string account,  uint256 amount, uint256 ddl);
    event FinancingEvent(int256 ret);
    //功能四 应收账款支付结算：返回值，账单号，距离借钱日过去的时间（天为单位）
    //event PayEvent(int256 ret, string bill, uint256 time);
    event PayEvent(int256 count);

    constructor() public {
        //构造函数中创建表
        createTable();
    }

    function createTable() private {
        TableFactory tf = TableFactory(0x1001); 

        // 资产管理表, key : bill, field : from_account  to_account  amount  ddl
        // |      账单号(主键)    |    借出钱的用户    |    欠钱的用户      |      总额         |    还款时间        |
        // |-------------------- |-------------------|-------------------|-------------------|-------------------|
        // |        bill         |    from_account   |    to_account     |    amount         |      ddl          |  
        // |---------------------|-------------------|-------------------|-------------------|-------------------|
        //
        // 创建表:账单
        tf.createTable("t_bill", "bill", "from_account,to_account,amount,ddl");


        // 资产管理表, key : account, field :  credit(0不受信用，1受信用)
        // |      用户名(主键)    |   是否受银行信用   |
        // |-------------------- |-------------------|
        // |       account       |      credit       |    
        // |---------------------|-------------------|
        //
        // 创建表：用户
        tf.createTable("t_user", "account", "credit");
    }

    function openTableUser() private returns(Table) {
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("t_user");
        return table;
    }    

    function openTableBill() private returns(Table) {
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("t_bill");
        return table;
    }    

    /*
    描述 : 根据用户名查询是否受银行信用
    参数 ： 
            account : 用户名

    返回值：
            参数一： 成功返回0, 账户不存在返回-1
            参数二： 第一个参数为0时有效，是否受信用
    */
    function selectAccount(string account) public constant returns(int256, uint256) {
        // 打开表
        Table table = openTableUser();
        // 查询
        Entries entries = table.select(account, table.newCondition());
        uint256 credit = 0;
        if (0 == uint256(entries.size())) {
            return (-1, credit);
        } else {
            Entry entry = entries.get(0);
            return (0, uint256(entry.getInt("credit")));
        }
    }

    /*
    描述 : 根据账单号查询借出钱的用户、欠钱的用户、总额、还款时间
    参数 ： 
            bill : 账单号

    返回值：
            参数一： 成功返回0, 账户不存在返回-1
            参数二： 第一个参数为0时有效，借出钱的用户
            参数三： 第一个参数为0时有效，欠钱的用户
            参数四： 第一个参数为0时有效，总额
            参数五： 第一个参数为0时有效，还款时间

    */    
    function selectBill(string bill) public constant returns(int256, string, string, uint256, uint256) {
        // 打开表
        Table table = openTableBill();
        // 查询
        Entries entries = table.select(bill, table.newCondition());
        //string  storage from_account = "0";
        //string storage to_account = "0";
        //uint256 amount = 0;
        //uint256 ddl = 0;
        if (0 == uint256(entries.size())) {
            //return (-1, from_account, to_account, amount, ddl);
            return (-1,"0","0",0,0);
        } else {
            Entry entry = entries.get(0);
            //from_account = entry.getString("from_account");
            //to_account = entry.getString("to_account");
            //amount = entry.getInt("amount");
            //ddl = entry.getInt("ddl");
            return (int256(0), entry.getString("from_account"), entry.getString("to_account"), uint256(entry.getInt("amount")), uint256(entry.getInt("ddl")));
        }
    }

    /*
    描述 : 用户注册
    参数 ： 
            account : 资产账户
            credit  : 是否受银行信用
    返回值：
            0  资产注册成功
            -1 资产账户已存在
            -2 其他错误
    */
    function registerUser(string account, int256 credit) public returns(int256){
        int256 ret_code = 0;
        int256 ret= 0;
        uint256 temp_credit = 0;
        // 查询账户是否存在
        (ret, temp_credit) = selectAccount(account);
        if(ret != 0) {
            Table table = openTableUser();
            
            Entry entry = table.newEntry();
            entry.set("account", account);
            entry.set("credit", credit);
            // 插入
            int count = table.insert(account, entry);
            if (count == 1) {
                // 成功
                ret_code = 0;
            } else {
                // 失败? 无权限或者其他错误
                ret_code = -2;
            }
        } else {
            // 账户已存在
            ret_code = -1;
        }

        emit RegisterEvent(ret_code, account, credit);
        return ret_code;
    }


    /*

    event IssueEvent(int256 ret, string bill, string from_account, string to_account,  uint256 amount, uint256 ddl);

    描述 : 功能一 签发应收账款：返回值，账单号，借出钱的用户，欠钱的用户，欠款总额，还款时间(多少天后)
    参数 ： 
            bill : 账单号
            from_account  : 借出钱的用户
            to_account : 欠钱的用户
            amount : 欠款总额
            ddl : 还款时间(多少天后)
    返回值：
            0  签发应收账款成功
            -2 其他错误
    */
    function issueBill(string bill, string from_account, string to_account, uint256 amount, uint256 ddl) public returns(int256){
        int256 ret_code = 0;
        int256 ret= 1;

        if(ret != 0) {
            Table table = openTableBill();
            
            Entry entry = table.newEntry();
            entry.set("bill", bill);
            entry.set("from_account", from_account);
            entry.set("to_account", to_account);
            entry.set("amount", int(amount));
            entry.set("ddl", int(ddl));

            // 插入
            int count = table.insert(bill, entry);
            if (count == 1) {
                // 成功
                ret_code = 0;
            } else {
                // 失败? 无权限或者其他错误
                ret_code = -2;
            }
        }

        emit IssueEvent(ret_code, bill, from_account, to_account, amount, ddl);
        return ret_code;
    }


    /*

    event TransferEvent(int256 ret, string from_bill, string new_bill, string new_account, uint256 amount);

    描述 :  功能二 转让应收账款：返回值，分解的账单号，新的账单号，新的借出钱的用户，转让总额
    
    参数 ： 
            from_bill : 分解的账单号
            new_bill ： 新的账单号
            new_account : 新借出钱的用户
            amount : 转让总额
    返回值：
            0  签发应收账款成功
            -2 其他错误
    */
    function transfer(string from_bill, string new_bill,string new_account, uint256 amount) public returns(int256) {
        uint256 from_amount = 0;

        Table table = openTableBill();

        Entries entries_temp0 = table.select(from_bill, table.newCondition());
        Entry entry_temp0 = entries_temp0.get(0);
        from_amount = uint256(entry_temp0.getInt("amount"));
//(int256(0), entry.getString("from_account"), entry.getString("to_account"), uint256(entry.getInt("amount")), uint256(entry.getInt("ddl")));

        Entry entry0 = table.newEntry();
        entry0.set("bill", from_bill);
        entry0.set("from_account", entry_temp0.getString("from_account"));
        entry0.set("to_account", entry_temp0.getString("to_account"));
        entry0.set("amount", int256(from_amount - amount));
        entry0.set("ddl", int256(entry_temp0.getInt("ddl")));

        // 更新被分解的账单
        int count = table.update(from_bill, entry0, table.newCondition());
        if(count != 1) {
            // 失败? 无权限或者其他错误?
            emit TransferEvent(-2, from_bill, new_bill, new_account, amount);
            return -2;
        }

        Entry entry1 = table.newEntry();
        entry1.set("bill", new_bill);
        entry1.set("from_account", new_account);
        entry1.set("to_account", entry_temp0.getString("to_account"));
        entry1.set("amount", int256(amount));
        entry1.set("ddl", int256(entry_temp0.getInt("ddl")));

        // 插入新账单
        int count2 = table.insert(new_bill, entry1);


        emit TransferEvent(0, from_bill, new_bill, new_account, amount);
        return 0;
    }



    /* 
    功能三 向银行融资：返回值，融资用户，融资总额，还款时间
    event FinancingEvent(int256 ret, string account,  uint256 amount, uint256 ddl); 

        参数 ： 
            from_bill : 分解的账单号
            new_bill ： 新的账单号
            amount : 转让总额
        返回值：
            0 银行融资成功
            -1 欠款方的账单不受信用，银行不融资
            -2 其他问题，融资失败
    */
    function fancing(string from_bill, string new_bill, uint256 amount) public returns(int256) {
        Table table0 = openTableBill();
        Table table1 = openTableUser();
        Entries entries_temp0 = table0.select(from_bill, table0.newCondition());
        Entry entry_temp0 = entries_temp0.get(0);
        
        Entries entries_temp1 = table1.select(entry_temp0.getString("to_account"), table1.newCondition());
        Entry entry_temp1 = entries_temp1.get(0);

        //1为受信用
        if(entry_temp1.getInt("credit")==int256(0)){
            emit FinancingEvent(-1);
            return -1;
        }

        if(entry_temp1.getInt("credit")==int256(1)){
            transfer(from_bill, new_bill,"bank",amount);
            emit FinancingEvent(0);
            return 0;
        }

        emit FinancingEvent(-2);
        return -2;
    }


    /*

    event PayEvent(int256 ret, string bill, uint256 time);

    描述 :  功能四 应收账款支付结算：返回值，账单号，距离借钱日过去的时间（天为单位）
    
    参数 ： 
            bill : 账单号
            time ： 距离借钱日过去的时间
    */

    function pay(string bill, uint256 time) public returns(int){
        Table table = openTableBill();

        Condition condition = table.newCondition();
        condition.EQ("bill", bill);
        condition.LE("ddl", int256(time));
        
        int count = table.remove(bill, condition);

        emit PayEvent(count);
        return count;
    }

}