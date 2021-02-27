/*
 * Created on 26.04.2005 for PIROL
 *
 * CVS header information:
 *  $RCSfile: ColorGenerator.java,v $
 *  $Revision: 1.19 $
 *  $Date: 2005/11/24 14:26:16 $
 *  $Source: D:/CVS/cvsrepo/pirolPlugIns/utilities/colors/ColorGenerator.java,v $
 */
package org.openjump.core.ui.color;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openjump.core.apitools.CollectionsTools;


/**
 * Class to generate a given number of color steps to
 * cross fade from color A to color C (and optionally
 * pass color B)
 * 
 * @author Ole Rahn, Stefan Ostermann
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Revision: 1.19 $
 * 
 */
public class ColorGenerator {
    
    protected List inputColors = new ArrayList();
    protected Color[] colorArray = null;
    protected int steps = 0;
   
    
    //protected PersonalLogger logger = new PersonalLogger(DebugUserIds.USER_Ole);
    
    public static ColorGenerator getTrafficLightColors(int steps){
        if (steps >= 3)
            return new ColorGenerator(steps, Color.GREEN.darker(), Color.YELLOW.brighter(), Color.RED.darker());
        
        return ColorGenerator.getGreenToRedColors(steps);
    }
    
    public static ColorGenerator getGreenToRedColors(int steps){
        return new ColorGenerator(steps, Color.GREEN.darker(), Color.RED.darker());
    }
    
    public static ColorGenerator getRedToGreenColors(int steps){
        return new ColorGenerator(steps, Color.RED.darker(), Color.GREEN.darker());
    }
    
    public static ColorGenerator getReverseTrafficLightColors(int steps){
        if (steps >= 3)
            return new ColorGenerator(steps, Color.RED.darker(), Color.YELLOW.brighter(), Color.GREEN.darker());
        
        return ColorGenerator.getRedToGreenColors(steps);
    }
    
    public static ColorGenerator getBlueToRedColors(int steps){
        return new ColorGenerator(steps, Color.BLUE, Color.RED);
    }
    
    /**
     * For java2xml which needs an empty constructor.
     *
     */
    public ColorGenerator() {
    	
    }
    
    /**
     * this constructor is untested, yet
     * (but there is no predictable reason, why it should not work...)
     * @param steps number of colors wanted
     * @param colors List containing java.awt.Color objects
     */
    public ColorGenerator(int steps, List colors) {
        super();
        
        this.steps = steps - 1;
        this.inputColors.addAll(colors);
        
        this.fillColorArray();
    }
    
    /**
     * this constructor is untested, yet
     * (but there is no predictable reason, why it should not work...)
     * @param steps number of colors wanted
     * @param colors array containing java.awt.Color objects
     */
    public ColorGenerator(int steps, Color[] colors) {
        super();
        
        this.steps = steps - 1;
        CollectionsTools.addArrayToList( this.inputColors, colors );
        
        this.fillColorArray();
    }
    
    public ColorGenerator(int steps, Color A, Color C) {
        super();
        
        this.steps = steps - 1;
        this.inputColors.add( A );
        this.inputColors.add( C );
        
        this.fillColorArray();
    }
    
    public ColorGenerator(int steps, Color A, Color B, Color C) {
        super();

        this.steps = steps -1;
        this.inputColors.add( A );
        this.inputColors.add( B );
        this.inputColors.add( C );
        
        this.fillColorArray();
    }
    
    /**
     *@return All generated colors in an object-array
     */
    public Color[] getColorArray() {
        return colorArray;
    }
    /**
     *@return the given number of color steps
     */
    public int getSteps() {
        return steps + 1;
    }
    
    /**
     *@param steps the number of color steps
     */
    public void setSteps(int steps) {
        this.steps = steps - 1;
        this.fillColorArray();
    }
    
    /**
     * Returns the nr-th color, generated by this class
     *@param nr zero-based index of the color wanted
     *@return Color or null if the given index is out-of-bounds
     */
    public Color getColor(int nr){
        if (nr < 0 || nr >= this.colorArray.length )
            return null;
        
        //logger.printDebug("color " + nr  + ": " + this.colorArray[nr]);
        return this.colorArray[nr];
    }
    
    /**
     * Sets the nr-th color, originally generated by this class
     *@param nr zero-based index of the color wanted
     */
    protected void setColor(int nr, Color color){
        if (nr < 0 || nr >= this.colorArray.length )
            return ;
        
        //logger.printDebug("color " + nr  + ": " + this.colorArray[nr]);
        this.colorArray[nr] = color;
    }
    
