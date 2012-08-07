package org.apxeolog.salem.config;

import haven.Resource;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apxeolog.salem.ALS;
import org.apxeolog.salem.HXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * I want to add some pretty nice things from OOP here ^_^
 * @author APXEOLOG
 *
 */
public class XMLConfigProvider {
	private static Class<?>[] initalizer = { MinimapHighlightConfig.class, XConfig.class, UIConfig.class };

	static {
		registeredConfigTypes = new HashMap<String, IConfigFactory>();
		loadedConfigs = new HashMap<String, IConfigExport>();
		try {
			// Init all classes to handle them properly
			for (Class<?> cl : initalizer)
				Class.forName(cl.getName(), true, cl.getClassLoader());
		} catch (ClassNotFoundException e) {
			// Whoops O_o
		}
	}

	private static HashMap<String, IConfigFactory> registeredConfigTypes;
	private static HashMap<String, IConfigExport> loadedConfigs;

	public static void registerConfig(String name, IConfigFactory factory) {
		registeredConfigTypes.put(name, factory);
	}

	public static IConfigExport getConfig(String name) {
		IConfigExport config = loadedConfigs.get(name);
		return config;
	}

	private static final int SAVE_TIMEOUT = 5000;
	private static long firstTimeSaveWasRequested = -1;

	public static void save() {
		long currentTime = System.currentTimeMillis();
		if (firstTimeSaveWasRequested > 0 && currentTime - firstTimeSaveWasRequested > SAVE_TIMEOUT) {
			save(true);
			firstTimeSaveWasRequested = -1;
		} else {
			firstTimeSaveWasRequested = currentTime;
		}
	}

	public static void save(boolean ignoreTiming) {
		if (ignoreTiming) {
			ALS.alDebugPrint("save", System.currentTimeMillis());
			saveConfigToFile(new File("salem.xml"));
		}
	}

	public static void load() {
		loadConfigFromFile(new File("salem.xml"));
	}

	public static void init() {
		for (Entry<String, IConfigExport> entry : loadedConfigs.entrySet()) {
			try {
				entry.getValue().init(null);
			} catch (Exception ex) {
				ALS.alError("XMLConfigProvider: [ERROR_INIT_CONFIG]", ex);
				// Try init from default
				Element rootElement = getDefaultSection(entry.getKey());
				if (rootElement != null) entry.getValue().init(rootElement);
			}
		}
	}

	private static void loadConfig(String name, Element root) {
		IConfigFactory factory = registeredConfigTypes.get(name);
		if (factory != null) {
			loadedConfigs.put(name, factory.create(root));
		}
	}

	private static Element getDefaultSection(String name) {
		try {
			Document loadedDocument = HXml.readXMLFile(Resource.class.getResourceAsStream("/res/config_default.xml"));
			NodeList list = loadedDocument.getElementsByTagName(name);
			if (list.getLength() > 0) return (Element) list.item(0);
			else {
				ALS.alError("XMLConfigProvider: [ERROR_NO_SECTION]", name);
				return null;
			}
		} catch (Exception e) {
			ALS.alError("XMLConfigProvider: [ERROR_DEFAULT_READ]", e);
			return null;
		}
	}

	private static void loadConfigFromFile(File file) {
		Document loadedDocument = HXml.readXMLFile(file);
		if (loadedDocument == null) return;
		try {
			Element rootElement = (Element) loadedDocument.getElementsByTagName("config").item(0);
			for (Entry<String, IConfigFactory> exporter : registeredConfigTypes.entrySet()) {
				try {
					NodeList list = rootElement.getElementsByTagName(exporter.getKey());
					if (list.getLength() > 0) loadConfig(exporter.getKey(), (Element) list.item(0));
					else loadConfig(exporter.getKey(), null);
				} catch (Exception ex) {
					ALS.alError("XMLConfigProvider: [ERROR_LOAD_CONFIG]", ex);
				}
			}
		} catch (Exception ex) {
			ALS.alError("XMLConfigProvider: [ERROR_LOAD_CONFIGS]", ex);
		}
	}

	private static void saveConfigToFile(File file) {
		Document saveDocument = HXml.newDoc();
		Element rootElement = (Element) saveDocument.appendChild(saveDocument.createElement("config"));
		Element bufElem = null;
		for (Entry<String, IConfigExport> exporter : loadedConfigs.entrySet()) {
			try {
				bufElem = (Element) rootElement.appendChild(saveDocument.createElement(exporter.getKey()));
				exporter.getValue().addElement(bufElem, saveDocument);
			} catch (Exception ex) {
				ALS.alError("XMLConfigProvider: [ERROR_SAVE_CONFIG]", ex);
			}
		}
		HXml.saveXML(saveDocument, file);
	}
}