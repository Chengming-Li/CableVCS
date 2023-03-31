import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;

class VersionControlSystemTest {
    private static final String testDir = "C:\\Users\\malic\\Downloads\\Project\\VersionControlSystem\\Test";
    @org.junit.jupiter.api.Test
    void init() {
        VersionControlSystem vcs = cleanUp();
        assertTrue(Files.exists(Paths.get(testDir + "\\.vcs")));
    }
    @org.junit.jupiter.api.Test
    void testCreateFile() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer = new FileWriter(testDir +"\\testText.txt");
            writer.write("This is some nice text, yada yada");
            writer.close();
            String hash = vcs.hash(new File(testDir +"\\testText.txt"));
            assertFalse(vcs.hashExists(hash));
            assertTrue(vcs.createFile(new File(testDir +"\\testText.txt"), hash));
            assertTrue(vcs.hashExists(hash));
            assertEquals(hash, vcs.hash("This is some nice text, yada yada"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void addTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer = new FileWriter(testDir +"\\testText.txt");
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(testDir +"\\testText.txt");
            BufferedReader br = new BufferedReader(new FileReader(testDir +"\\.vcs\\Index"));
            String firstLine = String.format("%s %s 1", "testText.txt", vcs.hash(new File(testDir +"\\testText.txt")));
            assertEquals(br.readLine(), firstLine);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void treeTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 2; i++) {
                writer = new FileWriter(testDir +"\\testText" + i +".txt");
                writer.write("This is some nice text, yada" + i);
                writer.close();
                vcs.add(testDir +"\\testText" + i +".txt");
                sb.append("testText").append(i).append(".txt ").append(vcs.hash("This is some nice text, yada" + i)).append("\n");
            }
            assertEquals(vcs.makeTree(), vcs.hash("testText1.txt 22d1403d437e1945b5264c769e3a735431bc1107\ntestText0.txt 79c23f15f7fcfb0667964d8dbeed553179928217\n"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    private static VersionControlSystem cleanUp() {
        File[] files = new File(testDir).listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        VersionControlSystem vcs = VersionControlSystem.init(testDir);
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
