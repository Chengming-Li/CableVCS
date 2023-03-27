import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Paths;

class VersionControlSystemTest {
    @org.junit.jupiter.api.Test
    void init() {
        VersionControlSystem vcs = new VersionControlSystem();
        vcs.updateDirectory("C:/Users/malic/Downloads/");
        vcs.init();
        assertTrue(Files.exists(Paths.get("C:/Users/malic/Downloads/.vcs")));
    }
}
