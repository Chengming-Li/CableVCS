import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.nio.file.StandardCopyOption;

public class VersionControlSystem {
    private String currentDirectory;
    private String vcsDirectory;
    private final String separator;
    private final String[] subDirectories = {"Objects"};
    private final String[] files = {"HEAD", "Index"};
    public VersionControlSystem() {
        currentDirectory = System.getProperty("user.dir");
        vcsDirectory = null;
        separator = System.getProperty("file.separator");
    }
    /**
     * Switches directories, updates currentDirectory and vcsDirectory if exists
     * @param dir: path to the new directory
     */
    public void updateDirectory(String dir) {
        if (Files.exists(Paths.get(dir))) {
            this.currentDirectory = dir;
            File vcs = new File(dir + separator + ".vcs");
            if (vcs.exists() && vcs.isDirectory()) {
                this.vcsDirectory = dir + separator + ".vcs";
            } else {
                this.vcsDirectory = null;
            }
        } else {
            this.currentDirectory = null;
            this.vcsDirectory = null;
        }
    }

    /**
     * The init command, creates a .vcs folder and subfolders to initialize version control system.
     * If a .vcs folder already exists, do nothing
     */
    public void init() {
        if (this.currentDirectory != null && Files.exists(Paths.get(currentDirectory))) {
            File vcs = new File(pathBuilder(new String[] {".vcs"}));
            if (vcs.exists()) {
                System.out.println("Version control system already exists");
                return;
            }
            if (vcs.mkdir()) {
                Path path = vcs.toPath();
                try {
                    Files.setAttribute(path, "dos:hidden", true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (String subDirectory : subDirectories) {
                    File subfolder = new File(pathBuilder(new String[] {".vcs"}), subDirectory);
                    if (!subfolder.mkdir()) {
                        vcs.delete();
                        System.out.println("Failed to create " + pathBuilder(new String[] {".vcs", subDirectory}));
                        return;
                    }
                }
                for (String f : files) {
                    File file = new File(pathBuilder(new String[] {".vcs"}), f);
                    try {
                        if (!file.createNewFile()) {
                            vcs.delete();
                            System.out.println("Failed to create " + pathBuilder(new String[] {".vcs", subDirectories[1], f}));
                            return;
                        }
                    } catch (Exception e) {
                        vcs.delete();
                        System.out.println("Failed to create " + pathBuilder(new String[] {".vcs", subDirectories[1], f}));
                        System.out.println(e.getMessage());
                        return;
                    }
                }
            } else {
                System.out.println("Failed to create " + pathBuilder(new String[] {".vcs"}));
            }
        } else {
            System.out.println("Current directory not set");
        }
    }
    /**
     * Returns a path to a file or directory given how to get there from currentDirectory
     * @param paths: how to get to the desired destination, in the form of an array of strings
     * @return the desired path
     */
    private String pathBuilder(String[] paths) {
        StringBuilder output = new StringBuilder();
        output.append(currentDirectory);
        for (String p : paths) {
            output.append(separator);
            output.append(p);
        }
        return output.toString();
    }
    /**
     * Returns the SHA-1 hash for a file
     * @param path: path to the file
     * @return returns the string hash for the file
     */
    public String hash(String path) {
        try (FileInputStream fis = new FileInputStream(path)){
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] dataBytes = new byte[1024];
            int bytesRead = 0;

            while ((bytesRead = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, bytesRead);
            }
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte hashByte : hashBytes) {
                sb.append(Integer.toString((hashByte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (Exception e) {
            System.out.println("Hash failed for " + path + "due to:\n" + e.getMessage());
            return null;
        }
    }
    public boolean hashExists(String hash) {
        return Files.exists(Paths.get(pathBuilder(new String[] {".vcs", subDirectories[0], hash.substring(0, 2), hash.substring(2)})));
    }
    public boolean createFile(String path, String hash) {
        if (hashExists(hash)) {
            return true;
        } else {
            Path source = Paths.get(path);
            Path target = Paths.get(pathBuilder(new String[] {".vcs", subDirectories[0], hash.substring(0, 2), hash.substring(2)}));
            File bin = new File(pathBuilder(new String[] {".vcs", subDirectories[0], hash.substring(0, 2)}));
            if (!bin.exists()) {
                bin.mkdir();
            }
            try {
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}

