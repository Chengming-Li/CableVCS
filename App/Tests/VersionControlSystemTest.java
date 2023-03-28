import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileWriter;

class VersionControlSystemTest {
    private static final String testDir = "C:/Users/malic/Downloads/Project/VersionControlSystem/Test";
    @org.junit.jupiter.api.Test
    void init() {
        VersionControlSystem vcs = cleanUp();
        assertTrue(Files.exists(Paths.get(testDir + "/.vcs")));
    }
    @org.junit.jupiter.api.Test
    void testCreateFile() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer = new FileWriter(testDir +"/testText.txt");
            writer.write("This is some nice text, yada yada ");
            writer.close();
            String hash = vcs.hash(testDir +"/testText.txt");
            assertFalse(vcs.hashExists(hash));
            assertTrue(vcs.createFile(testDir +"/testText.txt", hash));
            assertTrue(vcs.hashExists(hash));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
    private static VersionControlSystem cleanUp() {
        File[] files = new File(testDir).listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        VersionControlSystem vcs = new VersionControlSystem();
        vcs.updateDirectory(testDir);
        vcs.init();
        return vcs;
    }
    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
