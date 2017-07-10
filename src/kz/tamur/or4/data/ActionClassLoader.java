package kz.tamur.or4.data;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ActionClassLoader extends ClassLoader {

	public ActionClassLoader(ClassLoader parent) {
		super(parent);
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			byte[] data = loadClassFileData(name);
			return defineClass(name, data, 0, data.length);
		} catch (IOException e) {
			throw new ClassNotFoundException(e.getMessage(), e);
		}
	}
	
	private byte[] loadClassFileData(String name) throws IOException {
		String fileName = "D:/EclipseProjects/Or4Workspace/Or4DataBuilder/bin/" + name.replaceAll("\\.", "/") + ".class";
		File file = new File(fileName);
        byte buff[] = new byte[(int)file.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        in.readFully(buff);
        in.close();
        return buff;
    }
}
