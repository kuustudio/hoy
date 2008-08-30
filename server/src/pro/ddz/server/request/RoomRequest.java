package pro.ddz.server.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import pro.ddz.server.dao.DataAccessObject;
import pro.ddz.server.core.Message;
import pro.ddz.server.core.Request;
import pro.ddz.server.model.Desk;
import pro.ddz.server.model.Room;
import pro.ddz.server.model.Scene;
import pro.ddz.server.model.User;

public class RoomRequest extends Request {

	public RoomRequest(HttpServletRequest req, HashMap<String, Message> messageMap, DataAccessObject dao, ArrayList<User> onlineList, ArrayList<Scene> scenes){
		super(req, messageMap, dao, onlineList, scenes);
	}
	
	@Override
	public void execute() {
		//ʵ�ַ������Ϲ���
		//ROOM|0~9|CURRENTCOUNT|ROOM1COUNT|...@time
		String roomId = req.getHeader("Room-ID");
		this.userId = req.getHeader("UID")!=null?Integer.parseInt(req.getHeader("UID")):0;
		
		Room reqRoom = null;
		User currentUser = null;
		Scene currentScene = null;
		//��ȡ��ǰ�û�
		if(this.userId!=0){
			for(User u:this.onlineList){
				if(u.getId()==this.userId){
					currentUser = u;
					break;
				}
			}
		}
		
		//��ȡ��ǰ����
		if(currentUser!=null){
			for(Scene s:this.scenes){
				if(s.getId()==currentUser.getSceneId()){//��ȡ��ǰ�û�λ��
					currentScene = s;
					break;
				}
			}
		}
		
		//��ȡ���뷿��
		if(currentScene!=null&&roomId!=null){
			Iterator<Room> it = currentScene.getRooms().iterator();
			int i = 0;
			if(it.hasNext()&&i<Integer.parseInt(roomId)){
				reqRoom = (Room)it.next();
				i++;
			}
		}
		
		int count = 0;
		StringBuffer data = new StringBuffer();
		
		if(reqRoom!=null){

			//�����û�������Ϣ֮λ��
			currentUser.setRoomId(Integer.parseInt(roomId));
			//�����û��뿪����
			Desk desk = null;
			if(currentUser.getDeskId()!=0){
				Iterator<Desk> it = reqRoom.getDesks().iterator();
				int i = 0;
				if(it.hasNext()&&i<currentUser.getDeskId()){
					desk = (Desk)it.next();
					i++;
				}
				desk.leftUp(currentUser);
				currentUser.setDeskId(0);
			}
			
			//�û�����reqRoom
			reqRoom.jionRoom(currentUser);
			
			data.append("ROOM");
			data.append('|');
			data.append("1");
			data.append('|');
			data.append(reqRoom.getUsers().size());
			data.append('|');
			data.append('#');
			data.append('|');
			for(User u2:reqRoom.getUsers()){
//				data.append(u2.getId());
//				data.append('|');
				if(u2.getDeskId()!=0){
					count++;
					data.append(u2.isSexual());
					data.append('|');
					data.append(u2.getDeskId());
					data.append('|');
					data.append(u2.isStart());
					data.append('|');
				}
			}
			
			data.deleteCharAt(data.length()-1);
		}else{
			data.append("ROOM");
			data.append('|');
			data.append("2");
		}

		if(this.isAsync){
			getMessage().add(data.toString().replaceFirst("#", String.valueOf(count)));
		}else{
			this.result = data.toString().replaceFirst("#", String.valueOf(count));
		}
	}

	@Override
	public boolean isExecutable() {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

}