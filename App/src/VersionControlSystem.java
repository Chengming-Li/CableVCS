import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
public class VersionControlSystem {
    private String currentDirectory;
    private String vcsDirectory;
    private final String separator;
    private final String[] subDirectories = {"Objects", "References"};
    private final String[] refFiles = {"HEAD", "ROOT"};
    public VersionControlSystem() {
        currentDirectory = System.getProperty("user.dir");
        vcsDirectory = null;
        separator = System.getProperty("file.separator");
    }

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

    public void init() {
        if (this.currentDirectory != null && Files.exists(Paths.get(currentDirectory))) {
            File vcs = new File(pathBuilder(new String[] {".vcs"}));
            if (vcs.mkdir()) {
                Path path = vcs.toPath();
                try {
                    Files.setAttribute(path, "dos:hidden", true);
                } catch (java.io.IOException e) {
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
                for (String refFile : refFiles) {
                    File file = new File(pathBuilder(new String[] {".vcs", subDirectories[1]}), refFile);
                    try {
                        if (!file.createNewFile()) {
                            vcs.delete();
                            System.out.println("Failed to create " + pathBuilder(new String[] {".vcs", subDirectories[1], refFile}));
                            return;
                        }
                    } catch (java.io.IOException e) {
                        vcs.delete();
                        System.out.println("Failed to create " + pathBuilder(new String[] {".vcs", subDirectories[1], refFile}));
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
    private String pathBuilder(String[] path) {
        StringBuilder output = new StringBuilder();
        output.append(currentDirectory);
        for (String p : path) {
            output.append(separator);
            output.append(p);
        }
        return output.toString();
    }
}
