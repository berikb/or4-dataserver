package kz.tamur.or4.data;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

public class InterfaceManager {
	
	private static InterfaceManager instance;

	private final String defaultSchema;
	private final Path confDir;
	
	private final Map<String, InterfaceConfig> configs = new HashMap<>();
	private final Map<String, String> configIdByFileName = new HashMap<>();

	public static synchronized InterfaceManager getInstance(ServletContext sc) throws Exception {
		if (instance == null) {
			instance = new InterfaceManager(
					sc.getInitParameter("kz.tamur.or4.data.DefaultSchema"),
					sc.getInitParameter("kz.tamur.or4.data.ConfDir"));
		}
		return instance;
	}
	
	public InterfaceManager(String defaultSchema, final String confDirName) throws Exception {
		this.defaultSchema = defaultSchema;
		this.confDir = Paths.get(confDirName);

		// Первоначальная загрузка конфигураций
		try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(confDir, "*.xml")) {
            for (Path path : dirStream) {
            	try {
            		addConfig(path.toFile());
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            }
		}

		//new Thread(new ConfigWatcher()).start();
	}
	
	public File getConfigDir() {
		return confDir.toFile();
	}
	
	public String getDefaultSchema() {
		return defaultSchema;
	}

	public InterfaceConfig getConfig(String interfaceId) {
		return configs.get(interfaceId);
	}
	
	public Collection<InterfaceConfig> getAllConfigs() {
		return configs.values();
	}

	public void addConfig(File configFile) throws Exception {
		removeConfig(configFile);
		InterfaceConfig config = new InterfaceConfig(configFile, this);
		String configId = config.getId();
		configs.put(configId, config);
		configIdByFileName.put(configFile.getAbsolutePath(), configId);
	}
	
	public void removeConfig(File configFile) throws Exception {
		String fileName = configFile.getAbsolutePath();
		String configId = configIdByFileName.get(fileName);
		if (configId != null) {
			configs.remove(configId);
			configIdByFileName.remove(fileName);
		}
	}

	private class ConfigWatcher implements Runnable {
		
		private WatchService watcher;
		
		public ConfigWatcher() throws Exception {
			watcher = FileSystems.getDefault().newWatchService();
			confDir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
		}
		
		@Override
		public void run() {
			try {
				PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.xml");
				while (true) {
					WatchKey key = watcher.take();
					for (WatchEvent<?> event: key.pollEvents()) {
				        WatchEvent.Kind<?> kind = event.kind();

				        if (kind == OVERFLOW) {
				            continue;
				        }
				        @SuppressWarnings("unchecked")
				        WatchEvent<Path> ev = (WatchEvent<Path>)event;
				        Path filename = ev.context();
				        if (matcher.matches(filename)) {
					        Path file = confDir.resolve(filename);

					        if (kind == ENTRY_MODIFY || kind == ENTRY_CREATE) {
					        	addConfig(file.toFile());
					        
					        } else if (kind == ENTRY_DELETE) {
					        	removeConfig(file.toFile());
					        }
				        }
					}
					boolean valid = key.reset();
				    if (!valid) {
				        break;
				    }
				}				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
