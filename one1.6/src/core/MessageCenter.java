package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import constructions.Data;
import constructions.Request;

public class MessageCenter {
    public static int deleteCount = 0; //ɾ������Ϣ��
    public static int deliveredCount = 0; //Ͷ�ݳɹ�����Ϣ��
    public static List<Double> duringTimes = new ArrayList<>();
    public static List<Integer> hops = new ArrayList<>();
    public static double rsuThreshold = 50;
    public static double carThreshold = 40;
    public static double GodThreshold = 60;
    public static double oldTime = 0;
    public static int eventNumber = 0;
    public static int trueEvent = 0;
    public static int falseEvent = 0;
    public static int definateTrueTrueEvent = 0;
    public static int definateTrueFalseEvent = 0;
    public static int definateFalseTrueEvent = 0;
    public static int definateFalseFalseEvent = 0;
    public static List<Integer> definateEvent = new ArrayList<>();
    public static List<DTNHost> definatedEvent = new ArrayList<>();
    public static double cost = 0;//�¼�����������
    public static Map<DTNHost,Double> transformTime = new HashMap<>();//�¼�����ʱ�䣬�Ӳ������ж�����ʱ��
    
   
    /*
     * ������������������metric
     */
    //��Ϣ������
    public static double messageTransmission=0;
    //�����������͵�����
    public static double pushUpDatas=0;
    //�������ϴ��͵��ƶ˵�����
    public static double pushUpToCloud=0;
   
    //��ȡ���ݵ�����
    public static double pullDatas=0;
    //�������ݵ�����
    public static double pushDatas=0;
    //�ɹ��ظ���ѯ�ı���
    public static double repliedQueryRate=0;
    //��ѯ����
    public static double querys=0;
    //�ɹ���ѯ����
    public static double repliedQuerys=0;
    //���ݹ켣�������ɵĲ�ѯ
    public static double trackQuerys=0;
    //�켣���ݲ�ѯ�гɹ��ظ�������
    public static double trackRepliedQuerys=0;
    //�������rsu��cloud��ɲ�ѯ������
    public static double repliedByNearRSUOrCloud=0;
    public static double splitedFilters=0;
    
    //�������ݵĴ���
    public static double clearDatas=0;
    //����ɹ��ظ���ѯ�����ĺ���
    public static void calReplyRate(){
    	if(MessageCenter.querys==0) MessageCenter.repliedQueryRate=0;
    	else MessageCenter.repliedQueryRate=MessageCenter.repliedQuerys/MessageCenter.querys;
    }
    //filter cube ���µļ���ʱ��
    public static double filterCubeUpdateTime=0;
   //filter cube�ĸ��´���
    public static double filterCubeUpdates=0;
    
