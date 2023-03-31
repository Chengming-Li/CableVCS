import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.util.List;

class VersionControlSystemTest {
    private static final String TESTDIR = "C:\\Users\\malic\\Downloads\\Project\\VersionControlSystem\\Test";
    private static final String VCSDIR = TESTDIR + "\\.vcs";
    @org.junit.jupiter.api.Test
    void init() {
        VersionControlSystem vcs = cleanUp();
        assertTrue(Files.exists(Paths.get(VCSDIR)));
    }
    @org.junit.jupiter.api.Test
    void testCreateFile() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer = new FileWriter(TESTDIR +"\\testText.txt");
            writer.write("This is some nice text, yada yada");
            writer.close();
            String hash = vcs.hash(new File(TESTDIR +"\\testText.txt"));
            assertFalse(vcs.hashExists(hash));
            assertTrue(vcs.createFile(new File(TESTDIR +"\\testText.txt"), hash));
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
            FileWriter writer = new FileWriter(TESTDIR +"\\testText.txt");
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(TESTDIR +"\\testText.txt");
            BufferedReader br = new BufferedReader(new FileReader(VCSDIR +"\\Index"));
            String firstLine = String.format("%s %s 1", "testText.txt", vcs.hash(new File(TESTDIR +"\\testText.txt")));
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
                writer = new FileWriter(TESTDIR +"\\testText" + i +".txt");
                writer.write("This is some nice text, yada" + i);
                writer.close();
                vcs.add(TESTDIR +"\\testText" + i +".txt");
                sb.append("testText").append(i).append(".txt ").append(vcs.hash("This is some nice text, yada" + i)).append("\n");
            }
            assertEquals(vcs.makeTree(), vcs.hash("testText1.txt 22d1403d437e1945b5264c769e3a735431bc1107\ntestText0.txt 79c23f15f7fcfb0667964d8dbeed553179928217\n"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void commitTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer;
            for (int i = 0; i < 5; i++) {
                writer = new FileWriter(TESTDIR +"\\testText" + i +".txt");
                writer.write("This is some nice text, yada" + i);
                writer.close();
                vcs.add(TESTDIR +"\\testText" + i +".txt");
            }
            Path indexPath = Paths.get(VCSDIR + "\\Index");
            Path headPath = Paths.get(VCSDIR + "\\HEAD");
            assertEquals(5, Files.readAllLines(indexPath).size());
            vcs.commit("Test Commit", "User");
            assertEquals(0, Files.readAllLines(indexPath).size());
            assertEquals(Files.readAllLines(headPath).size(), 1);
            File firstCommit = vcs.getHashedFile(Files.readAllLines(headPath).get(0));
            assertTrue(firstCommit.exists());
            List<String> firstCommitContents = Files.readAllLines(firstCommit.toPath());
            writer = new FileWriter(TESTDIR +"\\testText0.txt", false);
            writer.write("This is some nice text, yada dabba doo");
            writer.close();
            vcs.add(TESTDIR +"\\testText0.txt");
            System.out.println(vcs.hash("This is some nice text, yada dabba doo"));
            vcs.commit("Second Commit", "User");
            File secondCommit = vcs.getHashedFile(Files.readAllLines(headPath).get(0));
            List<String> secondCommitContents = Files.readAllLines(secondCommit.toPath());
            System.out.println(firstCommitContents);
            System.out.println(secondCommitContents);
            assertNotEquals(firstCommitContents, secondCommitContents);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    private static VersionControlSystem cleanUp() {
        File[] files = new File(TESTDIR).listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        VersionControlSystem vcs = VersionControlSystem.init(TESTDIR);
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
