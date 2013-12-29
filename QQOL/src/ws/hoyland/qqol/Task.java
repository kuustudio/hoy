package ws.hoyland.qqol;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Timer;
import java.util.zip.CRC32;

import javax.crypto.KeyAgreement;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.math.ec.ECPoint;

import sun.security.ec.NamedCurve;

import ws.hoyland.qqol.sync.FinishObject;
import ws.hoyland.qqol.sync.PauseCountObject;
import ws.hoyland.qqol.sync.PauseObject;
import ws.hoyland.qqol.sync.StartObject;
import ws.hoyland.util.Configuration;
import ws.hoyland.util.Converts;
import ws.hoyland.util.Crypter;
import ws.hoyland.util.DM;
import ws.hoyland.util.EngineMessage;
import ws.hoyland.util.YDM;

public class Task implements Runnable, Observer {

//	private boolean timeout = false;
	//private String line;
	private boolean run = false;
	private boolean fb = false; // break flag;
//	private boolean fc = false; // continue flag;
	private int idx = -1; // method index;
//	private Configuration configuration = Configuration.getInstance();
	
	// private boolean block = false;
	// private TaskObject obj = null;

//	private String sig = null;
	// private byte[] ib = null;
	// private byte[] image = null;


	private EngineMessage message = null;
	private int id = 0;
	private String account = null;
	private String password = null;

	private int codeID = -1;
	

	protected String mid = null;
	private String mail = null;
	private String mpwd = null;
	
	private boolean sf = false; //stop flag from engine
	private boolean rec = false;//是否准备重拨
	private int finish = 0;
		
	//private String ecp = null;//encrypted password
	String vcode = null;
	//private boolean dfnvc = false;
	
	private boolean pause = false;
//	byte status = 1; //0在线，//1离开（自动那个回复）//2忙碌 //3隐身 
//	String leftmsg = "您好，我现在有事不在，一会再和您联系。";
	private int status = 1;
	
	String ip = "183.60.19.100";//默认IP
	byte[] ips = new byte[]{
			(byte)183, (byte)60, (byte)19, (byte)100
	};

	byte[] serverPBK = new byte[]{
		0x04, (byte)0x92, (byte)0x8D, (byte)0x88, 0x50, 0x67, 0x30, (byte)0x88, 
		(byte)0xB3, 0x43, 0x26, 0x4E, 0x0C, 0x6B, (byte)0xAC, (byte)0xB8, 
		0x49, 0x6D, 0x69, 0x77, (byte)0x99, (byte)0xF3, 0x72, 0x11, 
		(byte)0xDE, (byte)0xB2, 0x5B, (byte)0xB7, 0x39, 0x06, (byte)0xCB, 0x08, 
		(byte)0x9F, (byte)0xEA, (byte)0x96, 0x39, (byte)0xB4, (byte)0xE0, 0x26, 0x04, 
		(byte)0x98, (byte)0xB5, 0x1A, (byte)0x99, 0x2D, 0x50, (byte)0x81, (byte)0x3D, (byte)0xA8	
	};
	
	short seq = 0x1123; //包序号
	
	//联接服务器,发送数据
	DatagramSocket ds = null;
	DatagramPacket dpIn = null;
	DatagramPacket dpOut = null;
	
	
	DatagramChannel dc = null;
	SocketAddress sa = null;
	
	byte[] buf = null;
	byte[] buffer = null;
	byte[] content = null;
	
	ByteArrayOutputStream baos = null; 		
	ByteArrayOutputStream bsofplain = null; 
	
	ByteArrayOutputStream bsofpng = null; 
	
	byte[] key0825 = null;
	byte[] ecdhkey = null;
	byte[] token = null;
	byte[] key0836 = null;
	byte[] key0836x = null;
	byte[] pwdkey = null;
	byte[] key00BA = null;
//	byte[] data00BA = null;
	
	byte[] key0828 = null;
	byte[] key0828recv = null;
	byte[] sessionkey = null;
	
	byte[] vctoken = null;
	byte[] resultByte = null;
	
	Crypter crypter = new Crypter();
	byte[] encrypt = null;
	byte[] decrypt = null;

	byte[][] crcs = new byte[][]{Util.genKey(16), Util.genKey(16), Util.genKey(16)};
	CRC32 crc = new CRC32();
	
	boolean redirect = false;
	boolean nvc = false;//是否需要验证码
	boolean dlvc = false;
	
	byte[] logintime = new byte[4];
	byte[] loginip = new byte[4];
	private Timer timer = null;
	//private static Integer timercount = 0;
	
