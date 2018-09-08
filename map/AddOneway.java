package modifyRoadNetwork;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Record;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.osm.OSMDataset;
import org.neo4j.gis.spatial.osm.OSMDataset.Way;
import org.neo4j.gis.spatial.osm.OSMDataset.WayPoint;
import org.neo4j.gis.spatial.osm.OSMImporter;
import org.neo4j.gis.spatial.osm.OSMLayer;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.Node;

public class AddOneway {
	private static File basePath = new File("target/var");
    private static File dbPath = new File(basePath, "neo4j-db");
    
	private GraphDatabaseService db;
    
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbPath).newGraphDatabase();	
		try (Transaction tx = db.beginTx()) {
			
			//查询现在有多少个路段
            Result result_test1 = db.execute("match p=(m)-[r:NEXT]->(n) where EXISTS(r.length) return p");
            int count1=0;
            while (result_test1.hasNext()) {
            	result_test1.next();
            	count1++;
            }
            //54377
            System.out.println(count1);
			
            Result result1 = db.execute("MATCH (m)-[r:FIRST_NODE]->(n) RETURN m.oneway as oneway,n");
            //遍历每一条路，给每个路段都加上oneway属性
            while (result1.hasNext()) {
            	Map<String,Object> m1=result1.next();
            	//System.out.println(m1.toString());
            	String s="";
            	//把每一条路都赋予oneway属性=》现在是设为null，按both还是按forward？
            	if(m1.get("oneway") != null)
            		s=m1.get("oneway").toString();
            	else
            		s="BOTH";
            	Node n=(Node) m1.get("n");
                //System.out.println(s);
                //System.out.println(n.getId());
                //对从n开始的next，每个路段都加上一个属性
            	Boolean flag=true;
            	while(flag) {
            		//从一条路的头结点n开始，一个个找它的next和尾节点，把尾节点变成头结点
            		//BACKWARD则是反向next
            		if(s.equals("BACKWARD")) {
            			Result result2 = db.execute("match p=(m)-[r1:NEXT]->(n) where id(n)="+n.getId()+" return m,r1");
		                if (result2.hasNext()) {
		                	Map<String,Object> m2=result2.next();
		                	Relationship rs=(Relationship) m2.get("r1");
		                	//为n赋值成下一个节点
		                	n=(Node) m2.get("m");
		                	//在next关系上加上oneway属性
		                	db.execute("match p=()-[r1:NEXT]->() where id(r1)="+rs.getId()+" set r1.oneway='"+s+"'");               	
		                }
		                else
		                	flag=false;
            		}
            		else {
		                Result result2 = db.execute("match p=(m)-[r1:NEXT]->(n) where id(m)="+n.getId()+" return n,r1");
		                if (result2.hasNext()) {
		                	Map<String,Object> m2=result2.next();
		                	Relationship rs=(Relationship) m2.get("r1");
		                	n=(Node) m2.get("n");
		                	//在next关系上加上oneway属性
		                	db.execute("match p=()-[r1:NEXT]->() where id(r1)="+rs.getId()+" set r1.oneway='"+s+"'");               	
		                }
		                else
		                	flag=false;
            		}
            	}
            }
            
            //把路段上的oneway属性转换成oneway关系
            //此时所有的路段都有oneway和length属性
            Result result3 = db.execute("MATCH (m)-[r:NEXT]->(n) WHERE r.oneway IS NOT NULL RETURN r.oneway as oneway,r.length as length,m,n");
            while(result3.hasNext()) {
            	Map<String,Object> m1=result3.next();
            	Node m=(Node) m1.get("m");
            	Node n=(Node) m1.get("n");
            	String s=(String) m1.get("oneway");
            	double l=(double) m1.get("length");
            	//如果是以下的情况要建立反向next（注意这里有问题要设置成都是null或者都是both）
            	if(s.equals("BOTH")) {
            		db.execute("MATCH (m)-[r:NEXT]->(n) WHERE id(m)="+m.getId()+" and id(n)="+n.getId()+
            				" CREATE (n)-[r2:NEXT{oneway:'BOTH',length:"+l+"}]->(m)");
            	}          	
            }
			//查询现在有多少个路段(97482)
            Result result_test = db.execute("match p=(m)-[r:NEXT]->(n) where r.oneway IS NOT NULL return p");
            int count=0;
            while (result_test.hasNext()) {
            	Map<String,Object> m1=result_test.next();
            	//System.out.println(m1.get("p").toString());
            	count++;
            }
            //54377
            System.out.println(count);
            
			//加上新next
			//db.execute("match (a)-[r1:NODE]->(b),(a)-[r2:NEXT]->(c),(c)-[r3:NODE]->(d) create (b)-[r4:NEXT]->(d)");
			//db.execute("match (a)-[r1:NODE]->(b),(b)-[r2:NEXT]->(d),(a)-[r3:NEXT]->(c),(c)-[r4:NODE]->(d) SET r2.length=r3.length");
            tx.success();
        }
		db.shutdown();
	}

}
