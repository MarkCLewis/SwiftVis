/*
 * Created on Aug 6, 2009
 */
package edu.swri.swiftvis.util;

public class DisjointSets {
    public DisjointSets(int numSets) {
        parent=new int[numSets];
        rank=new int[numSets];
        for(int i=0; i<parent.length; ++i) {
            parent[i]=i;
            rank[i]=0;
        }
    }
    public int findSet(int n) {
        if(n!=parent[n]) {
            parent[n]=findSet(parent[n]);
        }
        return parent[n];
    }
    
    public void union(int m,int n) {
        n=findSet(n);
        m=findSet(m);
        if(rank[n]>rank[m]) {
            parent[m]=n;
        } else {
            parent[n]=m;
            if(rank[n]==rank[m]) {
                rank[m]++;
            }
        }
    }
    
    private int[] parent;
    private int[] rank;
}