	private static DateFormat format = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	public Task(String line) {
		String[] ls = line.split("----");
		this.id = Integer.parseInt(ls[0]);
		this.account = ls[1];
		this.password = ls[2];
		this.status = Integer.parseInt(Configuration.getInstance().getProperty("LOGIN_TYPE"));
		this.run = true;
		
		try{
			dc = DatagramChannel.open();
			dc.configureBlocking(false);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		info("开始运行");
		
		if(pause){//暂停
			info("暂停运行");
			synchronized(PauseCountObject.getInstance()){
				message = new EngineMessage();
				message.setType(EngineMessageType.IM_PAUSE_COUNT);
				Engine.getInstance().fire(message);
			}
			
			synchronized(PauseObject.getInstance()){
				try{
					PauseObject.getInstance().wait();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}

			
//			//阻塞等待重拨
//			if(rec){
//				info("等待重拨");
//				synchronized(ReconObject.getInstance()){
//					try{
//						ReconObject.getInstance().wait();
//					}catch(Exception e){
//						e.printStackTrace();
//					}
//				}
//				info("等待重拨结束， 继续执行");
//			}
	
			if(sf){//如果此时有停止信号，直接返回
				info("初始化(任务取消)");
				return;
			}

		synchronized(StartObject.getInstance()){	
			//通知有新线程开始执行
			message = new EngineMessage();
			message.setType(EngineMessageType.IM_START);
			Engine.getInstance().fire(message);
		}
		
		// System.err.println(line);
		while (run&&!sf) { //正常运行，以及未收到停止信号
			if (fb) {
				info("网络异常, 任务退出");
				break;
			}
//			if (fc) {
//				continue;
//			}

			// if(block){
			// synchronized (obj.getBlock()) {
			// try {
			// obj.getBlock().wait();
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }
			// block = false;
			// }

			process(idx);
		}
		
		//离线
		/**
		try{
			seq++;
			
			bsofplain.write(new byte[]{			
					0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
			});
			
			encrypt = crypter.encrypt(bsofplain.toByteArray(), sessionkey);
								
			baos = new ByteArrayOutputStream();
			baos.write(new byte[]{
					0x02, 0x34, 0x4B, 0x00, 0x62
			});
			baos.write(Converts.hexStringToByte(Integer.toHexString(seq).toUpperCase()));
			baos.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
			baos.write(new byte[]{
					//0x03, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x30
					//0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x3A
					//0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, 0x68, 0x00, 0x30, 0x00, 0x3A//(byte)0xA2?
					0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2 //0x65, (byte)0xCA								
			});
			baos.write(encrypt);
			baos.write(new byte[]{
					0x03
			});
			
			buf = baos.toByteArray();
			
			System.out.println("0062["+Converts.bytesToHexString(sessionkey)+"]");
			System.out.println(Converts.bytesToHexString(baos.toByteArray()));
			
			dpOut = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 8000);
			ds.send(dpOut);
			
			timer.cancel();	
		}catch(Exception e){
			e.printStackTrace();
		}
		 **/
		
		//通知Engine: 线程结束
		
		String[] dt = new String[6];

		dt[0] = String.valueOf(finish);
		dt[1] = this.account;
		dt[2] = this.password;
		
		if(finish==1){//成功
			//dt[0] = "1";
			dt[3] = "0";
			dt[4] = this.mail;
			dt[5] = this.mpwd;
		}		
		
		synchronized(FinishObject.getInstance()){
			message = new EngineMessage();
			message.setTid(this.id);
			message.setType(EngineMessageType.IM_FINISH);
			message.setData(dt);
			Engine.getInstance().fire(message);
		}
		
		Engine.getInstance().deleteObserver(this);
		
	}

	private void process(int index) {
		switch (index) {
		case -1:
			info("建立连接");
			try{
				if("2927238399".equals(account)){
					System.out.println("KK");
				}
				ds = new DatagramSocket(10023+this.id);
				idx++;
			}catch(Exception e){
				e.printStackTrace();
				fb = true;
			}
			break;
		case 0:
			info("正在登录");
			try{
				//ds = new DatagramSocket(5023);			
				//------------------------------------------------------------------------------			
				//0825
				do{
					redirect = false;
					seq++;
					key0825 = Util.genKey(0x10);
									
					//ecdhkey 随机生成的公钥，然后key0836x 即为共享秘密，由算法计算得出
					{
						Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
						
						ECParameterSpec ecSpec = NamedCurve.getECParameterSpec("secp192k1");
						
						KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDH", "BC");			
						keyGen.initialize(ecSpec, new SecureRandom()); //公私钥 工厂
						KeyPair pair = keyGen.generateKeyPair(); //生成公私钥
						BCECPublicKey cpk =(BCECPublicKey) pair.getPublic();
						ECPoint.Fp point = (ECPoint.Fp)cpk.getQ();

						ecdhkey = point.getEncoded(true);	//ecdhkey
						System.out.println(ecdhkey.length);
						
						java.security.spec.ECPoint sp = ECPointUtil.decodePoint(ecSpec.getCurve(), serverPBK);
						KeyFactory kf = KeyFactory.getInstance("ECDH", "BC");
						ECPublicKeySpec pubSpec = new ECPublicKeySpec(sp, ecSpec);
						ECPublicKey myECPublicKey = (ECPublicKey) kf.generatePublic(pubSpec);
						
						System.out.println(((BCECPublicKey)myECPublicKey).getQ().getEncoded().length);
						
						KeyAgreement agreement = KeyAgreement.getInstance("ECDH", "BC");
						agreement.init(pair.getPrivate());
						agreement.doPhase(myECPublicKey, true);
//						
						byte[] xx = agreement.generateSecret();
						key0836x = new byte[16];	//key0836x
						for(int i=0;i<key0836x.length;i++){
							key0836x[i] = xx[i];
						}
					}
					
					baos = new ByteArrayOutputStream();
					baos.write(new byte[]{
							0x02, 0x34, 0x4B, 0x08, 0x25
					});
					baos.write(Converts.hexStringToByte(Integer.toHexString(seq).toUpperCase()));
					baos.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
					baos.write(new byte[]{
							0x03, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x00, 0x00, 0x00
					});
					baos.write(key0825);
					//System.err.println(Converts.bytesToHexString(rndkey));
					//System.out.println(Converts.bytesToHexString(baos.toByteArray()));
					//以下需要加密
					bsofplain = new ByteArrayOutputStream();
					bsofplain.write(new byte[]{
							0x00, 0x18, 0x00, 0x16, 0x00, 0x01,
							0x00, 0x00, 0x04, 0x36, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x14, (byte)0x9B
					});
					bsofplain.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
					bsofplain.write(new byte[]{
							0x00, 0x00, 0x00, 0x00, 0x03, 0x09, 0x00, 0x08, 0x00, 0x01
					});
					bsofplain.write(ips);
//					bsofplain.write(new byte[]{
//							0x00, 0x00, 0x00, 0x00 
//					});
					bsofplain.write(new byte[]{
							//0x00, 0x04, 0x00, 0x36, 0x00, 0x12, 0x00, 0x02, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
							0x00, 0x02, 0x00, 0x36, 0x00, 0x12, 0x00, 0x02, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
							0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x14, 0x00, 0x1D, 0x01, 0x02 
					});
					bsofplain.write(new byte[]{
							0x00, 0x19 
					});
					bsofplain.write(ecdhkey);
					//System.out.println(Converts.bytesToHexString(bsofplain.toByteArray()));
					encrypt = crypter.encrypt(bsofplain.toByteArray(), key0825);
					//加密完成
		//			byte[] ts = crypter.decrypt(encrypt, rndkey);
		//			System.out.println(Converts.bytesToHexString(ts));
					baos.write(encrypt);
					baos.write(new byte[]{
							0x03
					});
					buf = baos.toByteArray();
					System.out.println("0825["+Converts.bytesToHexString(key0825)+"]");
					System.out.println(Converts.bytesToHexString(baos.toByteArray()));

					//OUT:	
//					dpOut = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 8000);
//					ds.send(dpOut);
					
					sa = new InetSocketAddress(ip, 8000);
					dc.connect(sa);
					
					Selector selector = Selector.open();
					dc.register(selector, SelectionKey.OP_READ );
					dc.write(ByteBuffer.wrap(buf));
					
					sa = new InetSocketAddress("183.60.19.101", 8000);
					try{
						dc = DatagramChannel.open();
						dc.configureBlocking(false);
					}catch(Exception e){
						e.printStackTrace();
					}
					dc.connect(sa);
					dc.register(selector, SelectionKey.OP_READ );
					dc.write(ByteBuffer.wrap(buf));
					dc.r
					
					ByteBuffer byteBuffer = ByteBuffer.allocate ( 1024 ) ;
					boolean t = true;
			         while ( t ) {
			             try {
			                 int eventsCount = selector.select () ;
			                 if ( eventsCount > 0 ) {
			                     Set selectedKeys = selector.selectedKeys () ;
			                     Iterator iterator = selectedKeys.iterator () ;
			                     while ( iterator.hasNext ()) {
			                         SelectionKey sk = ( SelectionKey ) iterator.next () ;
			                         iterator.remove () ;
			                         if ( sk.isReadable ()) {
			                             DatagramChannel datagramChannel = ( DatagramChannel ) sk
			                                     .channel () ;
			                             datagramChannel.read ( byteBuffer ) ;
			                             byteBuffer.flip () ;
			                            
			                             //TODO 将报文转化为RUDP消息并调用RUDP协议处理器来处理
			                            
			                             System.out.println ( Converts.bytesToHexString(byteBuffer.array())) ;
			                             byteBuffer.clear () ;
			                             
			                         }
			                     }
			                 }
			             } catch ( Exception e ) {
			                 e.printStackTrace () ;
			             }
			         } 
			         
					//IN:
					buffer = new byte[1024];
					dpIn = new DatagramPacket(buffer, buffer.length);
									
		//			byte[] ts = crypter.decrypt(encrypt, rndkey);
		//			System.out.println(Converts.bytesToHexString(ts));
					
					//while(true){
					ds.receive(dpIn);
					//还需判断buffer 121等位置，看是否是转向，也可能是104字节，不需转向
					buffer = Util.pack(buffer);
					//System.out.println(buffer.length);
					if(buffer.length==135){
						redirect = true;
						info("重定向中");
						System.out.println("重定向:"+buffer[128]+","+buffer[129]+","+buffer[130]);
						
						content = Util.slice(buffer, 14, 120);
						decrypt = crypter.decrypt(content, key0825);
						
						ips = Util.slice(decrypt, 95, 4);
						ip = (ips[0]&0xFF)+"."+(ips[1]&0xFF)+"."+(ips[2]&0xFF)+"."+(ips[3]&0xFF);
					}
				}while(redirect);

				System.out.println("bfl:"+buffer.length);
				//199 错误, 783错误
				//System.out.println(Converts.bytesToHexString(loginip));
				content = Util.slice(buffer, 14, 104);
				
//				String data = Converts.bytesToHexString(content);
//				System.out.println(data);
				
				//解密
				decrypt = crypter.decrypt(content, key0825);
				//System.out.println(Converts.bytesToHexString(decrypt));
				token = Util.slice(decrypt, 5, 0x38);
				logintime = Util.slice(decrypt, 67, 4);
				loginip = Util.slice(decrypt, 71, 4);
				
				idx++;
			}catch(Exception e){
				System.out.println(this.account+":V");
				e.printStackTrace();
				fb = true;
			}
			break;
		case 1:
			info("正在验证身份信息");
			try{
				//0836
				//------------------------------------------------------------------------------
				do{
					seq++;
					//nvc = false;
					
					//第一段
					bsofplain = new ByteArrayOutputStream();
					bsofplain.write(Util.genKey(4));
					bsofplain.write(new byte[]{
							0x00, 0x01 
					});
					bsofplain.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
					bsofplain.write(new byte[]{
							//0x00, 0x00, 0x04, 0x36, 0x06, 0x00, 0x05, 0x11, 0x00, 0x00, 0x01, 0x00,
							0x00, 0x00, 0x04, 0x36, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x14, (byte)0x9B,
							0x00, 0x00, 0x01 // 不记住密码
					});
					bsofplain.write(Converts.MD5Encode(password));
					bsofplain.write(logintime);
					bsofplain.write(new byte[]{
							0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
					});
					bsofplain.write(loginip);
					bsofplain.write(new byte[]{
							0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0B, 0x00,
							0x00, 0x10,
							(byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0
							//(byte)0xE3, 0x04, 0x2B, (byte)0xB2, (byte)0xF7, (byte)0xEA, 0x62, 0x40, (byte)0x99, 0x62, (byte)0x81, 0x11, 0x44, 0x52, 0x17, 0x22
							// F0 F0 F0 F0 F0 F0 F0 F0 F0 F0 F0 F0 F0 F0 F0 F0 ?
					});
					key0836 = Util.genKey(0x10);
					bsofplain.write(key0836);
					
					System.out.println("XX:"+bsofplain.toByteArray().length);
					//pwdkey
					ByteArrayOutputStream bsofpwd = new ByteArrayOutputStream();
					bsofpwd.write(Converts.MD5Encode(password));
					bsofpwd.write(new byte[]{
							0x00, 0x00, 0x00, 0x00
					});
					bsofpwd.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
					pwdkey = Converts.MD5Encode(bsofpwd.toByteArray());
					//System.out.println(Converts.bytesToHexString(pwdkey));
					
					byte[] first = crypter.encrypt(bsofplain.toByteArray(), pwdkey);
					//System.out.println(Converts.bytesToHexString(first));
					System.err.println(first.length);
					//第二段
					bsofplain = new ByteArrayOutputStream();
					bsofplain.write(new byte[]{
							0x00, 0x15, 0x00, 0x30, 0x00, 0x00 
					});
					bsofplain.write(new byte[]{
							0x01
					});
					crc.reset();
					crc.update(crcs[1]);
					bsofplain.write(Converts.hexStringToByte(Long.toHexString(crc.getValue()).toUpperCase()));
					bsofplain.write(new byte[]{
							0x00, 0x10
					});
					bsofplain.write(crcs[1]);
					
					bsofplain.write(new byte[]{
							0x02
					});
					crc.reset();
					crc.update(crcs[2]);
					bsofplain.write(Converts.hexStringToByte(Long.toHexString(crc.getValue()).toUpperCase()));
					bsofplain.write(new byte[]{
							0x00, 0x10
					});
					bsofplain.write(crcs[2]);
					
					System.out.println("YY:"+bsofplain.toByteArray().length);
					byte[] second = crypter.encrypt(bsofplain.toByteArray(), key0836);
					System.err.println(second.length);
					//System.out.println(Converts.bytesToHexString(second));
					//System.out.println(Converts.bytesToHexString(crcs[1]));
					//System.out.println(crc.getValue());
					//byte[] kk = Converts.hexStringToByte(Long.toHexString(crc.getValue()).toUpperCase());
					//System.out.println(Converts.bytesToHexString(kk));
					
					//对一、二段和 0x00, 0x18 进行加密
					bsofplain = new ByteArrayOutputStream();
					bsofplain.write(new byte[]{
							0x01, 0x12,
							0x00, 0x38
					});
					bsofplain.write(token);
					bsofplain.write(new byte[]{
							0x03, 0x0F,
							0x00, 0x11 //是计算机名加上计算机名长度的长度
					});
					bsofplain.write(new byte[]{
							0x00, 0x0F // 长度
					});
					
		//			String kk = genHostName(0x0F);
		//			byte[] xx = kk.getBytes();
					//48 46 5A 4D 43 5A 35 44 4A 50 30 53 4A 46 35 
					bsofplain.write(Util.genHostName(0x0F).getBytes());// 计算机名
					
					bsofplain.write(new byte[]{
							0x00, 0x05, 0x00, 0x06, 0x00, 0x02
					});
					bsofplain.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
					bsofplain.write(new byte[]{
							0x00, 0x06
					});
					bsofplain.write(new byte[]{
							0x00, (byte)first.length
							//first length
					});
					//System.out.println("V:"+bsofplain.toByteArray().length);
					bsofplain.write(first);		//first
					
					bsofplain.write(new byte[]{
							0x00, 0x1A
					});
					bsofplain.write(new byte[]{
							0x00, (byte)second.length
							//second length
					});
					bsofplain.write(second);		//second
					bsofplain.write(new byte[]{
							0x00, 0x18, 0x00, 0x16, 0x00, 0x01,
							//0x00, 0x00, 0x04, 0x36, 0x06, 0x00, 0x05, 0x11, 0x00, 0x00, 0x01, 0x00 
							0x00, 0x00, 0x04, 0x36, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x14, (byte)0x9B //提示重新输入密码？
							//00 00 04 36 00 00 00 01 00 00 14 9B?
					});
					bsofplain.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
					bsofplain.write(new byte[]{
							0x00, 0x00,  // 记住密码00 01
							0x00, 0x00, 0x01, 0x03, 0x00, 0x14, 0x00, 0x01, // 固定
							0x00, 0x10 //长度
					});
					//bsofplain.write(Util.genKey(0x10));//机器固定验证key
					bsofplain.write(new byte[]{
							0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 // 机器固定验证key
					});
					
					//如果是验证码已经识别后的情况，需要加入一段数据
					if(nvc){
						bsofplain.write(new byte[]{
								0x01, 0x10, 0x00, 0x3C, 0x00, 0x01
						});
						bsofplain.write(new byte[]{
								0x00, 0x38
						});
						bsofplain.write(vctoken);
						/////TODO
						/**
						【注意这个位置 如果是有验证码已经识别了验证码，需要在这个地方加上识别验证码返回的token
						01 10 00 3C 00 01
						00 00 // 识别验证码返回token长度
						00 00 00 00 00 ..... // 识别验证码返回token
						】
						**/
					}
					//SP5不需要?
		//			bsofplain.write(new byte[]{
		//					0x00, 0x32, 0x00, 0x37, 0x3E, 0x00, 0x37, 0x01, 0x03, 0x04, 0x02, 0x00, 0x00, 0x04, 0x00	//固定
		//			});
		//			bsofplain.write(new byte[]{
		//					0x22, (byte)0x88, // unknow
		//					(byte)0x00, 0x00, 0x00, 0x00, 0x03, 0x5A, 0x44, 0x22, (byte)0x00, 
		//					(byte)0xFC, (byte)0xCF, 0x68, (byte)0xF5, 0x53, 0x26, 0x3B, (byte)0xB8, (byte)0xE6, 0x61, 0x76, (byte)0xF1, (byte)0x9D, 0x1C, 0x7A, (byte)0xD8,
		//					(byte)0xA9, (byte)0xAE, 0x04, (byte)0xE0, (byte)0xD6, (byte)0xBF, (byte)0xBA, (byte)0x8F, 0x55, (byte)0xF0, 0x36, 0x7A, (byte)0xF7, (byte)0xD6, (byte)0xED, (byte)0x92 // unknow是一个验证，未知算法，可以用随机的，0826包不返回0018的数据，0018的数据也可以直接随机
		//			});
		//			bsofplain.write(new byte[]{
		//					0x68, 0x01, 0x14, 0x00, 0x1D, 0x01, 0x02	//固定
		//			});
		//			bsofplain.write(new byte[]{
		//					0x00, 0x19  // 长度
		//			});
		//			bsofplain.write(ecdhkey);
					bsofplain.write(new byte[]{
							0x01, 0x02, 0x00, 0x62, 0x00, 0x01 //固定
					});
					bsofplain.write(new byte[]{
							0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 // 应该是用于出现验证码时计算key，全放0，后面可以直接用0826send key解密
					});
					bsofplain.write(new byte[]{
							0x00, 0x38  // 长度
					});
					bsofplain.write(Util.genKey(0x38));
					bsofplain.write(new byte[]{
							0x00, 0x14  // 长度
					});
					
					crcs[0] = new byte[]{
							0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00// 应该是用于出现验证码时计算key，全放0，后面可以直接用0826send key解密 
					};
					bsofplain.write(crcs[0]);
					//crc.reset();
					//crc.update(crcs[0]);
					//bsofplain.write(reverse(Converts.hexStringToByte(Long.toHexString(crc.getValue()).toUpperCase()))); // here should reverse
					bsofplain.write(new byte[]{
							0x00, 0x00, 0x00, 0x00
					});
		
					System.out.println("ZZ:"+bsofplain.toByteArray().length);
					//用key0836x 进行加密
					encrypt = crypter.encrypt(bsofplain.toByteArray(), key0836x);
					System.err.println(">>"+encrypt.length);
					//加密完成
					
					System.out.println("XXXXXX");
					System.out.println(Converts.bytesToHexString(key0836));
					System.out.println(Converts.bytesToHexString(key0836x));
					System.out.println(Converts.bytesToHexString(pwdkey));
					
					//整段			
					baos = new ByteArrayOutputStream();
					baos.write(new byte[]{
							0x02, 0x34, 0x4B, 0x08, 0x36
					});
					baos.write(Converts.hexStringToByte(Integer.toHexString(seq).toUpperCase()));
					baos.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
					baos.write(new byte[]{
							0x03, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x00, 0x00, 0x00
					});
					baos.write(new byte[]{
							0x00, 0x01, 0x01, 0x02 
					});
					baos.write(new byte[]{
							0x00, 0x19
					});
					baos.write(ecdhkey);
					baos.write(new byte[]{
							0x00, 0x00,
							0x00, 0x10	//长度? //TODO
					});
					baos.write(Util.genKey(0x10));
		//			baos.write(new byte[]{
		//					(byte)0x94, (byte)0xF5, 0x56, (byte)0xD7, (byte)0xDC, (byte)0xE1, (byte)0x84, (byte)0xB8, 0x2F, 0x44, (byte)0x8C, 0x4D, (byte)0xB1, 0x3D, 0x65, (byte)0xB8,
		//					(byte)0x97, (byte)0xC1, 0x39
		//			});
					
					baos.write(encrypt);
					baos.write(new byte[]{
							0x03
					});
					buf = baos.toByteArray();
					
					System.out.println("0836["+Converts.bytesToHexString(key0836x)+"]");
					System.out.println(Converts.bytesToHexString(baos.toByteArray()));			
					
					dpOut = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 8000);
					ds.send(dpOut);
					
					//IN:
					buffer = new byte[1024];
					dpIn = new DatagramPacket(buffer, buffer.length);
									
		//			byte[] ts = crypter.decrypt(encrypt, rndkey);
		//			System.out.println(Converts.bytesToHexString(ts));
					
					//while(true){
					ds.receive(dpIn);
					//还需判断buffer 121等位置，看是否是转向，也可能是104字节，不需转向
					buffer = Util.pack(buffer);
					System.out.println("TK:"+buffer.length);
					//System.out.println("OUT:"+Converts.bytesToHexString(buffer));
					nvc = false; //清空nvc状态
					
					//175密码错误，871需要验证码
					if(buffer.length==871){
						//00BA 处理验证码
						info("下载验证码");
						System.out.println("需要验证码处理");
						nvc = true; //需要再次请求0836
						bsofpng = new ByteArrayOutputStream();
						
						content = Util.slice(buffer, 14, buffer.length-15);
						decrypt = crypter.decrypt(content, key0836x);
						
						//截取png数据，以及相关key, data
						//token = Util.slice(decrypt, 22, 0x38); //new token?
						byte[] ilbs = Util.slice(decrypt, 22+0x38, 2);
						int imglen = ilbs[0]*0x100 + (ilbs[1] & 0xFF);
						
						//byte[] png = Util.slice(decrypt, 24+0x38, imglen);
						bsofpng.write(Util.slice(decrypt, 24+0x38, imglen));
						//System.out.println(Converts.bytesToHexString(png));
						dlvc = (Util.slice(decrypt, 25+0x38+imglen, 1)[0]==1);
						System.out.println("dlvc:"+dlvc);
						System.out.println("imglen:"+imglen);
						
						//no use?
//						byte[] key0836r = Util.slice(decrypt, 25+0x38+imglen, 0x10);
						
						System.out.println(Converts.bytesToHexString(decrypt));
						byte[] tokenfor00ba = Util.slice(decrypt, 28+0x38+imglen, 0x28);
						//System.out.println(Converts.bytesToHexString(tokenfor00ba));
						byte[] keyfor00ba = Util.slice(decrypt, 32+0x38+0x28+imglen, 0x10);
						//System.out.println("keyfor00ba:"+Converts.bytesToHexString(keyfor00ba));
						

//						//data00BA0 = token;
//						data00BA1 = Util.genKey(0x28);
//						data00BA2 = Util.genKey(0x10);
//						
//						byte pidx = 0x00;
						//需要继续下载验证码数据
						boolean rv = false; //request verify
						
						do{
							//00BA: 继续下载验证码
							//-----------------------------------------------------------
							if(!dlvc){
								rv = true; //此次发送验证请求
								info("提交验证码");
							}else{
								info("继续下载验证码");
							}
							
							seq++;
							dlvc = false;
							
							key00BA = Util.genKey(0x10);
//							data00BA = Util.genKey(0x15);
//							data00BA1 = Util.genKey(0x28);
//							data00BA2 = Util.genKey(0x10);
							
							bsofplain = new ByteArrayOutputStream();
							bsofplain.write(new byte[]{
									0x00, 0x02, 0x00, 0x00, 0x08, 0x04, 0x01, (byte)0xE0, 
									0x00, 0x00, 0x04, 0x36, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x14, (byte)0x9B
							});
		
							if(!rv){
								bsofplain.write(new byte[]{
										0x00
								});
							}else{
								bsofplain.write(new byte[]{
										0x01
								});
							}
							
							bsofplain.write(new byte[]{
									0x00, 0x38
							});
							bsofplain.write(token);
							
							bsofplain.write(new byte[]{
									0x01, 0x02
							});
							
							bsofplain.write(new byte[]{
									0x00, 0x19
							});
							bsofplain.write(ecdhkey);
							
							if(!rv){ //继续下载验证码
								bsofplain.write(new byte[]{
										0x13, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00, 0x01
										
								});
								

								bsofplain.write(new byte[]{
										0x00, 0x28
								});
								bsofplain.write(tokenfor00ba);
								
							}else{ //发送验证请求
								bsofplain.write(new byte[]{
										0x14, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00
								});
								bsofplain.write(new byte[]{
										0x04 // 验证码长度
								});
//								byte[] ccode = new byte[]{
//									0x79, 0x6F, 0x6F, 0x62 
//								};
								bsofplain.write(resultByte);							

								bsofplain.write(new byte[]{
										0x00, 0x38
								});
								//bsofplain.write(Util.genKey(0x38));
								bsofplain.write(tokenfor00ba);
							}
							
							
							bsofplain.write(new byte[]{
									0x00, 0x10
							});
							bsofplain.write(keyfor00ba);				
							
							encrypt = crypter.encrypt(bsofplain.toByteArray(), key00BA);
							
							//total
							baos = new ByteArrayOutputStream();
							baos.write(new byte[]{
									0x02, 0x34, 0x4B, 0x00, (byte)0xBA
							});
							baos.write(Converts.hexStringToByte(Integer.toHexString(seq).toUpperCase()));
							baos.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
							baos.write(new byte[]{
									0x03, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x00, 0x00, 0x00
							});
							baos.write(key00BA);
							baos.write(encrypt);
							baos.write(new byte[]{
									0x03
							});
							
							buf = baos.toByteArray();
							
							System.out.println("00BA["+Converts.bytesToHexString(key00BA)+"]");
							System.out.println(Converts.bytesToHexString(baos.toByteArray()));			
							
							dpOut = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 8000);
							ds.send(dpOut);
							
							//IN:
							buffer = new byte[1024];
							dpIn = new DatagramPacket(buffer, buffer.length);
											
							ds.receive(dpIn);
							
							buffer = Util.pack(buffer);
							System.out.println(buffer.length);
							System.out.println(Converts.bytesToHexString(buffer));
							
							
							if(!rv){
								content = Util.slice(buffer, 14, buffer.length-15);
								decrypt = crypter.decrypt(content, key0836x);//??????!!!!!! 第二次才是key00ba
								System.out.println(Converts.bytesToHexString(decrypt));
								byte[] imglenbs = Util.slice(decrypt, 10+0x38, 2);
								//System.out.println(Converts.bytesToHexString(imglenbs));
								imglen = imglenbs[0]*0x100 + (imglenbs[1] & 0xFF);
								bsofpng.write(Util.slice(decrypt, 12+0x38, imglen));
								//System.out.println(Converts.bytesToHexString(png));
								dlvc = (Util.slice(decrypt, 13+0x38+imglen, 1)[0]==1);
								System.out.println("dlvc:"+dlvc);
								System.out.println("imglen:"+imglen);
								
								//需要更新 tokenfor00ba keyfor00ba ?
								//tokenfor00ba = Util.slice(decrypt, 16+0x38+imglen, 0x28);
								//keyfor00ba = Util.slice(decrypt, 18+0x38+0x28+imglen, 0x10);
								tokenfor00ba = Util.slice(decrypt, 10, 0x38);
								
								byte[] by = bsofpng.toByteArray();
								//resultByte = new byte[30]; // 为识别结果申请内存空间
//								StringBuffer rsb = new StringBuffer(30);
								String rsb = "0000";
								resultByte = rsb.getBytes();

								info("识别验证码");
								if(Engine.getInstance().getCptType()==0){
									codeID = YDM.INSTANCE.YDM_DecodeByBytes(by, by.length, 1004, resultByte);//result byte
	//									result = "xxxx";
	//									for(int i=0;i<resultByte.length;i++){
	//										System.out.println(resultByte[i]);
	//									}
	//									System.out.println("TTT:"+codeID);
								}else{
									codeID = DM.INSTANCE.uu_recognizeByCodeTypeAndBytesA(by,
											by.length, 1, resultByte); // 调用识别函数,resultBtye为识别结果
								}
								String result = new String(resultByte, "UTF-8").trim();
								
								System.out.println("result:"+resultByte.length+":"+result);
									
							
								/**
								File file = new File("c:/t.png");
								FileOutputStream fileOutputStream  = new FileOutputStream(file);
								//写到文件中
								fileOutputStream.write(bsofpng.toByteArray());
								//reader.close();
								bsofpng.close();
								fileOutputStream.close();
								**/
							}else{
								System.out.println("验证结果:"+buffer.length);
								//其他情况，报告错误							
								//95正常?
								if(buffer.length==95){
									content = Util.slice(buffer, 14, buffer.length-15);
									decrypt = crypter.decrypt(content, key00BA);
									//获取vctoken
									vctoken = Util.slice(decrypt, 10, 0x38); 
									System.out.println("KK1:");
									System.out.println(Converts.bytesToHexString(decrypt));
									System.out.println(Converts.bytesToHexString(vctoken));
									
									//idx++; //not need 要验证后才确定是否 idx++
								}else{
									info("验证码错误， 报告异常");
									try {
										//
										int reportErrorResult = -1;
										if(Engine.getInstance().getCptType()==0){
											reportErrorResult = YDM.INSTANCE.YDM_Report(codeID, false);
										}else{
											reportErrorResult = DM.INSTANCE.uu_reportError(codeID);
										}
										System.err.println(reportErrorResult);										
									} catch (Exception e) {
										e.printStackTrace();
									}
									//timer.cancel();
									nvc = false;
									idx = 0;//重新验证身份 //idx = 1?
									//return;
								}
							}
//							System.out.println(decrypt.length);
//							System.out.println("XXX");
//							System.out.println(Converts.bytesToHexString(decrypt));
							
						}while(!rv);//提交验证码后，则退出

						//return;
					}/**
					else if(buffer.length==175){
						//System.out.println("用户名或密码错误, 退出任务");
						byte[] ts = Util.slice(buffer, 14, buffer.length-15);
						ts = crypter.decrypt(ts, key0836x);
						System.out.println(Converts.bytesToHexString(ts));
						System.out.println(new String(Util.slice(ts, 15, ts.length-15), "utf-8"));
						info(new String(Util.slice(ts, 15, ts.length-15), "utf-8"));
						run = false;
						return;
						//idx = 1;?
					}else if(buffer.length==95){					
						byte[] ts = Util.slice(buffer, 14, buffer.length-15);
						ts = crypter.decrypt(ts, key0836x);
						System.out.println(Converts.bytesToHexString(ts));
						System.out.println(new String(Util.slice(ts, 15, ts.length-15), "utf-8"));
						info(new String(Util.slice(ts, 15, ts.length-15), "utf-8"));
						run = false;
						return;
						//idx = 1;
//						System.out.println("LENGTH: 95");
//						content = Util.slice(buffer, 14, buffer.length-15);
//						decrypt = crypter.decrypt(content, key0836x);
//						System.out.println(Converts.bytesToHexString(decrypt));
					}**/
					else if(buffer.length==175||buffer.length==95||buffer.length==247||buffer.length==239){
						byte[] ts = Util.slice(buffer, 14, buffer.length-15);
						ts = crypter.decrypt(ts, key0836x);
						System.out.println(Converts.bytesToHexString(ts));
						System.out.println(new String(Util.slice(ts, 15, ts.length-15), "utf-8"));
						info(new String(Util.slice(ts, 15, ts.length-15), "utf-8"));
						run = false;
						return;
						//idx = 1;
					}else if(buffer.length==255){
						info("需要验证密保");
						run = false;
						return;
					}else{
						idx++;
					}

//					System.out.println(decrypt.length);
//					System.out.println(Converts.bytesToHexString(decrypt));
				}while(nvc);
			}catch(Exception e){
				e.printStackTrace();
				fb = true;
			}
			break;
		case 2:
			info("获取会话密钥");
			try{
				//正常情况下的 key0836解密
				content = Util.slice(buffer, 14, buffer.length-15);
				decrypt = crypter.decrypt(content, key0836);
				
				System.out.println(Converts.bytesToHexString(decrypt));
				//需解释出某些值供 0828使用
				key0828 = Util.slice(decrypt, 7, 0x10);
				byte[] tokenfor0828 = Util.slice(decrypt, 9+0x10, 0x38);
				String rbof0836 = Converts.bytesToHexString(decrypt);
				
				//查找昵称
				byte[] rdecrypt = Util.reverse(decrypt);
				String rbofrdec = Converts.bytesToHexString(rdecrypt);
				int nickidx = -1;
				do{
					//nickidx = rbofrdec.indexOf("0100")/2;
					rbofrdec = rbofrdec.substring(rbofrdec.indexOf("0100") + 4);//往前查找
				}while(!"0801".equals(rbofrdec.substring(4, 8)));
				//退出循环，当为找到
				nickidx = rbofrdec.length()/2 + 8;
				int nicklen = decrypt[nickidx];
				byte[] nick = new byte[nicklen];
				for(int i=0;i<nicklen;i++){
					nick[i] = decrypt[nickidx+1+i];
				}
				System.out.println("Nick:"+new String(nick, "utf-8"));
				setNick(new String(nick, "utf-8"));
				
				key0828recv = Util.slice(decrypt, rbof0836.indexOf("0000003C0002")/2+6, 0x10);
				//00 00 00 3C 00 02
				//Converts.bytesToHexString(decrypt).indexOf(")
				//------------------------------------------------------------------------------
				//0828
				seq++;
				
				System.out.println("ip="+ip);
				bsofplain = new ByteArrayOutputStream();
				bsofplain.write(new byte[]{
						0x00, 0x07, 0x00, (byte)0x88, 0x00, 0x04
				});
				System.err.println(Converts.bytesToHexString(Util.slice(decrypt, rbof0836.indexOf("00880004")/2+4, 4)));
				System.err.println(Converts.bytesToHexString(Util.slice(decrypt, rbof0836.indexOf("00880004")/2+8, 4)));
				System.err.println(Converts.bytesToHexString(ips));
				bsofplain.write(Util.slice(decrypt, rbof0836.indexOf("00880004")/2+4, 4));
				bsofplain.write(Util.slice(decrypt, rbof0836.indexOf("00880004")/2+8, 4));
//				bsofplain.write(Util.genKey(4));
//				bsofplain.write(Util.genKey(4));
				bsofplain.write(new byte[]{
						0x00, 0x00, 0x00, 0x00
				});
				bsofplain.write(new byte[]{
						0x00, 0x78
				});
				bsofplain.write(Util.slice(decrypt, rbof0836.indexOf("000000000078")/2+6, 0x78));
				bsofplain.write(new byte[]{
						0x00, 0x0C, 0x00, 0x16, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 //TODO 06->00?
				});
				bsofplain.write(ips);
				bsofplain.write(new byte[]{
						0x1F, 0x40
				});
				bsofplain.write(new byte[]{
						0x00, 0x00, 0x00, 0x00, 0x00, 0x15, 0x00, 0x30, 0x00, 0x01
				});
				
				/**
				bsofplain.write(new byte[]{
						0x01
				});
				crc.reset();
				crc.update(pwdkey);
				bsofplain.write(Converts.hexStringToByte(Long.toHexString(crc.getValue()).toUpperCase()));
				bsofplain.write(new byte[]{
						0x00, 0x10
				});
				bsofplain.write(pwdkey);
				
				bsofplain.write(new byte[]{
						0x02
				});
				crc.reset();
				crc.update(key0836);
				bsofplain.write(Converts.hexStringToByte(Long.toHexString(crc.getValue()).toUpperCase()));
				bsofplain.write(new byte[]{
						0x00, 0x10
				});
				bsofplain.write(key0836);
				**/
				bsofplain.write(new byte[]{
						0x01
				});
				crc.reset();
				crc.update(crcs[1]);
				bsofplain.write(Converts.hexStringToByte(Long.toHexString(crc.getValue()).toUpperCase()));
				bsofplain.write(new byte[]{
						0x00, 0x10
				});
				bsofplain.write(crcs[1]);
				
				bsofplain.write(new byte[]{
						0x02
				});
				crc.reset();
				crc.update(crcs[2]);
				bsofplain.write(Converts.hexStringToByte(Long.toHexString(crc.getValue()).toUpperCase()));
				bsofplain.write(new byte[]{
						0x00, 0x10
				});
				bsofplain.write(crcs[2]);
				
				bsofplain.write(new byte[]{
						0x00, 0x36, 0x00, 0x12, 0x00, 0x02, 0x00, 0x01, 0x00, 0x00, 0x00, // 固定
						0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 固定 如果有验证码的包第一位为13
						0x00, 0x18, 0x00, 0x16, 0x00, 0x01, // 固定
						0x00, 0x00, 0x04, 0x36, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x14, (byte)0x9B
				});
				bsofplain.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
				bsofplain.write(new byte[]{
						0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0x00, 0x22, 0x00, 0x01
				});
				bsofplain.write(Util.genKey(0x20));// 32位机器码
				bsofplain.write(new byte[]{
						0x01, 0x05, 0x00, 0x46, 0x00, 0x01, 0x01, 0x03
						//0x01, 0x05, 0x00, 0x30, 0x00, 0x01, 0x01, 0x02
				});
				bsofplain.write(new byte[]{
						0x00, 0x14, 0x01, 0x01,
						0x00, 0x10
				});
				bsofplain.write(Util.genKey(0x10));
				
				bsofplain.write(new byte[]{
						0x00, 0x14, 0x01, 0x01,
						0x00, 0x10
				});
				bsofplain.write(Util.genKey(0x10));
				
				bsofplain.write(new byte[]{
						0x00, 0x14, 0x01, 0x02,
						0x00, 0x10
				});
				bsofplain.write(Util.genKey(0x10));
				bsofplain.write(new byte[]{
						//0x01, 0x0B, 0x00, 0x38, 0x00, 0x01
						0x01, 0x0B, 0x00, 0x20, 0x00, 0x01 //去掉0x18的情况
				});
				bsofplain.write(new byte[]{
						(byte)0xCF, (byte)0x99, (byte)0xD8, (byte)0xE3, 0x79, (byte)0x95, 0x2A, (byte)0xF1, (byte)0xCA, 0x6D, (byte)0xEC, 0x42, 0x0A, (byte)0xC7, (byte)0xC5, 0x10// QQ file MD5 //TODO
				});			
				//bsofplain.write(Util.genKey(0x10)); 
				bsofplain.write(new byte[]{
						(byte)0xF8, //flag
						0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02 // 固定
				});
				bsofplain.write(new byte[]{
						0x00, 0x00 //去掉0x18的情况 ，原来是 0x00, 0x18
				});
//				bsofplain.write(new byte[]{//Util.genKey(0x18);
//						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
//						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
//				}); // 0836 receive token3, 没有收到?
				bsofplain.write(new byte[]{
						0x00, 0x00, 0x00, 0x2D, 0x00, 0x06, 0x00, 0x01, // 固定
						(byte)0xC0, (byte)0xA8, 0x01, 0x66 // 本地IP
				});
				System.out.println("V:"+Converts.bytesToHexString(bsofplain.toByteArray()));	
				encrypt = crypter.encrypt(bsofplain.toByteArray(), key0828);
				System.out.println(Converts.bytesToHexString(encrypt));		
				
				baos = new ByteArrayOutputStream();
				baos.write(new byte[]{
						0x02, 0x34, 0x4B, 0x08, 0x28
				});
				baos.write(Converts.hexStringToByte(Integer.toHexString(seq).toUpperCase()));
				baos.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
				baos.write(new byte[]{
						//0x03, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x30
						//0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x3A
						0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, 0x68, 0x00, 0x30, 0x00, 0x3A//(byte)0xA2?
				});
				baos.write(new byte[]{
						0x00, 0x38
				});
				baos.write(tokenfor0828);
				baos.write(encrypt);
				baos.write(new byte[]{
						0x03
				});
				
				buf = baos.toByteArray();
				
				System.out.println("0828["+Converts.bytesToHexString(key0828)+"]");
				System.out.println(Converts.bytesToHexString(baos.toByteArray()));			
				
				dpOut = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 8000);
				ds.send(dpOut);
				
				//IN:
				buffer = new byte[1024];
				dpIn = new DatagramPacket(buffer, buffer.length);
								
				ds.receive(dpIn);
				
				buffer = Util.pack(buffer);
				System.out.println(buffer.length);
				System.out.println(Converts.bytesToHexString(buffer));
				if(buffer.length==127){
					//System.out.println("您的网络环境可能发生了变化，为了您的帐号安全，请重新登录。");
					//System.out.println("退出任务");
					
					byte[] ts = Util.slice(buffer, 14, buffer.length-15);
					ts = crypter.decrypt(ts, key0828);
					System.out.println(Converts.bytesToHexString(ts));
					System.out.println(new String(Util.slice(ts, 15, ts.length-15), "utf-8"));
					info(new String(Util.slice(ts, 15, ts.length-15), "utf-8"));
					idx = 0;//重新来
				}else{
					System.out.println("OK");
					info("获取成功");
					content = Util.slice(buffer, 14, buffer.length-15);
					System.out.println(Converts.bytesToHexString(key0828recv));
					decrypt = crypter.decrypt(content, key0828recv);
					System.out.println(decrypt.length);
					System.out.println(Converts.bytesToHexString(decrypt));
					
					sessionkey = Util.slice(decrypt, 63, 0x10);		
					idx++;		
				}
			}catch(Exception e){
				e.printStackTrace();
				fb = true;
			}
			break;
		case 3:
			info("设置上线状态");
			try{
				//00EC 上线包
				//-------------------------------------------------------------------
				seq++;
				bsofplain = new ByteArrayOutputStream();
				
				switch(status){
				case 0://在线
					bsofplain.write(new byte[]{
							0x01, 0x00, 0x0A
					});
					break;
				case 1://离开，自动回复
					bsofplain.write(new byte[]{
							0x01, 0x00, 0x1E
					});
					break;
				case 2://忙碌
					bsofplain.write(new byte[]{
							0x01, 0x00, 0x32 
					});
					break;
				case 3://隐身
					bsofplain.write(new byte[]{
							0x01, 0x00, 0x28
					});
					break;
				default:
					bsofplain.write(new byte[]{
							0x01, 0x00, 0x0A //在线;
					});
					break;
				}
				/**
				bsofplain.write(new byte[]{
						0x01, 0x00, 0x0A //在线;
						//0x00, 0x3C //Q我吧
						//0x00, 0x32 //离开
						//	//隐身？
				});
				**/
				encrypt = crypter.encrypt(bsofplain.toByteArray(), sessionkey);
				
				baos = new ByteArrayOutputStream();
				baos.write(new byte[]{
						0x02, 0x34, 0x4B, 0x00, (byte)0xEC
				});
				baos.write(Converts.hexStringToByte(Integer.toHexString(seq).toUpperCase()));
				baos.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
				baos.write(new byte[]{
						//0x03, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x30
						//0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x3A
						//0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, 0x68, 0x00, 0x30, 0x00, 0x3A//(byte)0xA2?
						0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x65, (byte)0xCA 
				});
				baos.write(encrypt);
				baos.write(new byte[]{
						0x03
				});
				
				buf = baos.toByteArray();
				
				System.out.println("00EC["+Converts.bytesToHexString(sessionkey)+"]");
				System.out.println(Converts.bytesToHexString(baos.toByteArray()));
				
				dpOut = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 8000);
				ds.send(dpOut);
				
				//IN:
				buffer = new byte[1024];
				dpIn = new DatagramPacket(buffer, buffer.length);
								
				ds.receive(dpIn);
				
				buffer = Util.pack(buffer);
//				System.out.println(buffer.length);
//				System.out.println(Converts.bytesToHexString(buffer));
				idx++;
			}catch(Exception e){
				e.printStackTrace();
				fb = true;
			}
			break;
		case 4:
			info("更新资料");
			try{
				//00EC 上线包
				//-------------------------------------------------------------------
				boolean goingon005c = true;
				do{
					seq++;
					bsofplain = new ByteArrayOutputStream();
					bsofplain.write(new byte[]{
							(byte)0x88
					});
					bsofplain.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
					bsofplain.write(new byte[]{
							0x00
					});
					
					encrypt = crypter.encrypt(bsofplain.toByteArray(), sessionkey);
					
					baos = new ByteArrayOutputStream();
					baos.write(new byte[]{
							0x02, 0x34, 0x4B, 0x00, (byte)0x5C
					});
					baos.write(Converts.hexStringToByte(Integer.toHexString(seq).toUpperCase()));
					baos.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
					baos.write(new byte[]{
							//0x03, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x30
							//0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x3A
							//0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, 0x68, 0x00, 0x30, 0x00, 0x3A//(byte)0xA2?
							0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x65, (byte)0xCA 
					});
					baos.write(encrypt);
					baos.write(new byte[]{
							0x03
					});
					
					buf = baos.toByteArray();
					
					System.out.println("005C["+Converts.bytesToHexString(sessionkey)+"]");
					System.out.println(Converts.bytesToHexString(baos.toByteArray()));
					
					dpOut = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 8000);
					ds.send(dpOut);
					
					//IN:
					buffer = new byte[1024];
					dpIn = new DatagramPacket(buffer, buffer.length);
									
					ds.receive(dpIn);
					
					buffer = Util.pack(buffer);
	//				System.out.println(buffer.length);
					System.out.println(Converts.bytesToHexString(buffer));
					
					content = Util.slice(buffer, 14, buffer.length-15);
					decrypt = crypter.decrypt(content, sessionkey);
					System.out.println("005C>>>"+Converts.bytesToHexString(decrypt));
					if(decrypt[0]==(byte)0x88){
						goingon005c = false;
					}else{
						info("继续更新资料");
					}
				}while(goingon005c);
				int level = Util.slice(decrypt, 10, 1)[0];
				int days = Util.slice(decrypt, 16, 1)[0];
				setProfile(level, days);
				idx++;
			}catch(Exception e){
				e.printStackTrace();
				fb = true;
			}
			break;
		case 5:
			info("登录完成");
			try{
				SocketLand.getInstance().add(new SSClient(this.id, account, ds, ip, sessionkey));
				idx++;
				run = false;
			}catch(Exception e){
				e.printStackTrace();
				fb = true;
			}
			break;
			/**
		case 6:
			finish = 1;
			idx++;
			break;
			**/
		default:
			break;
		}
	}

