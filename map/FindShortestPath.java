package modifyRoadNetwork;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.osm.OSMDataset;
import org.neo4j.gis.spatial.osm.OSMDataset.Way;
import org.neo4j.gis.spatial.osm.OSMDataset.WayPoint;
import org.neo4j.gis.spatial.osm.OSMImporter;
import org.neo4j.gis.spatial.osm.OSMLayer;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class FindShortestPath {
	private static File basePath = new File("target/var");
    private static File dbPath = new File(basePath, "neo4j-db");
    
	private GraphDatabaseService db;
    
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbPath).newGraphDatabase();	
		
		try (Transaction tx = db.beginTx()) {
			SpatialDatabaseService spatial = new SpatialDatabaseService(db);
	    	OSMLayer layer = (OSMLayer) spatial.getLayer("data/map.osm");
	    	Node layerNode=layer.getLayerNode();

	    	OSMDataset osmDs=new OSMDataset(spatial,layer,layerNode);
	    	
	    	int count_poi=osmDs.getPoiCount();
	    	int count_node=osmDs.getNodeCount();
	    	int count_way=osmDs.getWayCount();
	    	System.out.println(count_poi+":"+count_node+":"+count_way);
	    	
	    	Node nid1=osmDs.getNodeFromOSMId("1422596956");
	    	Node nid2=osmDs.getNodeFromOSMId("1422597068");
	    	
	    	Instant inst1 = Instant.now();
	    	//System.out.println("local time now : " + LocalTime.now());
	    	
	    	int n=0;
	    	while(n<10000) {
	    		WeightedPath p1=osmDs.getdijkstraLengthShortestPath(nid1, nid2);
	    		if(n==0)
	    			System.out.println(p1);
	    		n++;
	    	}
	    	Instant inst2 = Instant.now();
	    	System.out.println("local time now : " + (Duration.between(inst1, inst2).toMillis()));
	    	//运行结果：64550407和63845413=>(498)<--[NODE,55406]--(55404)--[NEXT,55409]-->(55405)--[NEXT,55411]-->(55406)--[NODE,55410]-->(312) weight:83.79734875117573
	    	
	    	//1422598603=》1422598605单向路到1422598675=》反向只有三步
	    	//(12262)<--[NODE,139279]--(100869)--[NEXT,139282]-->(100870)--[NEXT,139284]-->(100871)--[NEXT,139286]-->(100872)--[NEXT,139288]-->(100873)--[NEXT,139290]-->(100874)--[NEXT,139292]-->(100875)--[NEXT,139294]-->(100876)--[NEXT,139296]-->(100877)--[NEXT,139298]-->(100878)--[NEXT,139300]-->(100879)--[NEXT,139302]-->(100880)--[NEXT,139304]-->(100881)--[NEXT,139306]-->(100882)--[NEXT,139308]-->(100883)--[NEXT,139310]-->(100884)--[NEXT,139312]-->(100885)--[NEXT,139314]-->(100886)--[NEXT,139316]-->(100887)--[NEXT,139318]-->(100888)--[NEXT,139320]-->(100889)--[NEXT,139322]-->(100890)--[NEXT,139324]-->(100891)--[NEXT,139326]-->(100892)--[NEXT,139328]-->(100893)--[NEXT,139330]-->(100894)--[NODE,139329]-->(38776)<--[NODE,158374]--(111036)--[NEXT,158377]-->(111037)--[NEXT,158379]-->(111038)--[NEXT,158381]-->(111039)--[NEXT,158383]-->(111040)--[NEXT,158385]-->(111041)--[NEXT,158387]-->(111042)--[NEXT,158389]-->(111043)--[NEXT,158391]-->(111044)--[NEXT,158393]-->(111045)--[NODE,158392]-->(38784)<--[NODE,158942]--(111335)--[NEXT,158945]-->(111336)--[NEXT,158947]-->(111337)--[NEXT,158949]-->(111338)--[NEXT,158951]-->(111339)--[NEXT,158953]-->(111340)--[NODE,158952]-->(39043)<--[NODE,75826]--(66411)--[NEXT,75829]-->(66412)--[NEXT,75831]-->(66413)--[NEXT,75833]-->(66414)--[NEXT,75835]-->(66415)--[NODE,75834]-->(11928)<--[NODE,78633]--(68010)--[NEXT,78636]-->(68011)--[NEXT,78638]-->(68012)--[NEXT,78640]-->(68013)--[NODE,78639]-->(11943)<--[NODE,142401]--(102557)--[NEXT,142404]-->(102558)--[NEXT,142406]-->(102559)--[NEXT,142408]-->(102560)--[NEXT,142410]-->(102561)--[NEXT,142412]-->(102562)--[NODE,142411]-->(11976)<--[NODE,76248]--(66647)--[NEXT,76251]-->(66648)--[NODE,76250]-->(11980)<--[NODE,134387]--(98192)--[NEXT,134390]-->(98193)--[NEXT,134392]-->(98194)--[NEXT,134394]-->(98195)--[NEXT,134396]-->(98196)--[NEXT,134398]-->(98197)--[NEXT,134400]-->(98198)--[NEXT,134402]-->(98199)--[NEXT,134404]-->(98200)--[NEXT,134406]-->(98201)--[NEXT,134408]-->(98202)--[NEXT,134410]-->(98203)--[NEXT,134412]-->(98204)--[NODE,134411]-->(12053)<--[NODE,125291]--(93160)--[NEXT,125294]-->(93161)--[NEXT,125296]-->(93162)--[NEXT,125298]-->(93163)--[NODE,125297]-->(31038)<--[NODE,133267]--(97594)--[NEXT,133270]-->(97595)--[NEXT,133272]-->(97596)--[NEXT,133274]-->(97597)--[NEXT,133276]-->(97598)--[NEXT,133278]-->(97599)--[NODE,133277]-->(29098)<--[NODE,132597]--(97217)--[NEXT,132600]-->(97218)--[NEXT,132602]-->(97219)--[NEXT,132604]-->(97220)--[NEXT,132606]-->(97221)--[NEXT,132608]-->(97222)--[NEXT,132610]-->(97223)--[NEXT,132612]-->(97224)--[NEXT,132614]-->(97225)--[NEXT,132616]-->(97226)--[NEXT,132618]-->(97227)--[NEXT,132620]-->(97228)--[NEXT,132622]-->(97229)--[NEXT,132624]-->(97230)--[NEXT,132626]-->(97231)--[NEXT,132628]-->(97232)--[NEXT,132630]-->(97233)--[NEXT,132632]-->(97234)--[NEXT,132634]-->(97235)--[NEXT,132636]-->(97236)--[NEXT,132638]-->(97237)--[NEXT,132640]-->(97238)--[NEXT,132642]-->(97239)--[NEXT,132644]-->(97240)--[NEXT,132646]-->(97241)--[NEXT,132648]-->(97242)--[NODE,132647]-->(29132)<--[NODE,75615]--(66304)--[NEXT,75618]-->(66305)--[NEXT,75620]-->(66306)--[NODE,75619]-->(12335) weight:4559.676251843255
	    	
	    	//两条长度不同但相近的路1422596956和1422597068
	    	
	    	
	    	n=0;
	    	while(n<10000) {
	    		WeightedPath p2=osmDs.getAStarLengthShortestPath(nid1, nid2);
	    		if(n==0)
	    			System.out.println(p2);
	    		n++;
	    	}
	    	Instant inst3 = Instant.now();
	    	System.out.println("local time now : " + (Duration.between(inst2, inst3).toMillis()));
	    	
			tx.success();
		}
		db.shutdown();
	}

}
