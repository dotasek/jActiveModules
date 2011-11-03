package csplugins.jActiveModules;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkFactory;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.creation.LoadVisualStyles;
//import org.cytoscape.task.creation.LoadVisualStylesFromFileFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskManager;

public class ServicesUtil {

	
	public static CySwingApplication cySwingApplicationServiceRef;
	
	public static CyApplicationManager cyApplicationManagerServiceRef;
	
	public static CyNetworkViewManager cyNetworkViewManagerServiceRef;
	public static CyNetworkManager cyNetworkManagerServiceRef;
	public static CyServiceRegistrar cyServiceRegistrarServiceRef;
	public static CyEventHelper cyEventHelperServiceRef;
	public static TaskManager taskManagerServiceRef;
	
	public static CyProperty<Properties> cytoscapePropertiesServiceRef;
	public static VisualMappingManager visualMappingManagerRef;
	public static CyNetworkFactory cyNetworkFactoryServiceRef;

	public static CyRootNetworkFactory cyRootNetworkFactory;
	public static CyNetworkViewFactory cyNetworkViewFactoryServiceRef;
	public static CyLayoutAlgorithmManager cyLayoutsServiceRef;

	public static LoadVisualStyles loadVizmapFileTaskFactory;
	
}