    //��ʱ��ӵ�
    //��ѯ�������ʱ��
    public static double exitTime=300;
    //��Ч����
    public static double dis=600;
    //�������ݲ�ѯ��ʱ���೤��
    public static double okTime=300;
    //Ϊ���Ϊ��ͨ�����ݵĲ�ѯ����ѡ��Ŀ��RSU
    public static DTNHost selectRSUForTraffic(Request q){
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	DTNHost des=null;
    	double near=20000;
    	for(DTNHost s:d){
    		//��RSU�ڵ���в���,�ж�query�е�location�����RSU
    		if(s.getType()==1){
    			double charge=s.getLocation().distance(q.getLocation());
    			if(charge<near){
    				near=charge;
    				des=s;
    			}
    		}
    	}
    	return des;
    }
    //Ϊ���Ϊ���ֵ����ݵĲ�ѯ����ѡ��Ŀ�Ľڵ�
    public static DTNHost selectRSUForAmuse(DTNHost h){
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	DTNHost res=null;
    	double ndis=2000;
    	for(DTNHost s:d){
    		//��RSU�ڵ���в�����ѡ��һ�����������RSU
    		if(s.getType()==1){
    			
    			double dis=h.getLocation().distance(s.getLocation());
    			if(dis<ndis){
    				res=s;  
    				ndis=dis;
    			}
    				
    		}
    	}
    	return res;
    }
    /*
     * Ϊһ���ƶ��ĳ�����ȡ�����·�߽ڵ㣬������ѯ
     */
    public static DTNHost getNearestEdge(DTNHost h){
    	List<DTNHost> ds=SimScenario.getInstance().getEdges();
    	DTNHost res=null;
    	double minDis=2000000000;
    	for(DTNHost d:ds){
    		double dis=h.getLocation().distance(d.getLocation());
    		if(minDis>dis){
    			res=d;
    			minDis=dis;
    		}
    	}
    	return res;
    }
    //ΪRSUѡȡĿ�ĳ����Ա����䷢�������Ի�ȡ����
    public static List<DTNHost> selectNodeForRSU(DTNHost h){
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	List<DTNHost> res=new ArrayList<>();
    	int num=0;
    	for(DTNHost s:d){
    		//�Բ�ΪRSU�Ľڵ���в���
    		if(s.getType()!=1) {
    			if(s.getLocation().distance(h.getLocation())<400){
    				res.add(s);
    				num=num+1;
    			}
    		}
    		//����RSU�������������ȡ����ʱѡȡ���������ڵ�
    		if(num>2) return res;
    	}
    	return res;
    }
    //��ȡRSU�ڵ��ƽ��ʣ��ռ�ռ����
    public static String showRestSpaceRateOfRSU(){
    	String res="";
    	List<DTNHost> d=SimScenario.getInstance().getEdges();
    	int num=0;//��¼RSU����
    	double allRate=0;//��¼�ܵĿռ�ռ����
    	double minRate=1;
    	double maxRate=0;
    	for(DTNHost s:d){
    		if(s.getType()==1){
    			num++;
    			allRate+=s.getRestSpaceRate();
    			if(s.getRestSpaceRate()>maxRate) maxRate=s.getRestSpaceRate();
    			else if(s.getRestSpaceRate()<minRate) minRate=s.getRestSpaceRate();
    		}
    	}
    	allRate=allRate/num;
    	res=res+"����RSU��ƽ��ʣ��ռ����Ϊ��"+allRate+"�����ʣ��ռ����Ϊ��"+maxRate
    			+"����Сʣ��ռ����Ϊ��"+minRate;
    	return res;
    }
    //��ȡRSU�ڵ��ƽ���ظ���ѯ�ɹ���
    public static String showReplyRateOfRSU(){
    	List<DTNHost> d=SimScenario.getInstance().getEdges();
    	int num=0;
    	double allRate=0;
    	for(DTNHost s:d){
    		if(s.getType()==1&&s.getNumOfQuery()>0){
    			num++;
    			allRate+=s.getReplyRate();
    		}
    	}
    	allRate=allRate/num;
    	return "RSU�ڵ��ƽ���ظ���ѯ�ɹ���Ϊ��"+allRate;
    }
    //��ȡRSU�ڵ��ƽ��������
    public static String showReplyByLocalRateOfRSU(){
    	List<DTNHost> d=SimScenario.getInstance().getEdges();
    	int num=0;//��¼RSU����
    	double allRate=0;//��¼�ܵ�������
    	for(DTNHost s:d){
    		if(s.getType()==1&&s.getNumOfQuery()>0){
    			num++;
    			allRate+=s.getReplyByLocalRate();
    		}
    	}
    	allRate=allRate/num;
    	return "RSU�ڵ�ƽ���ı������������ѯ��Ϊ��"+allRate+"����rsu������"+num;
    }
    //��ȡRSU�ظ���ѯ��ƽ������ʱ��
    public static String showAverTimeOfRSU(){
    	String res="";
    	List<DTNHost> d=SimScenario.getInstance().getEdges();
    	int num=0;
    	double allTime=0;
    	for(DTNHost e:d){
    		if(e.getAverReplyTime()>0){
    			num++;
    			allTime=allTime+e.getAverReplyTime();
    		}
    	}
    	allTime=allTime/num;
    	res=res+"RSU��Ӧ��ѯ��ƽ������ʱ��Ϊ��"+allTime;
    	return res;
    }
  //��ȡ������ƽ����ѯ�ɹ���
    public static String showReplyRateOfCar(){
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	double nums=0;
    	double allNum=0;//��¼�ܵĲ�ѯ��
    	double allRate=0;//��¼�ܵĲ�ѯ�ɹ���
    	for(DTNHost s:d){
    		if(s.getType()==0&&s.getNumOfQuery()>0){
    			allNum+=1;
    			nums=nums+s.getNumOfQuery();
    			allRate+=s.getReplyRate();
    		}
    	}
    	
    	return "������ƽ����ѯ�ɹ���Ϊ��"+(double)allRate/allNum
    			+"�����ѯ�ĳ����еĲ�ѯ����������"+allNum
    			+"����ѯ������"+nums;
    }
    
    //��ȡ������ѯ��ƽ���ȴ�ʱ��
    public static String showAverTimeOfCar(){
    	String res="";
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	int num=0;//��¼��������
    	double allTime=0;//��¼�ܵĵȴ�ʱ��
    	for(DTNHost s:d){
    		if(s.getType()==0 &&s.getAverReplyTime()>0){
    			num++;
    			allTime+=s.getAverReplyTime();
    		}
    	}
    	allTime=allTime/num;
    	res=res+"������ѯ��ƽ���ظ��ȴ�ʱ��Ϊ��"+allTime;
    	return res;
    }
    
    //��ȡcloud�˵�ֱ����Ӧ��ѯ�ɹ���
    public static String showReplyByLocalRateOfCloud(){
    	String res="";
    	res=res+"Cloud�����ñ������ݿ�ֱ�ӻظ���ѯ�ı�����"+Cloud.getInstance().getReplyByLocalRate();
    	return res;
    }
    
    //��ȡcloud�˵Ŀռ�������
    public static String showRestSpaceRateOfCloud(){
    	String res="";
    	res=res+"Cloud �˵�ʣ��ռ����Ϊ��"+Cloud.getInstance().getRestSpaceRate();
    	return res;
    }
    //��ȡcloud�˻ظ�����ƽ��ʹ��ʱ��
    public static String showAverTimeOfCloud(){
    	String res="";
    	res=res+"Cloud�˵Ļظ���ѯƽ��ʹ��ʱ��Ϊ��"+Cloud.getInstance().getAverReplyTime();
    	return res;
    }
   

  
    //Ϊ����������������
    public static Data generDataForEnterQuery(Request q,int id){
    	return new Data(q.getTime(),id,q.getType(),q.getLevel(),q.getLocation());
    }
}