//	private void tf() { //task finish
//		message = new EngineMessage();
//		message.setTid(this.id);
//		message.setType(EngineMessageType.IM_TF);
//
//
//		Engine.getInstance().fire(message);
//		
//	}

	private void info(String info){
		message = new EngineMessage();
		message.setTid(this.id);
		message.setType(EngineMessageType.IM_INFO);
		message.setData(info);

		//DateFormat format = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String tm = format.format(new Date());
		
		System.err.println("["+this.account+"]"+info+"("+tm+")");
		Engine.getInstance().fire(message);
	}
	
//	private void infoact(){
//		//DateFormat format = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//		String tm = format.format(new Date());
//		
//		message = new EngineMessage();
//		message.setTid(this.id);
//		message.setType(EngineMessageType.IM_INFOACT);
//		message.setData(tm);
//
//		
//		System.err.println("["+this.account+"]ACT("+tm+")");
//		Engine.getInstance().fire(message);
//	}
	
	private void setNick(String nick){
		
		message = new EngineMessage();
		message.setTid(this.id);
		message.setType(EngineMessageType.IM_NICK);
		message.setData(nick);

		
		//System.err.println("["+this.account+"]ACT("+tm+")");
		Engine.getInstance().fire(message);
	}
	
	private void setProfile(int level, int days){
		message = new EngineMessage();
		message.setTid(this.id);
		message.setType(EngineMessageType.IM_PROFILE);
		message.setData(level+":"+days);
		
		Engine.getInstance().fire(message);
	}
		
	private void logout(){
		try{
			//离线的处理
			info("正在离线");
			seq++;
			
			bsofplain = new ByteArrayOutputStream();
			bsofplain.write(new byte[]{			
					0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
			});
			
			encrypt = crypter.encrypt(bsofplain.toByteArray(), sessionkey);
								
			baos = new ByteArrayOutputStream();
			baos.write(new byte[]{
					0x02, 0x34, 0x4B, 0x00, 0x62
			});
			baos.write(Converts.hexStringToByte(Integer.toHexString(seq).toUpperCase()));
			baos.write(Converts.hexStringToByte(Long.toHexString(Long.valueOf(account)).toUpperCase()));
			baos.write(new byte[]{
					//0x03, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x30
					//0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2, 0x00, 0x30, 0x00, 0x3A
					//0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, 0x68, 0x00, 0x30, 0x00, 0x3A//(byte)0xA2?
					0x02, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x66, (byte)0xA2 //0x65, (byte)0xCA								
			});
			baos.write(encrypt);
			baos.write(new byte[]{
					0x03
			});
			
			buf = baos.toByteArray();
			
			if(sessionkey!=null){
				System.out.println("0062["+Converts.bytesToHexString(sessionkey)+"]");
				System.out.println(Converts.bytesToHexString(baos.toByteArray()));
				
				dpOut = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 8000);
				ds.send(dpOut);
			}
			
			if(ds!=null){
				ds.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	@Override
	public void update(Observable obj, Object arg) {
		final EngineMessage msg = (EngineMessage) arg;

		if (msg.getTid() == this.id || msg.getTid()==-1) { //-1, all tasks message
			int type = msg.getType();

			switch (type) {
			case EngineMessageType.OM_REQUIRE_MAIL:
				if(msg.getData()!=null){
					String[] ms = (String[]) msg.getData();
					System.err.println(ms[0] + "/" + ms[1] + "/" + ms[2]);
					this.mid = ms[0];
					this.mail = ms[1];
					this.mpwd = ms[2];
				}else {
					info("没有可用邮箱, 退出任务");
					this.run = false;
					
					//通知引擎
					EngineMessage message = new EngineMessage();
					//message.setTid(this.id);
					message.setType(EngineMessageType.IM_NO_EMAILS);
					// message.setData(obj);
					Engine.getInstance().fire(message);
				}
				break;
			case EngineMessageType.OM_RECONN: //系统准备重拨
				//System.err.println("TASK RECEIVED RECONN:"+rec);
				rec = !rec;
				break;
			case EngineMessageType.OM_PAUSE:
				//System.err.println("XXXV");
				pause  = !pause;
				break;
			case EngineMessageType.OM_STOP:
				run = false;
				if(timer!=null){
					timer.cancel();
				}				
				logout();
				break;
			default:
				break;
			}
		}
	}	
}
