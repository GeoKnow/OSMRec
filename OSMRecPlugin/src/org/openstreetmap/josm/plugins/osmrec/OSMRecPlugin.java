
package org.openstreetmap.josm.plugins.osmrec;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;


/** 
 * Plugin entry point class.
 * 
 * @author imis-nkarag
 */

public class OSMRecPlugin extends Plugin{
    
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isLinux = false;
    private final MenuExportAction menuExportAction;
    private static MapFrame mapFrame;
    public OSMRecPlugin plugin;
    
    
    public OSMRecPlugin(PluginInformation info) {       
        super(info);
        defineOS();
        System.out.println(getPluginDir());
        menuExportAction = new MenuExportAction();
        Main.main.menu.toolsMenu.add(menuExportAction);
    }
   
    /**
     * Called when the JOSM map frame is created or destroyed.
     * @param oldFrame
     * @param newFrame
     */
    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {             
        if (oldFrame == null && newFrame != null) { // map frame added
            System.out.println("mapFrameInitialized");
            setCurrentMapFrame(newFrame);
            setState(this);
        }
    }
    
    private void setCurrentMapFrame(MapFrame newFrame){
        OSMRecPlugin.mapFrame = newFrame;
    }
    
    public static MapFrame getCurrentMapFrame() {
        return mapFrame;
    }
    
    private void setState(OSMRecPlugin plugin){
        this.plugin = plugin;
    }
    
    public OSMRecPlugin getState(){
        return plugin;
    }
    
    private static void defineOS(){
        if(OS.contains("nux")){
            isLinux = true;
        }
        else if(OS.contains("win")){
            isLinux = false;
        }
        else{            
            System.out.println("OS not supported");
            System.exit(0);
        }
    }
    
    public static boolean isLinux(){
        return isLinux;
    }
    
//    @Override
//    public PreferenceSetting getPreferenceSetting() {
//        return new PreferenceEditor();
//    }
}
