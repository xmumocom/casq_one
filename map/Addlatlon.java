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

public class Addlatlon {
	private static File basePath = new File("target/var");
    private static File dbPath = new File(basePath, "neo4j-db");
    
	private GraphDatabaseService db;
    
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbPath).newGraphDatabase();	
		try (Transaction tx = db.beginTx()) {
			
            Result result1 = db.execute("MATCH (m)-[r:NODE]->(n) RETURN m,n.lat as lat,n.lon as lon");
            //遍历每一条路，给每个路段都加上oneway属性
            while (result1.hasNext()) {
            	Map<String,Object> m1=result1.next();
            	
            	Node m=(Node) m1.get("m");
            	double lat=(double) m1.get("lat");
            	double lon=(double) m1.get("lon");
            	//注意：不能用and，要用逗号
            	db.execute("MATCH (m) WHERE id(m)="+m.getId()+" SET m.lat="+lat+", m.lon="+lon+""); 
            }  
            tx.success();
        }
		db.shutdown();
	}

}