    /**
     * method to actually generate the colors
     */
    protected void fillColorArray(){
        ArrayList colors = new ArrayList();
        
        if (this.getSteps() > this.inputColors.size()){
        
            int r = 0, g = 0, b = 0;
            int rTarget = 0, gTarget = 0, bTarget = 0;
            double rStep = 0, gStep = 0, bStep = 0;
            double stepsToSwitch = (double)this.steps / (this.inputColors.size()-1);
    
            int currentBaseColor = 0;
            
            Color baseColor = (Color)this.inputColors.get(0);
            Color nextColor = (Color)this.inputColors.get(1);
            
            boolean switchR, switchG, switchB;
            
            switchR = false;
            switchG = false;
            switchB = false;
    
            r = baseColor.getRed();
            g = baseColor.getGreen();
            b = baseColor.getBlue();
            
            rStep = Math.ceil((double)(nextColor.getRed()-baseColor.getRed()) / Math.round(stepsToSwitch));
            gStep = Math.ceil((double)(nextColor.getGreen()-baseColor.getGreen()) / Math.round(stepsToSwitch));
            bStep = Math.ceil((double)(nextColor.getBlue()-baseColor.getBlue()) / Math.round(stepsToSwitch));
            
            rTarget = nextColor.getRed();
            gTarget = nextColor.getGreen();
            bTarget = nextColor.getBlue();
            
            colors.add(baseColor);
            
            for (int i=0; i<this.steps; i++){
                
                if (  (r + rStep >= rTarget && rStep >= 0) || (r + rStep <= rTarget && rStep < 0)){
                    
                    if (nextColor!=null){
                        switchR = true;
                    } 
                }
                if (  (g + gStep >= gTarget && gStep >= 0) || (g + gStep <= gTarget && gStep < 0)){
                    
                    if (nextColor!=null){
                        switchG = true;
                    } 
                }
                if (  (b + bStep >= bTarget && bStep >= 0) || (b + bStep <= bTarget && bStep < 0)){
                    
                    if (nextColor!=null){
                        switchB = true;
                    } 
                }
                
                if (switchR && switchG && switchB){
                    
                    // (this.inputColors.size() -1) - (currentBaseColor + 1) --> this.inputColors.size() - currentBaseColor
                    stepsToSwitch = (double)(this.steps - currentBaseColor) / Math.max(this.inputColors.size() - currentBaseColor, 1);
                    
                    switchR = false;
                    switchG = false;
                    switchB = false;
                    
                    rStep = (nextColor.getRed()-baseColor.getRed()) / Math.round(stepsToSwitch);
                    rTarget = nextColor.getRed();
                    
                    gStep = (nextColor.getGreen()-baseColor.getGreen()) / Math.round(stepsToSwitch);
                    gTarget = nextColor.getGreen();
                    
                    bStep = (nextColor.getBlue()-baseColor.getBlue()) / Math.round(stepsToSwitch);
                    bTarget = nextColor.getBlue();
                    
                    
                    currentBaseColor ++;
                    if (currentBaseColor < this.inputColors.size()){
                        baseColor = (Color)this.inputColors.get(currentBaseColor);
                    }
                    if (currentBaseColor < this.inputColors.size()-1){
                        nextColor = (Color)this.inputColors.get(currentBaseColor+1);
                    } else {
                        nextColor = null;
                    }
                }
    
                r += (int)Math.round(rStep);
                r = Math.max(Math.min(r, 255), 0);
                
                g += (int)Math.round(gStep);
                g = Math.max(Math.min(g, 255), 0);
                
                b += (int)Math.round(bStep);
                b = Math.max(Math.min(b, 255), 0);
                
                colors.add(new Color(r,g,b));
            }
        } else {
            for (int i=0; i<this.getSteps(); i++){
                colors.add(this.inputColors.get(i));
            }
        }

        this.colorArray = (Color[])colors.toArray(new Color[0]);
        
    }
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "ColorGenerator";
	}
	
    public Color[] getInputColorsAsArray(){
        return (Color[])this.inputColors.toArray(new Color[0]);
    }
	
	
	/**For java2xml
	 * @return Returns the inputColors.
	 */
	public Collection getXMLInputColors() {
		return inputColors;
	}
    
    /**For java2xml
     * 
     * @param color the color to add
     */
    public void addXMLInputColor(Color color) {
    	this.inputColors.add(color);
    }
	
    /**
     * For java2xml
     * 
     * @return the number of steps to cross fade from color A to color C
     */
	public int getStepsXML() {
		return steps;
	}
	
	/**For java2xml
	 * 
	 * @param steps number of steps
	 */
	public void setStepsXML(int steps) {
		this.steps = steps;
		this.fillColorArray();
	}
	
}
