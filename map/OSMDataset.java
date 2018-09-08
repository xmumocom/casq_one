/*
 * Copyright (c) 2010-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j Spatial.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gis.spatial.osm;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.cypher.internal.compiler.v3_1.pipes.matching.TraversalPathExpander;
import org.neo4j.gis.spatial.GeometryEncoder;
import org.neo4j.gis.spatial.Layer;
import org.neo4j.gis.spatial.SpatialDatabaseException;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.SpatialDataset;
import org.neo4j.gis.spatial.SpatialRelationshipTypes;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.path.TraversalAStar;
import org.neo4j.graphalgo.impl.path.TraversalPathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Resource;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class OSMDataset implements SpatialDataset, Iterable<OSMDataset.Way>, Iterator<OSMDataset.Way> {
    private OSMLayer layer;
    private Node datasetNode;
    private Iterator<Node> wayNodeIterator;

    /**
     * This method is used to construct the dataset on an existing node when the node id is known,
     * which is the case with OSM importers.
     */
    public OSMDataset(SpatialDatabaseService spatialDatabase, OSMLayer osmLayer, Node layerNode, long datasetId) {
        try (Transaction tx = spatialDatabase.getDatabase().beginTx()) {
            this.layer = osmLayer;
            this.datasetNode = spatialDatabase.getDatabase().getNodeById(datasetId);
            Relationship rel = layerNode.getSingleRelationship(SpatialRelationshipTypes.LAYERS, Direction.INCOMING);
            if (rel == null) {
                    datasetNode.createRelationshipTo(layerNode, SpatialRelationshipTypes.LAYERS);
            } else {
                Node node = rel.getStartNode();
                if (!node.equals(datasetNode)) {
                    throw new SpatialDatabaseException("Layer '" + osmLayer + "' already belongs to another dataset: " + node);
                }
            }
            tx.success();
        }

    }

    /**
     * This method is used to construct the dataset when only the layer node is known, and the
     * dataset node needs to be searched for.
     */
    public OSMDataset(SpatialDatabaseService spatialDatabase, OSMLayer osmLayer, Node layerNode) {
        try (Transaction tx = layerNode.getGraphDatabase().beginTx())
        {
            this.layer = osmLayer;
            Relationship rel = layerNode.getSingleRelationship( SpatialRelationshipTypes.LAYERS, Direction.INCOMING );
            if ( rel == null )
            {
                throw new SpatialDatabaseException( "Layer '" + osmLayer + "' does not have an associated dataset" );
            }
            else
            {
                datasetNode = rel.getStartNode();
            }
            tx.success();
        }
    }
    //获取所有的用户节点146个，等价于MATCH p=(m)-[r1:USERS]->(n),q=(n)-[r2:OSM_USER]-(t) where m.type="osm" RETURN q
    //深度优先，添加遍历的方向和关系（向外遍历所有的USERS和OSM_USER），比较the type of the last relationship是OSM_USER
    //如果去除evaluator，会多两个节点node1和node4，即通过includeWhereLastRelationshipTypeIs来保留路径上最后关系是OSM_USER的节点
    //这里有一点疑惑：怎么判断last relationship，目前可以确定的是和方向无关。
    //如果改成relationship的结果：(4)-[OSM_USER,127341]->(94275)
	public Iterable<Node> getAllUserNodes() {
		TraversalDescription td = datasetNode.getGraphDatabase().traversalDescription()
				.depthFirst()
				.relationships( OSMRelation.USERS, Direction.OUTGOING )
				.relationships( OSMRelation.OSM_USER, Direction.OUTGOING )
				.evaluator( Evaluators.includeWhereLastRelationshipTypeIs( OSMRelation.OSM_USER ) );
		return td.traverse( datasetNode ).nodes();
	}
	//在上一步的基础上，加了一个指向内部的user关系，且要求路径上最后关系是USER的节点
	//等价于MATCH p=(m)-[r1:USERS]->(n),q=(n)-[r2:OSM_USER]-(t),s=(o)-[r3:USER]->(t) where m.type="osm" RETURN s
	//这个深度优先就能比较明显了，比如Node[42401]、Node[42411]、Node[33261]这三个点就在一起
	public Iterable<Node> getAllChangesetNodes() {
		TraversalDescription td = datasetNode.getGraphDatabase().traversalDescription()
				.depthFirst()
				.relationships( OSMRelation.USERS, Direction.OUTGOING )
				.relationships( OSMRelation.OSM_USER, Direction.OUTGOING )
				.relationships( OSMRelation.USER, Direction.INCOMING )
				.evaluator( Evaluators.includeWhereLastRelationshipTypeIs( OSMRelation.USER ) );
		return td.traverse( datasetNode ).nodes();
	}
	//获取所有的道路6190条，用node来表示
	//也是从node1开始，获取ways和next关系，从第一个路55402开始，一直到每一个next。ways就是包含所有的道路。只去除最初的node1节点。
	//等价于MATCH p=(m)-[r1:WAYS]->(n),q=(n)-[r2:NEXT*..]->(t) RETURN q
	public Iterable<Node> getAllWayNodes() {
		TraversalDescription td = datasetNode.getGraphDatabase().traversalDescription()
				.depthFirst()
				.relationships( OSMRelation.WAYS, Direction.OUTGOING )
				.relationships( OSMRelation.NEXT, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		return td.traverse( datasetNode ).nodes();
	}
	//获取所有的point node节点，一共50719个(一些没有出现在道路上的节点不算)
	//依次获取这四种关系,过滤留下最后关系是NODE的点（不包括FIRST_NODE）
	//There's no priority or order in which types to traverse.这里说没有顺序，也就是这些relationship是没有顺序的
	//没找到语义上等价的语句
	public Iterable<Node> getAllPointNodes() {
		TraversalDescription td = datasetNode.getGraphDatabase().traversalDescription()
				.depthFirst()
				.relationships( OSMRelation.WAYS, Direction.OUTGOING )
				.relationships( OSMRelation.NEXT, Direction.OUTGOING )
				.relationships( OSMRelation.FIRST_NODE, Direction.OUTGOING )
				.relationships( OSMRelation.NODE, Direction.OUTGOING )
				.evaluator( Evaluators.includeWhereLastRelationshipTypeIs( OSMRelation.NODE ) );
		return td.traverse( datasetNode ).nodes();
	}
	//获取某个道路上的所有点。
	//首先求该道路的FIRST_NODE节点，然后再代入这个遍历中
	public Iterable<Node> getWayNodes(Node way) {
        TraversalDescription td = datasetNode.getGraphDatabase().traversalDescription()
                .depthFirst()
                .relationships( OSMRelation.NEXT, Direction.OUTGOING )
                .relationships( OSMRelation.NODE, Direction.OUTGOING )
                .evaluator( Evaluators.includeWhereLastRelationshipTypeIs( OSMRelation.NODE ) );
        return td.traverse(
                way.getSingleRelationship( OSMRelation.FIRST_NODE, Direction.OUTGOING ).getEndNode()
        ).nodes();
    }
	//获取某个道路的changeset
	public Node getChangeset(Node way) {
		try {
			return way.getSingleRelationship(OSMRelation.CHANGESET, Direction.OUTGOING).getEndNode();
		} catch (Exception e) {
			System.out.println("Node has no changeset: " + e.getMessage());
			return null;
		}
	}
	//获取某个node的所有历史用户
	public Node getUser(Node nodeWayOrChangeset) {
		TraversalDescription td = datasetNode.getGraphDatabase().traversalDescription()
				.depthFirst()
				.relationships( OSMRelation.CHANGESET, Direction.OUTGOING )
				.relationships( OSMRelation.USER, Direction.OUTGOING )
				.evaluator( Evaluators.includeWhereLastRelationshipTypeIs( OSMRelation.USER ) );
		Iterator<Node> results = td.traverse( nodeWayOrChangeset ).nodes().iterator();
		return results.hasNext() ? results.next() : null;
	}

	public Way getWayFromId(long id) {
		return getWayFrom(datasetNode.getGraphDatabase().getNodeById(id));
	}
	//获取某个节点的所有道路，注意这里是incoming了
	public Way getWayFrom(Node osmNodeOrWayNodeOrGeomNode) {
		TraversalDescription td = datasetNode.getGraphDatabase().traversalDescription()
				.depthFirst()
				.relationships( OSMRelation.NODE, Direction.INCOMING )
				.relationships( OSMRelation.NEXT, Direction.INCOMING )
				.relationships( OSMRelation.FIRST_NODE, Direction.INCOMING )
				.relationships( OSMRelation.GEOM, Direction.INCOMING )
				.evaluator(path -> path.endNode().hasProperty( "way_osm_id" ) ? Evaluation.INCLUDE_AND_PRUNE
                                                                  : Evaluation.EXCLUDE_AND_CONTINUE);
		Iterator<Node> results = td.traverse( osmNodeOrWayNodeOrGeomNode ).nodes().iterator();
		return results.hasNext() ? new Way(results.next()) : null;
	}
	
	public Iterator<Node> getOsmWayFrom(Node osmNodeOrWayNodeOrGeomNode) {
		TraversalDescription td = datasetNode.getGraphDatabase().traversalDescription()
				.depthFirst()
				.relationships( OSMRelation.NODE, Direction.INCOMING )
				.relationships( OSMRelation.NEXT, Direction.INCOMING )
				.relationships( OSMRelation.FIRST_NODE, Direction.INCOMING )
				.relationships( OSMRelation.GEOM, Direction.INCOMING )
				.evaluator(path -> path.endNode().hasProperty( "way_osm_id" ) ? Evaluation.INCLUDE_AND_PRUNE
                                                                  : Evaluation.EXCLUDE_AND_CONTINUE);
		Iterator<Node> results = td.traverse( osmNodeOrWayNodeOrGeomNode ).nodes().iterator();
		return results;
	}
	//下面是根据osmid获取node
	//效率：100个1.357s
	public Node getNodeFromOSMId(String osmid) {
		TraversalDescription td = datasetNode.getGraphDatabase().traversalDescription()
				.depthFirst()
				.relationships( OSMRelation.WAYS, Direction.OUTGOING )
				.relationships( OSMRelation.NEXT, Direction.OUTGOING )
				.relationships( OSMRelation.FIRST_NODE, Direction.OUTGOING )
				.relationships( OSMRelation.NODE, Direction.OUTGOING )
				.evaluator( Evaluators.includeWhereLastRelationshipTypeIs( OSMRelation.NODE) );
		ResourceIterable<Node> results =td.traverse( datasetNode ).nodes();
		for(Node n:results) {
			if(n.hasProperty("node_osm_id")&&n.getProperty("node_osm_id").toString().equals(osmid))
				return n;
    	}
		return null;
	}
	//效率：100个0.925s(如果换成match (n) where n.node_osm_id=，效率就会很低要7.8s)
	public Node getNodeFromOSMId_m(long osmid) {
		GraphDatabaseService db=datasetNode.getGraphDatabase();
		try (Transaction tx = db.beginTx()) {	
			//查询现在有多少个路段
            Result result_test1 = db.execute("match (m)-[r:NODE]->(n) where n.node_osm_id="+osmid+" return n");
            Map<String,Object> m1=result_test1.next();
            Node n=(Node) m1.get("n");
            tx.success();
            return n;
        }
	}
	public Iterator<Node> getnWayFromOSMId(long osmid) {
		GraphDatabaseService db=datasetNode.getGraphDatabase();
		try (Transaction tx = db.beginTx()) {	
			//查询现在有多少个路段
            Result result_test1 = db.execute("match p=(m)-[r:NODE]->(n) where n.node_osm_id="+osmid+" return m");
            Map<String,Object> m1=result_test1.next();
            Node m=(Node) m1.get("m");
            tx.success();
            return (Iterator<Node>) m;
        }
	}
	public Iterator<Node> getnWayFromOSMId_m(long osmid) {
		GraphDatabaseService db=datasetNode.getGraphDatabase();
		try (Transaction tx = db.beginTx()) {	
			//查询现在有多少个路段
            Result result_test1 = db.execute("match p=(m)-[r:NODE]->(n) where n.node_osm_id="+osmid+" return m");
            Map<String,Object> m1=result_test1.next();
            Node m=(Node) m1.get("m");
            tx.success();
            return (Iterator<Node>) m;
        }
	}
	//求两个节点之间的最短路径和长度
	public Iterable<Path> getBothShortestPath(Node node1, Node node2) {
		PathExpander expander=PathExpanders.forTypesAndDirections(OSMRelation.NODE, Direction.BOTH, OSMRelation.NEXT, Direction.BOTH);
	    PathFinder<Path> finder=GraphAlgoFactory.shortestPath( expander,10000);
	    //本应该6190条路段以及一些node之间的转换（不同way），但当值设的比较大时，就会内存溢出，暂设为200吧
	    
	    Iterable<Path> paths = finder.findAllPaths(node1, node2);
		//先获取oneway（有node_osm_id的才有可能有oneway）
	    //已经判断出如果是BACKWARD，则next是反向的；所以不管是FORWARD或BACKWARD，都有next代表了
	    //对于BOTH来说，可以沿着反NEXT的方向
	    //对于没有oneway属性的来说，认为是BOTH
	    //把oneway属性加入到每一个路段上（）
	    return paths;
	}
	//求两个节点之间的最短长度的路径
		public WeightedPath getdijkstraLengthShortestPath(Node node1, Node node2) {		
			PathExpander expander=PathExpanders.forTypesAndDirections(OSMRelation.NODE, Direction.BOTH, OSMRelation.NEXT, Direction.OUTGOING);
			//PathExpander expander=PathExpanders.forTypeAndDirection(OSMRelation.NEXT, Direction.BOTH);
		    //EstimateEvaluator<Double> estimateEvaluator;
		    //PathFinder<WeightedPath> finder=GraphAlgoFactory.aStar(expander, lengthEvaluator, estimateEvaluator);
		    PathFinder<WeightedPath> finder= GraphAlgoFactory.dijkstra(expander, CommonEvaluators.doubleCostEvaluator("length",0));

		    WeightedPath path = finder.findSinglePath(node1, node2);
		    return path;
		}
		//求两个节点之间的最短长度的路径
		public WeightedPath getAStarLengthShortestPath(Node node1, Node node2) {		
			PathExpander expander=PathExpanders.forTypesAndDirections(OSMRelation.NODE, Direction.BOTH, OSMRelation.NEXT, Direction.OUTGOING);
			//PathExpander expander=PathExpanders.forTypeAndDirection(OSMRelation.NEXT, Direction.BOTH);
			CostEvaluator<Double> lengthEvaluator=CommonEvaluators.doubleCostEvaluator("length",0);
		    EstimateEvaluator<Double> estimateEvaluator=CommonEvaluators.geoEstimateEvaluator("lat", "lon");
		    PathFinder<WeightedPath> finder=GraphAlgoFactory.aStar(expander, lengthEvaluator, estimateEvaluator);

		    WeightedPath path = finder.findSinglePath(node1, node2);
		    return path;
		}
	public Iterable<Path> getWardShortestPath(Node node1, Node node2) {
		PathExpander expander=PathExpanders.forTypesAndDirections(OSMRelation.NODE, Direction.BOTH, OSMRelation.NEXT, Direction.OUTGOING);
	    PathFinder<Path> finder=GraphAlgoFactory.shortestPath( expander,10000);//6190条路段以及一些node之间的转换（不同way）
	    Iterable<Path> paths = finder.findAllPaths(node1, node2);
		//先获取oneway（有node_osm_id的才有可能有oneway）
	    //已经判断出如果是BACKWARD，则next是反向的；所以不管是FORWARD或BACKWARD，都有next代表了
	    //对于BOTH来说，可以沿着反NEXT的方向
	    //对于没有oneway属性的来说，认为是BOTH
	    //把oneway属性加入到每一个路段上（）
	    return paths;
	}
	//求两个节点之间的所有路径和长度(设成50才能比较快的选择出来)
	public Iterable<Path> getAllPaths(Node node1, Node node2, int l) {
		//先试着考虑next的双向，即不考虑查看way的oneway
		PathExpander expander=PathExpanders.forTypesAndDirections(OSMRelation.NODE, Direction.BOTH, OSMRelation.NEXT, Direction.BOTH);

	    PathFinder<Path> finder=GraphAlgoFactory.allPaths( expander,l);

	    Iterable<Path> paths = finder.findAllPaths(node1, node2);
	    int count=0;
	    Iterator it=paths.iterator();
	    while(it.hasNext()) {
	    	count++;
	    	it.next();
	    }
	    //System.out.println(count);	
	    if(count<=50000)
	    	return paths;
	    else
	    	return null;
	}
	//获取两个节点之间length最短的路径
	public Path getLengthShortestPaths(Node node1, Node node2) {
		//先试着考虑next的双向，即不考虑查看way的oneway
		PathExpander expander=PathExpanders.forTypesAndDirections(OSMRelation.NODE, Direction.BOTH, OSMRelation.NEXT, Direction.BOTH);
	    PathFinder<Path> finder=GraphAlgoFactory.allPaths( expander,6190);
	    Iterable<Path> paths = finder.findAllPaths(node1, node2);
	    double minlength=1000000;
	    Path p = null;
	    for(Path allPath: paths) {
            System.out.println(allPath.toString());
            System.out.println(allPath.length());
            Iterable<Relationship> itrelationship=allPath.relationships();
            double l=0;
            for(Relationship n:itrelationship) {
            	if(n.hasProperty("length")) {
            		//System.out.println(n.getProperty("length"));
            		l+=Double.valueOf(n.getProperty("length").toString());
            	}
            }
            if(l<minlength) {
            	minlength=l;
            	p=allPath;
            }
        }
	    return p;
	}
	
	public class OSMNode {
		protected Node node;
		protected Node geomNode;
		protected Geometry geometry;

		OSMNode(Node node) {
			this.node = node;
			Relationship geomRel = this.node.getSingleRelationship(OSMRelation.GEOM, Direction.OUTGOING);
			if(geomRel != null) geomNode = geomRel.getEndNode();
		}
		
		public Way getWay() {
			return OSMDataset.this.getWayFrom(this.node);
		}

		public Geometry getGeometry() {
			if(geometry == null && geomNode != null) {
				geometry = layer.getGeometryEncoder().decodeGeometry(geomNode);
			}
			return geometry;
		}

		public Envelope getEnvelope() {
			return getGeometry().getEnvelopeInternal();
		}
		
		public boolean equals(OSMNode other) {
			return this.node.getId() == other.node.getId();
		}
		
		public Node getNode() {
			return node;
		}

		public String toString() {
			if (node.hasProperty("name")) {
				return node.getProperty("name").toString();
			} else if (getGeometry() != null) {
				return getGeometry().getGeometryType();
			} else {
				return node.toString();
			}
		}
	}

	public class Way extends OSMNode implements Iterable<WayPoint>, Iterator<WayPoint> {
		private Iterator<Node> wayPointNodeIterator;
		Way(Node node) {
			super(node);
		}
		
		Iterable<Node> getWayNodes() {
			return OSMDataset.this.getWayNodes(this.node);
		}
		
		public Iterable<WayPoint> getWayPoints() {
			return this;
		}

		public Iterator<WayPoint> iterator() {
			if(wayPointNodeIterator==null || !wayPointNodeIterator.hasNext()) {
				wayPointNodeIterator = getWayNodes().iterator();
			}
			return this;
		}

		public boolean hasNext() {
			return wayPointNodeIterator.hasNext();
		}

		public WayPoint next() {
			return new WayPoint(wayPointNodeIterator.next());
		}

		public void remove() {
			throw new UnsupportedOperationException("Cannot modify way-point collection");
		}

		public WayPoint getPointAt(Coordinate coordinate) {
			for (WayPoint wayPoint : getWayPoints()) {
				if (wayPoint.isAt(coordinate))
					return wayPoint;
			}
			return null;
		}

	}

	public class WayPoint extends OSMNode {
		WayPoint(Node node) {
			super(node);
		}

		boolean isAt(Coordinate coord) {
			return getCoordinate().equals(coord);
		}

		public Coordinate getCoordinate() {
			return new Coordinate(getX(), getY());
		}

		private double getY() {
			return (Double) node.getProperty("latitude", 0.0);
		}

		private double getX() {
			return (Double) node.getProperty("longitude", 0.0);
		}
	}

	public Iterable< ? extends Geometry> getAllGeometries() {
        //@TODO: support multiple layers
        return layer.getAllGeometries();
    }

    public Iterable<Node> getAllGeometryNodes() {
        //@TODO: support multiple layers
        return layer.getAllGeometryNodes();
    }

    public boolean containsGeometryNode(Node geomNode) {
        //@TODO: support multiple layers
        return layer.containsGeometryNode(geomNode);
    }

    public GeometryEncoder getGeometryEncoder() {
        //@TODO: support multiple layers
        return layer.getGeometryEncoder();
    }

    public Iterable< ? extends Layer> getLayers() {
        return Collections.singletonList(layer);
    }

	public Iterable<Way> getWays() {
		return this;
	}

	public Iterator<Way> iterator() {
		if(wayNodeIterator==null || !wayNodeIterator.hasNext()) {
			wayNodeIterator = getAllWayNodes().iterator();
		}
		return this;
	}

	public boolean hasNext() {
		return wayNodeIterator.hasNext();
	}

	public Way next() {
		return new Way(wayNodeIterator.next());
	}

	public void remove() {
		throw new UnsupportedOperationException("Cannot modify way collection");
	}

	public int getPoiCount() {
		return (Integer) this.datasetNode.getProperty("poiCount", 0);
	}

	public int getNodeCount() {
		return (Integer) this.datasetNode.getProperty("nodeCount", 0);
	}

	public int getWayCount() {
		return (Integer) this.datasetNode.getProperty("wayCount", 0);
	}

	public int getRelationCount() {
		return (Integer) this.datasetNode.getProperty("relationCount", 0);
	}

	public int getChangesetCount() {
		return (Integer) this.datasetNode.getProperty("changesetCount", 0);
	}

	public int getUserCount() {
		return (Integer) this.datasetNode.getProperty("userCount", 0);
	}
	//最短路径 START d=node(1), e=node(2) MATCH p = shortestPath( d-[*..15]->e ) RETURN p
	//http://www.uml.org.cn/sjjm/201203063.asp
}
