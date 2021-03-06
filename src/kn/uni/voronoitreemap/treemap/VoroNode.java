/*******************************************************************************
 * Copyright (c) 2013 Arlind Nocaj, University of Konstanz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * For distributors of proprietary software, other licensing is possible on request: arlind.nocaj@gmail.com
 * 
 * This work is based on the publication below, please cite on usage, e.g.,  when publishing an article.
 * Arlind Nocaj, Ulrik Brandes, "Computing Voronoi Treemaps: Faster, Simpler, and Resolution-independent", Computer Graphics Forum, vol. 31, no. 3, June 2012, pp. 855-864
 ******************************************************************************/
package kn.uni.voronoitreemap.treemap;


import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;

import kn.uni.voronoitreemap.core.VoronoiCore;
import kn.uni.voronoitreemap.extension.VoroCellObject;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;



/**
 * Represents a node in the Voronoi Treemap hierarchy. This node is put into the computation queue. 
 * 
 * @author Arlind Nocaj
 * 
 */
public class VoroNode implements VoroCellObject {

	//pointers to higher levels of the hierarchy
	/**
	 * tree information
	 */
	private VoronoiTreemap treemap;
	private VoroNode parent;
	private ArrayList<VoroNode> children;
	
	private Integer nodeID;
	private int height=0;
	private PolygonSimple polygon;
	private boolean toConsider = true;
	
	private double weight=1;
	private VoronoiCore core;
	private Site site;
	private double wantedPercentage=0;
	private Point2D relativeVector;
	
	public VoroNode(int nodeID) {
		this.nodeID = nodeID;
	}
	
	public VoroNode(int nodeID, int numberChildren){
		this(nodeID);
		children=new ArrayList<VoroNode>(numberChildren);
	}

	public void calculateWeights() {
		treemap.amountAllNodes++;
		if (children == null || children.size() == 0) {
			return;
		}
		double sum = 0;
		for (VoroNode child : children) {
			child.calculateWeights();
			sum += child.getWeight();
		}

		for (VoroNode child : children) {
			child.setWantedPercentage(child.getWeight() / sum);
		}
		this.weight = sum;

	}

//	@Override
//	public void doFinalWork() {
//		polygonComponent.setVoroPolygon(polygon, getHeight());
//		if (hasLeaf) {
//			polygonComponent.setIsLast(true);
//		}
//
//		if (this.getPolygon().getArea() / getParent().getPolygon().getArea() > 0.9) {
//			// our area is not much smaller then the area of our parent, we
//			// still need to do make the font smaller
//			polygonComponent.setMakeFontSmaller(true);
//		}
//		polygonComponent.setFillColor(Colors.getColors().get(getHeight()));
//
//		// polygonComponent.setFillColor(VoronoiTreemap.interpolColor.getColorLinear(scoreValue,80));
//		if (!polygonComponent.isVisible())
//			polygonComponent.setVisible(true);
//		polygonComponent.doFinalWork();
//		// save relative position to the centroid of the parent
//		// NPolygon2D parentPolygon = parent.getPolygon();
//		// Point2D parentCentroid = parentPolygon.calculateCentroid();
//		// Point2D myPosition = polygon.calculateCentroid();
//		// setRelativeVector(new Punkt(myPosition.getX() -
//		// parentCentroid.getX(),
//		// myPosition.getY() - parentCentroid.getY()));
//	}


	public void setNodeID(int nodeID) {
		this.nodeID = nodeID;
	}

	public int getNodeID() {
		return nodeID;
	}

	public void setParent(VoroNode parent) {
		this.parent = parent;
	}

	public VoroNode getParent() {
		return parent;
	}

	public void setChildren(ArrayList<VoroNode> children) {
		this.children = children;
	}

	public ArrayList<VoroNode> getChildren() {
		return children;
	}

	public void setPolygon(PolygonSimple polygon) {
		this.polygon = polygon;
	}

	public PolygonSimple getPolygon() {
		return polygon;
	}

	public void setToConsider(boolean toConsider) {
		this.toConsider = toConsider;
	}

	public boolean isToConsider() {
		return toConsider;
	}

	public void setWantedPercentage(double percentage) {
		this.weight=percentage;
		this.wantedPercentage = percentage;
		if (site != null)
			site.setPercentage(percentage);
	}

	public double getWantedPercentage() {
		return wantedPercentage;
	}

	public void addChild(VoroNode child) {
		if (children == null) {
			children = new ArrayList<VoroNode>();
		}
		children.add(child);
		child.parent = this;
		child.treemap = treemap;
		child.height = height + 1;
	}

