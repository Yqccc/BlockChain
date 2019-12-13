package org.fisco.bcos.asset.client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fisco.bcos.asset.contract.MyProject;
import org.fisco.bcos.asset.contract.MyProject.RegisterEventEventResponse;
import org.fisco.bcos.asset.contract.MyProject.IssueEventEventResponse;
import org.fisco.bcos.asset.contract.MyProject.TransferEventEventResponse;
import org.fisco.bcos.asset.contract.MyProject.FinancingEventEventResponse;
import org.fisco.bcos.asset.contract.MyProject.PayEventEventResponse;
import org.fisco.bcos.channel.client.Service;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.channel.ChannelEthereumService;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.tuples.generated.Tuple2;
import org.fisco.bcos.web3j.tuples.generated.Tuple5;
import org.fisco.bcos.web3j.tx.gas.StaticGasProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class AssetClient {

	static Logger logger = LoggerFactory.getLogger(AssetClient.class);

	private Web3j web3j;

	private Credentials credentials;

	public Web3j getWeb3j() {
		return web3j;
	}

	public void setWeb3j(Web3j web3j) {
		this.web3j = web3j;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public void recordAssetAddr(String address) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		prop.setProperty("address", address);
		final Resource contractResource = new ClassPathResource("contract.properties");
		FileOutputStream fileOutputStream = new FileOutputStream(contractResource.getFile());
		prop.store(fileOutputStream, "contract address");
	}

	public String loadAssetAddr() throws Exception {
		// load Asset contact address from contract.properties
		Properties prop = new Properties();
		final Resource contractResource = new ClassPathResource("contract.properties");
		prop.load(contractResource.getInputStream());

		String contractAddress = prop.getProperty("address");
		if (contractAddress == null || contractAddress.trim().equals("")) {
			throw new Exception(" load Asset contract address failed, please deploy it first. ");
		}
		logger.info(" load Asset address from contract.properties, address is {}", contractAddress);
		return contractAddress;
	}

	public void initialize() throws Exception {

		// init the Service
		@SuppressWarnings("resource")
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		Service service = context.getBean(Service.class);
		service.run();

		ChannelEthereumService channelEthereumService = new ChannelEthereumService();
		channelEthereumService.setChannelService(service);
		Web3j web3j = Web3j.build(channelEthereumService, 1);

		// init Credentials
		Credentials credentials = Credentials.create(Keys.createEcKeyPair());

		setCredentials(credentials);
		setWeb3j(web3j);

		logger.debug(" web3j is " + web3j + " ,credentials is " + credentials);
	}

	private static BigInteger gasPrice = new BigInteger("30000000");
	private static BigInteger gasLimit = new BigInteger("30000000");

	public void deployAssetAndRecordAddr() {

		try {
			MyProject asset = MyProject.deploy(web3j, credentials, new StaticGasProvider(gasPrice, gasLimit)).send();
			System.out.println(" deploy Asset success, contract address is " + asset.getContractAddress());

			recordAssetAddr(asset.getContractAddress());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println(" deploy Asset contract failed, error message is  " + e.getMessage());
		}
	}

	public void getCredit(String account) {
		try {
			String contractAddress = loadAssetAddr();

			MyProject asset = MyProject.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			Tuple2<BigInteger, BigInteger> result = asset.selectAccount(account).send();
			if (result.getValue1().compareTo(new BigInteger("0")) == 0) {
				System.out.printf(" account %s has credit %s\n", account, result.getValue2());
			} else {
				System.out.printf(" %s account is not exist\n", account);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(" getCredit exception, error message is {}", e.getMessage());

			System.out.printf(" getCredit failed, error message is %s\n", e.getMessage());
		}
	}

	public void getBill(String bill) {
		try {
			String contractAddress = loadAssetAddr();

			MyProject asset = MyProject.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			Tuple5<BigInteger, String, String, BigInteger, BigInteger> result = asset.selectBill(bill).send();
			if (result.getValue1().compareTo(new BigInteger("0")) == 0) {
				System.out.printf(" bill: %s\n from_account: %s\n to_account: %s\n amount: %s\n ddl: %s\n", bill, result.getValue2(), result.getValue3(), result.getValue4(), result.getValue5());
			} else {
				System.out.printf(" %s bill is not exist \n", bill);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(" getBill exception, error message is {}", e.getMessage());

			System.out.printf(" getBill failed, error message is %s\n", e.getMessage());
		}
	}

	public void registerAccount(String account, BigInteger credit) {
		try {
			String contractAddress = loadAssetAddr();

			MyProject asset = MyProject.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.registerUser(account, credit).send();
			List<RegisterEventEventResponse> response = asset.getRegisterEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" registerAccount success!\n account: %s\n credit: %s \n", account, credit);
				} else {
					System.out.printf(" registerAccount failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" registerAccount exception, error message is {}", e.getMessage());
			System.out.printf(" registerAccount, error message is %s\n", e.getMessage());
		}
	}

	public void createBill(String bill, String from_account, String to_account, BigInteger amount, BigInteger ddl) {
		try {
			String contractAddress = loadAssetAddr();

			MyProject asset = MyProject.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.issueBill(bill, from_account, to_account, amount, ddl).send();
			List<IssueEventEventResponse> response = asset.getIssueEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" createBill success!\n bill: %s\n from_account: %s\n to_account: %s\n amount: %s\n ddl: %s\n", bill, from_account, to_account, amount, ddl);
				} else {
					System.out.printf(" createBill failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" createBill exception, error message is {}", e.getMessage());
			System.out.printf(" createBill, error message is %s\n", e.getMessage());
		}
	}

	public void transferBill(String from_bill, String new_bill, String new_account, BigInteger amount) {
		try {
			String contractAddress = loadAssetAddr();
			MyProject asset = MyProject.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.transfer(from_bill, new_bill, new_account, amount).send();
			List<TransferEventEventResponse> response = asset.getTransferEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" transferBill success!\n from_bill: %s\n new_bill: %s\n new_account: %s\n amount: %s\n", from_bill, new_bill, new_account, amount);
				} else {
					System.out.printf(" transferBill failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" transferBill exception, error message is {}", e.getMessage());
			System.out.printf(" transferBill failed, error message is %s\n", e.getMessage());
		}
	}

	public void financing(String from_bill, String new_bill, BigInteger amount) {
		try {
			String contractAddress = loadAssetAddr();
			MyProject asset = MyProject.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.fancing(from_bill, new_bill, amount).send();
			List<FinancingEventEventResponse> response = asset.getFinancingEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" financing success!\n from_bill: %s\n new_bill: %s\n new_account: bank\n amount: %s\n", from_bill, new_bill, amount);
				} else if(response.get(0).ret.compareTo(new BigInteger("-1")) == 0){
					System.out.printf(" financing failed, because from_account is not credit\n");
				} else {
					System.out.printf(" financing failed, because amount is bigger than from_bill");
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" financing exception, error message is {}", e.getMessage());
			System.out.printf(" financing failed, error message is %s\n", e.getMessage());
		}
	}

	public void payBill(String bill, BigInteger time) {
		try {
			String contractAddress = loadAssetAddr();
			MyProject asset = MyProject.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.pay(bill, time).send();
			List<PayEventEventResponse> response = asset.getPayEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).count.compareTo(new BigInteger("1")) == 0) {
					System.out.printf(" payBill success!\n");
				} else {
					System.out.printf(" payBill failed, the time is not up to the deadline.\n");
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" payBill exception, error message is {}", e.getMessage());
			System.out.printf(" payBill failed, error message is %s\n", e.getMessage());
		}
	}

	public static void Usage() {
		System.out.println(" Usage:");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient deploy");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient getCredit account");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient getBill bill");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient registerAccount account credit");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient createBill bill from_account to_account amount ddl");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient transferBill from_bill new_bill new_account amount");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient financing from_bill new_bill amount");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient payBill bill time");
		System.exit(0);
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 1) {
			Usage();
		}

		AssetClient client = new AssetClient();
		client.initialize();

		switch (args[0]) {
		case "deploy":
			client.deployAssetAndRecordAddr();
			break;
		case "getCredit":
			if (args.length < 2) {
				Usage();
			}
			client.getCredit(args[1]);
			break;
		case "getBill":
			if (args.length < 2) {
				Usage();
			}
			client.getBill(args[1]);
			break;
		case "registerAccount":
			if (args.length < 3) {
				Usage();
			}
			client.registerAccount(args[1], new BigInteger(args[2]));
			break;
		case "createBill":
			if (args.length < 6) {
				Usage();
			}
			client.createBill(args[1], args[2], args[3], new BigInteger(args[4]), new BigInteger(args[5]));
			break;
		case "transferBill":
			if (args.length < 5) {
				Usage();
			}
			client.transferBill(args[1], args[2], args[3], new BigInteger(args[4]));
			break;
		case "financing":
			if (args.length < 4) {
				Usage();
			}
			client.financing(args[1], args[2], new BigInteger(args[3]));
			break;	
		case "payBill":
			if (args.length < 3) {
				Usage();
			}
			client.payBill(args[1], new BigInteger(args[2]));
			break;
		default: {
			Usage();
		}
		}

		System.exit(0);
	}
}

