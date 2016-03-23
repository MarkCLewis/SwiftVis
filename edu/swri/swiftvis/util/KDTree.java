package edu.swri.swiftvis.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KDTree<T extends KDTree.TreePoint> {
	private List<T> points;
	private int axes;
	private KDNode<T> root;
	private boolean built = false;
	private final int numElemsPerNode;
	private double[] spread;
	
	
	// ------------------------------------------------
	// TESTING PURPOSES
	// ------------------------------------------------	
	public static void main(String[] args) {
		List<TreePoint> data = new ArrayList<TreePoint>();
		for(int i = 0; i < 100; i++) {
			data.add(new testClass(new double[]{
					(Math.random()*5*Math.pow(-1,i)),
					(Math.random()*5*Math.pow(-1,i)),
					(Math.random()*5*Math.pow(-1,i))}
			));
		}
		KDTree<TreePoint> tree = new KDTree<TreePoint> (data,3,4,new double[]{10,10,10});
		
		data = tree.getNearPoints(new testClass(new double[]{-2.5,-2.5,-2.5}), new double[]{1,2,3});

		tree.printTree();
		
		System.out.println("After looking for points within (1,2,3) of (-2.5,-2.5,-2.5), results:");
		for(TreePoint tp:data) {
			System.out.println("Point: " + tp);
		}
		
	}
	
	private static class testClass implements TreePoint {
		double[] data;
		
		public testClass(double[] dat) {
			data = dat;
		}
		
		@Override
		public double getVal(int val) {
			return data[val];
		}
		
		@Override
		public String toString() {
			return  data[0] + " " + data[1] + " " + data[2];
		}
		
	}
	
	
	// ------------------------------------------------
	// END TESTING
	// ------------------------------------------------	
	public KDTree(int numAxes, int elemsPerNode) {
		numElemsPerNode = elemsPerNode;
		axes = numAxes;
		root = new KDNode<T>(elemsPerNode,true);
		spread = new double[axes];
		for(int i = 0; i < axes; i++) {
			spread[i] = 1.0;
		}
	}


	public KDTree(List<T> input, int numAxes, int elemsPerNode, double[] spreads) {
		numElemsPerNode = elemsPerNode;
		axes = numAxes;
		points = new ArrayList<T>(input);
		spread = Arrays.copyOf(spreads,spreads.length);
		root = buildNodes(points,0,points.size());
		built = true;
	}

	private KDNode<T> buildNodes(List<T> data, int start, int end) {
		if ((end-start-1) < numElemsPerNode) {
			return new KDNode<T>(numElemsPerNode,data.subList(start,end));
		}

		double[] minMax = new double[axes*2];
		T tmp = data.get(0);
		for(int i = 0; i < axes; i++) {
			minMax[i*2] = tmp.getVal(i);
			minMax[i*2+1] = tmp.getVal(i);
		}

		for(int i = start;i < end; i++) {
			T p = data.get(i); 
			for(int j = 0; j < axes; j++) {
				if (minMax[j*2] > p.getVal(j))	minMax[j*2] = p.getVal(j);
				if (minMax[j*2+1] < p.getVal(j))	minMax[j*2+1] = p.getVal(j);
			}
		}

		double scale = (minMax[1] - minMax[0]) / spread[0];
		int axis = 0;
		for(int i = 1; i < axes ; i++) {
			double tmpSpread = (minMax[i*2+1] - minMax[i*2]) / spread[i];
			if (tmpSpread > scale) {
				scale = tmpSpread;
				axis = i;
			}
		}
		int median = quickMedian(data,start,end,axis,(start+end)/2);
		double split = data.get(median).getVal(axis);

		KDNode<T> node = new KDNode<T>(numElemsPerNode,axis,split);	
		node.left = buildNodes(data,start,median+1);
		node.right = buildNodes(data,median+1,end);	

		return node;	
	}

	private int quickMedian(List<T> data, int start, int end, int axis,int indexWanted) {
		if (end - start <= 1) {
			return start;
		}
				
		int mid=(start+end)/2;
		T pivot = data.get(mid);
		data = swapElems(data,data.indexOf(pivot),end-1);
		
		int index = start;
		for(int i = start; i < end-1; i++) {
			if (data.get(i).getVal(axis) <= pivot.getVal(axis)) {
				data = swapElems(data,i,index);
				index+=1;
			}
		}
		data = swapElems(data,index,end-1);
		
		if(index==indexWanted) return index;
		if (index < indexWanted) {
			return quickMedian(data,index+1,end,axis,indexWanted);
		}
		return quickMedian(data,start,index,axis,indexWanted);
	}

	private List<T> swapElems(List<T> data, int one, int two) {
		T tmp1 = data.get(one);
		T tmp2 = data.get(two);
		data.set(one,tmp2);
		data.set(two,tmp1);
		return data;
	}

	public void addPoint(T t) {
		if (built) return;
		KDNode<T> rover = root;
		
		while(rover.left != null) {
			if (t.getVal(rover.axis) <= rover.split) {
				rover = rover.left;
			} else {
				rover = rover.right;
			}
		}
		if (rover.data.size()+1 >= rover.totalPoints) {
			splitNode(rover);
			if (t.getVal(rover.axis) <= rover.split) {
				rover.left.data.add(t);
			} else {
				rover.right.data.add(t);
			}
		} else {
			rover.data.add(t);
		}
	}
	
	private void splitNode(KDNode<T> toSplit) {
		if (toSplit.data.size() < toSplit.totalPoints) return;
		double[] minMax = new double[axes*2];
		T tmp = toSplit.data.get(0);
		for(int i = 0; i < axes; i++) {
			minMax[i*2] = tmp.getVal(i);
			minMax[i*2+1] = tmp.getVal(i);
		}

		for(T p:toSplit.data) {
			for(int j = 0; j < axes; j++) {
				if (minMax[j*2] > p.getVal(j))	minMax[j*2] = p.getVal(j);
				if (minMax[j*2+1] < p.getVal(j))	minMax[j*2+1] = p.getVal(j);
			}
		}

		double scale = (minMax[1] - minMax[0]) / spread[0];
		int axis = 0;
		for(int i = 1; i < axes ; i++) {
			double tmpSpread = (minMax[i*2+1] - minMax[i*2]) / spread[i];
			if (tmpSpread > scale) {
				scale = tmpSpread;
				axis = i;
			}
		}
		int median = quickMedian(toSplit.data,0,toSplit.data.size(),axis,toSplit.data.size()/2);
		double split = toSplit.data.get(median).getVal(axis);

		toSplit = new KDNode<T>(toSplit.totalPoints,axis,split);
		toSplit.left = new KDNode<T>(toSplit.totalPoints,toSplit.data.subList(0,median+1));
		toSplit.right = new KDNode<T>(toSplit.totalPoints,toSplit.data.subList(median+1,toSplit.data.size()));
	}

	public List<T> getNearPoints(T basePoint, double[] range) {
		if (range.length < axes) return null;
		
        List<T> ret = new ArrayList<T>();
		getNearPoints(basePoint,range,root,ret);
		return ret;
	}
	
	private KDNode<T> findTopNode(T basePoint, double[] range,KDNode<T> curNode) {
		if (curNode.isLeaf) return curNode;
		int axis = curNode.axis;
		
		if (basePoint.getVal(axis) <= curNode.split && (basePoint.getVal(axis) + range[axis]) > curNode.split  )
			return curNode;
		if (basePoint.getVal(axis) >= curNode.split && (basePoint.getVal(axis) - range[axis]) < curNode.split  )
			return curNode;
		
		if (basePoint.getVal(axis) <= curNode.split)
			return findTopNode(basePoint,range,curNode.left);
		
		return findTopNode(basePoint,range,curNode.right);
	}
	
	private void getNearPoints(T point, double[] constraints, KDNode<T> node,List<T> ret) {
		if (node.isLeaf) {
			for(T tp:node.data) {
				if (checkPoint(point,tp,constraints,0))
					ret.add(tp);
			}
		} else {
			int axis = node.axis;
			if (node.left.isLeaf) {
				getNearPoints(point,constraints,node.left,ret);
				getNearPoints(point,constraints,node.right,ret);
				return;
			}
			if (point.getVal(axis) <= node.split && (point.getVal(axis) + constraints[axis]) > node.split ||
				point.getVal(axis) > node.split && (point.getVal(axis) - constraints[axis]) <= node.split
					) {
				getNearPoints(point,constraints,node.left,ret);
				getNearPoints(point,constraints,node.right,ret);
			} 
			else if (point.getVal(axis) <= node.split && (point.getVal(axis) + constraints[axis]) <= node.split) {
				getNearPoints(point,constraints,node.left,ret);
			} else {
				getNearPoints(point,constraints,node.right,ret);
			}
		}
	}
	
	private boolean checkPoint(T basePoint, T toCheck, double[] constraints, int axis) {
		if (axis >= axes) return true;
		
		if (toCheck.getVal(axis) > basePoint.getVal(axis)) {
			if (basePoint.getVal(axis) + constraints[axis] < toCheck.getVal(axis))
				return false;
		}
		if (toCheck.getVal(axis) <= basePoint.getVal(axis)) {
			if (basePoint.getVal(axis) - constraints[axis] > toCheck.getVal(axis))
				return false;
		}
		

		return checkPoint(basePoint,toCheck,constraints,axis+1);
	}
	
	
	public void printTree() {
		printRecur(root,"","");
	}
	
	private void printRecur(KDNode<T> toPrint, String leafSide, String infoTab) {
		if (toPrint.isLeaf) {
			System.out.println(infoTab + "+----------------------------+");
			System.out.printf(infoTab + "|       %s LEAF NODE      |\n",leafSide);
			System.out.println(infoTab + "+----------------------------+");
			for(T t:toPrint.data) {
				System.out.println(infoTab + "| Item " + t);
				System.out.println(infoTab + "+----------------------------+");
			}
		} else {
			System.out.println(infoTab + "+------------+----------+--------------------------+");
		 	System.out.printf(infoTab +  "| SPLIT NODE | AXIS: %d  | VALUE: %5.10f     |\n",toPrint.axis,toPrint.split);
			System.out.println(infoTab + "+------------+----------+--------------------------+");
			if (toPrint.left != null) {
				printRecur(toPrint.left," LEFT", infoTab + "\t");
				printRecur(toPrint.right,"RIGHT", infoTab + "\t");
			}
		}
	}
	
	public static interface TreePoint {
		public double getVal(int val);
	}

	private class KDNode<S> {
		public final int totalPoints;
		public KDNode<S> left,right;
		public List<S> data;
		public boolean isLeaf;
		public double split;
		public int axis;

		public KDNode(int points, boolean leaf) {
			totalPoints = points;
			data = new ArrayList<S>(totalPoints);
			isLeaf = leaf;
		}

		public KDNode(int points,List<S> newData) {
			this(points,true);
			data = newData;
		}

		public KDNode(int points, int ax, double splitValue) {
			this(points,false);
			split = splitValue;
			axis = ax;
		}

		@Override
		public String toString() {
			if (isLeaf)
				return "Points (cur / tot): " + data.size() + " / " + totalPoints;
			else
				return "Parent Node split on axis " + axis + " and value " + split; 
		}
	}
}
