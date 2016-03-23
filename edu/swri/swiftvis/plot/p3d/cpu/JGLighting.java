/*
 * Created on Aug 4, 2008
 */
package edu.swri.swiftvis.plot.p3d.cpu;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.plot.p3d.AmbientLight;
import edu.swri.swiftvis.plot.p3d.DirectionalLight;
import edu.swri.swiftvis.plot.p3d.PointLight;
import edu.swri.swiftvis.plot.p3d.Vect3D;

public class JGLighting {
    public void setAmbientLight(AmbientLight al) {
        ambientLight=al;
    }
    
    public void addPointLight(PointLight pl) {
        pointLights.add(pl);
    }
    
    public void addDirectLight(DirectionalLight dl) {
        directLights.add(dl);
    }
    
    public void clearLights() {
        pointLights.clear();
        directLights.clear();
    }
    
    public void setLengthScale(double s) {
        scale=s;
    }
    
    public void fixValues(DataSink sink) {
        for(PointLight pl:pointLights) pl.fixPosition(sink);
        for(DirectionalLight dl:directLights) dl.fixDirection(sink);
    }
    
    public Color getLightColor(JGVector pos,JGVector norm,Color col) {
        float red=0,green=0,blue=0;
        float[] comp=col.getColorComponents(null);
        float[] acomp=ambientLight.getColor().getColorComponents(null);
        red+=comp[0]*acomp[0];
        green+=comp[1]*acomp[1];
        blue+=comp[2]*acomp[2];
        for(PointLight pl:pointLights) {
            Vect3D lpos=pl.getPosition();
            double[] dir={lpos.getX()-pos.element(0),lpos.getY()-pos.element(1),lpos.getZ()-pos.element(2)};
            double len=Math.sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]);
            dir[0]/=len;
            dir[1]/=len;
            dir[2]/=len;
            double mag=(norm.element(0)*dir[0]+norm.element(1)*dir[1]+norm.element(2)*dir[2])*scale/len;
            if(mag>0) {
                float[] dcomp=pl.getColor().getColorComponents(null);
                red+=comp[0]*dcomp[0]*mag;
                green+=comp[1]*dcomp[1]*mag;
                blue+=comp[2]*dcomp[2]*mag;
            }
        }
        for(DirectionalLight dl:directLights) {
            Vect3D dir=dl.getDirection();
            double mag=-(norm.element(0)*dir.getX()+norm.element(1)*dir.getY()+norm.element(2)*dir.getZ());
            if(mag>0) {
                float[] dcomp=dl.getColor().getColorComponents(null);
                red+=comp[0]*dcomp[0]*mag;
                green+=comp[1]*dcomp[1]*mag;
                blue+=comp[2]*dcomp[2]*mag;
            }
        }
        if(red>1) red=1;
        if(green>1) green=1;
        if(blue>1) blue=1;
        return new Color(red,green,blue);
    }
    
    private List<PointLight> pointLights=new ArrayList<PointLight>();
    private List<DirectionalLight> directLights=new ArrayList<DirectionalLight>();
    private AmbientLight ambientLight=new AmbientLight(new Color(20,20,20));
    private double scale=1;
}
