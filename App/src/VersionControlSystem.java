import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class VersionControlSystem {
    private final String currentDirectory;
    private final String vcsDirectory;
    private final String head;
    private final String index;
    private final String objects;
    private final String separator;
    private static final String[] subDirectories = {"Objects"};
    private static final String[] files = {"HEAD", "Index"};
    public VersionControlSystem(String currentDirectory) {
        this.currentDirectory = currentDirectory;
        this.separator = System.getProperty("file.separator");
        this.vcsDirectory = pathBuilder(new String[]{".vcs"}, currentDirectory, separator);
        this.head = pathBuilder(new String[]{"HEAD"}, vcsDirectory, separator);
        this.index = pathBuilder(new String[]{"Index"}, vcsDirectory, separator);
        this.objects = pathBuilder(new String[]{"Objects"}, vcsDirectory, separator);
    }
    public VersionControlSystem(String currentDirectory, String vcsDirectory, String head, String index, String objects) {
        this.currentDirectory = currentDirectory;
        this.vcsDirectory = vcsDirectory;
        this.head = head;
        this.index = index;
        this.objects = objects;
        this.separator = System.getProperty("file.separator");
    }
    /**
     * The init command, creates a .vcs folder and subfolders to initialize version control system.
     * If a .vcs folder already exists, do nothing
     */
    public static VersionControlSystem init(String dir) {
        String separator = System.getProperty("file.separator");
        File vcs = new File(pathBuilder(new String[] {".vcs"}, dir, separator));
        if (!Files.exists(Paths.get(dir))) {
            System.out.println("Directory doesn't exist");
        } else if (vcs.exists()) {
            System.out.println("Version Control System already exists");
        } else {
            if (vcs.mkdir()) {
                Map<String, String> sub = new HashMap<>();
                Path path = vcs.toPath();
                try {
                    Files.setAttribute(path, "dos:hidden", true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String vcsDirectory = path.toString();
                for (String subDirectory : subDirectories) {
                    File subfolder = new File(vcsDirectory, subDirectory);
                    if (!subfolder.mkdir()) {
                        vcs.delete();
                        System.out.println("Failed to create " + pathBuilder(new String[] {subDirectory}, vcsDirectory, separator));
                        return null;
                    } else {
                        sub.put(subDirectory, subfolder.toPath().toAbsolutePath().toString());
                    }
                }
                for (String f : files) {
                    File file = new File(vcsDirectory, f);
                    try {
                        if (!file.createNewFile()) {
                            vcs.delete();
                            System.out.println("Failed to create " + pathBuilder(new String[] {f}, vcsDirectory, separator));
                            return null;
                        } else {
                            sub.put(f, file.toPath().toAbsolutePath().toString());
                        }
                    } catch (Exception e) {
                        vcs.delete();
                        System.out.println("Failed to create " + pathBuilder(new String[] {f}, vcsDirectory, separator));
                        System.out.println(e.getMessage());
                        return null;
                    }
                }
                return new VersionControlSystem(dir, vcsDirectory, sub.get("HEAD"), sub.get("Index"), sub.get("Objects"));
            } else {
                System.out.println("Unable to create " + vcs.toPath());
            }
        }
        return null;
    }
    public void add(String path, int status) {
        try {
            File head = new File(this.head);
            File index = new File(this.index);
            File file = new File(path);
            String name = file.getName();
            String hash = hash(path);
            BufferedReader br = new BufferedReader(new FileReader(head));
            StringBuilder sb = new StringBuilder();
            boolean exists = false;
            String line;
            if (br.readLine() != null) {
                return;
            }
            br = new BufferedReader(new FileReader(index));
            while ((line = br.readLine()) != null) {
                if (line.length() == name.length() + 43 && line.startsWith(name)) {  // space between name and hash, 40 character hash, space between hash and status, and 1 number status
                    sb.append(String.format("%s %s %d", name, hash, status));
                    exists = true;
                } else {
                    sb.append(line);
                }
                sb.append("\n");
            }
            if (!exists) {
                sb.append(String.format("%s %s %d", name, hash, status));
            }
            createFile(path, hash);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(index))) {
                bw.write(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Returns a path to a file or directory given how to get there from currentDirectory
     * @param paths: how to get to the desired destination, in the form of an array of strings
     * @return the desired path
     */
    private static String pathBuilder(String[] paths, String start, String separator) {
        StringBuilder output = new StringBuilder();
        output.append(start);
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
        try (FileInputStream fis = new FileInputStream(path)) {
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

    /**
     * Checks if a file with the hash as the name is already saved
     * @param hash: the hash
     * @return boolean if the file is already saved.
     */
    public boolean hashExists(String hash) {
        return Files.exists(Paths.get(pathBuilder(new String[] {subDirectories[0], hash.substring(0, 2), hash.substring(2)}, this.vcsDirectory, separator)));
    }

    /**
     * Creates the bin of the first two characters of the hash if that doesn't exist, and copies the file to the bin
     * @param path: path to the file to be copied
     * @param hash: the hashcode of the file, for naming and bin assignment purposes
     * @return boolean whether the creation was successful or not
     */
    public boolean createFile(String path, String hash) {
        if (hashExists(hash)) {
            return true;
        } else {
            Path source = Paths.get(path);
            Path target = Paths.get(pathBuilder(new String[] {subDirectories[0], hash.substring(0, 2), hash.substring(2)}, vcsDirectory, separator));
            File bin = new File(pathBuilder(new String[] {subDirectories[0], hash.substring(0, 2)}, vcsDirectory, separator));
            if (!bin.exists()) {
                if (!bin.mkdir()) {
                    return false;
                }
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