	public void iterate() {
//		System.out.println("VoroNode begin Iteration Node: " + getNodeID()+ " Layer: " + getHeight() + "  " + Arrays.toString(getChildrenIDs()));
		if (children == null || children.size() == 0)
			return;
		if (site != null)
			polygon=this.site.getPolygon();
		scaleRelativeVectors();
		if (this.core == null) {
			core = new VoronoiCore(this.polygon);
			setSettingsToCore();
			// add each children as a site
			for (VoroNode child : children) {
				Point2D p = polygon.getRelativePosition(child.relativeVector);
			
				Site s = new Site(p.getX(), p.getY());

				s.setPercentage(child.wantedPercentage);
				s.setData(child);
				core.addSite(s);
				child.setSite(s);
				s.cellObject = child;
			}
		} else {
			// move my children so that they are in my polygon
			// use their relative Vector for that

			core = new VoronoiCore(polygon);
			setSettingsToCore();
			for (VoroNode child : children) {

				if (child.getWantedPercentage() > 0) {
					Point2D pos = null;
					// if (getParent() != null) {
					if (child.relativeVector != null) {
						pos = polygon.getRelativePosition(child.relativeVector);
					} else {
						pos = polygon.getInnerPoint();
					}
					child.site.setXY(pos.getX(), pos.getY());
					// } else {
					// // for the root childs we can directly set the old
					// // location
					// //in this case just leave the old location
					//					
					// //we have precalculated relative Vectors for each cell,
					// so use them also for the root
					// }
					// child.site.setWeight(0.1);
					core.addSite(child.site);
				}
			}

		}

		
		core.doIterate();

		if (treemap.getUseBorder()) {
			double shrinkPercentage=treemap.getShrinkPercentage();
			int length = core.getSites().size;
			Site[] sites = core.getSites().array;
			for (int i = 0; i < length; i++) {
				Site s = sites[i];
				s.getPolygon().shrinkForBorder(shrinkPercentage);
			}
		}
	}

	/**
	 * Scales the relative vectors of the child nodes to the size of our current
	 * polygon. Doing this helps to preserve the mental map.
	 */
	public void scaleRelativeVectors() {
		if (getChildren() == null)
			return;
		if (getChildren().size() == 1) {
			VoroNode child = getChildren().get(0);
			child.setRelativeVector(polygon.getInnerPoint());
			return;
		}
		Rectangle bounds = polygon.getBounds();

		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		double localCenterX = 0;
		double localCenterY = 0;
		for (VoroNode child : getChildren()) {
			Point2D pos = child.getRelativeVector();
			// TODO
			if (pos == null) {
				pos=this.polygon.getInnerPoint();
			}

			localCenterX += pos.getX();
			localCenterY += pos.getY();

			if (pos.getX() < minX) {
				minX = pos.getX();
			}
			if (pos.getX() > maxX) {
				maxX = pos.getX();
			}
			if (pos.getY() < minY) {
				minY = pos.getY();
			}
			if (pos.getY() > maxY) {
				maxY = pos.getY();
			}
		}
		localCenterX = localCenterX / getChildren().size();
		localCenterY = localCenterY / getChildren().size();

		double scaleX = (bounds.getWidth() / (maxX - minX)) * 0.9;
		double scaleY = (bounds.getHeight() / (maxY - minY)) * 0.9;
		double centerX = bounds.getCenterX();
		double centerY = bounds.getCenterY();

		for (VoroNode child : getChildren()) {
			Point2D pos = child.getRelativeVector();
			pos.setLocation((pos.getX() - localCenterX) * scaleX,
					(pos.getY() - localCenterY) * scaleY);
		}
	}

	private void setSettingsToCore(){
		core.setPreflowPercentage(treemap.getPreflowPercentage());
		core.setPreflowIncrease(treemap.getPreflowIncrease());
		core.setUseExtrapolation(treemap.getUseExtrapolation());
		core.setCancelOnAreaErrorThreshold(treemap.getCancelOnThreshold());
		core.setErrorAreaThreshold(treemap.getCancelErrorThreshold());
		core.setCancelOnMaxIterat(treemap.getCancelOnMaxIteration());
		core.setGuaranteeValidCells(treemap.getGuaranteeValidCells());
		core.setNumberMaxIterations(treemap.getNumberMaxIterations());
		core.setUseNegativeWeights(treemap.isUseNegativeWeights());
		core.setAggressiveMode(treemap.getAggressiveMode());
		
	}
	
	public void increasePercentageDirectly() {
		weight = weight * 1.5;
		// site.percent=site.percent*1.5;
	}

	public void decreasePercentage() {
		site.setPercentage(site.getPercentage() * 0.4);
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getHeight() {
		return height;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getWeight() {
		return weight;
	}

	public void setSite(Site s) {
		this.site = s;
	}

	public Site getSite() {
		return site;
	}
	
	

	// public static void main(String[] args) {
	// long start = System.currentTimeMillis();
	// ArrayList<VoroNode> list = new ArrayList<VoroNode>();
	// ArrayList<JPolygon> polygon = new ArrayList<JPolygon>();
	// for (int i = 0; i < 10000; i++) {
	// list.add(new VoroNode(i));
	// // polygon.add(new JPolygon());
	// }
	// long end = System.currentTimeMillis();
	// System.out.println("Time to create:" + (end - start));
	// }

	public void setTreemap(VoronoiTreemap treemap) {
		this.treemap = treemap;
	}

	public VoronoiTreemap getTreemap() {
		return treemap;
	}

	public void setRelativeVector(Point2D relativeVector) {
		this.relativeVector = relativeVector;
	}

	public Point2D getRelativeVector() {
		return relativeVector;
	}


	@Override
	public void doFinalWork() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVoroPolygon(PolygonSimple polygon) {
		this.polygon=polygon;
	}
	
public int[] getChildrenIDs(){
	int length;
		if (children==null || (length=children.size())==0){
			return null;
		}
		int[] ids = new int[length];
		int i=0;
		for (VoroNode node:children){
			ids[i]=node.getNodeID();
			i++;
		}
		return ids;
	}

public PolygonSimple[] getChildrenPolygons(){
	int length;
	if (children==null || (length=children.size())==0){
		return null;
	}
	PolygonSimple[] polygons=new PolygonSimple[length];
	int i=0;
	for (VoroNode node:children){
		polygons[i]=node.getSite().getPolygon();
		i++;
	}
	return polygons;
}


}
