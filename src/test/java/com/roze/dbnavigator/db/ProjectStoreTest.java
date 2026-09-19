package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectStoreTest {

    @BeforeEach
    public void setUp() {
        // Reset current project to default before each test
        Project def = ProjectStore.getCurrentProject();
        if (def != null && !"default".equals(def.getName())) {
            Project defProj = new Project("default", System.getProperty("user.home") + "/DBNavigatorProjects/default");
            ProjectStore.setCurrentProject(defProj);
        }
    }

    @Test
    public void testProjectModelAttributes() {
        Project p = new Project("MyDataProject", "/home/test/projects/MyDataProject");
        assertEquals("MyDataProject", p.getName());
        assertEquals("/home/test/projects/MyDataProject", p.getPath());
        assertEquals("M", p.getInitialLetter());

        Project emptyProject = new Project("", "");
        assertEquals("D", emptyProject.getInitialLetter());

        Project defaultP = new Project("default", System.getProperty("user.home") + "/DBNavigatorProjects/default");
        assertEquals("D", defaultP.getInitialLetter());
        assertEquals("~/DBNavigatorProjects/default", defaultP.getDisplayPath());
    }

    @Test
    public void testDefaultProjectAlwaysExists() {
        List<Project> all = ProjectStore.load();
        assertFalse(all.isEmpty(), "Projects list must never be empty");
        assertTrue(all.stream().anyMatch(p -> "default".equalsIgnoreCase(p.getName())),
                "Default project must always exist in ProjectStore");

        Project current = ProjectStore.getCurrentProject();
        assertNotNull(current, "Current project must not be null");
    }

    @Test
    public void testCreateOrOpenProject() {
        String testPath = System.getProperty("user.home") + "/Documents/test_analytics";
        Project created = ProjectStore.createOrOpenProject(testPath);

        assertNotNull(created);
        assertEquals("test_analytics", created.getName());
        assertEquals(testPath, created.getPath());

        List<Project> all = ProjectStore.load();
        assertTrue(all.stream().anyMatch(p -> p.getName().equals("test_analytics")));

        // Cleanup
        ProjectStore.delete(created);
    }

    @Test
    public void testProjectChangeListenerNotification() {
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        ProjectStore.ProjectChangeListener listener = p -> {
            if ("demo_proj".equals(p.getName())) {
                listenerFired.set(true);
            }
        };

        ProjectStore.addProjectChangeListener(listener);

        Project demo = new Project("demo_proj", "/tmp/demo_proj");
        ProjectStore.setCurrentProject(demo);

        assertTrue(listenerFired.get(), "Project change listener should have fired when current project was set");

        // Clean up
        ProjectStore.removeProjectChangeListener(listener);
        ProjectStore.delete(demo);
        ProjectStore.setCurrentProject(new Project("default", System.getProperty("user.home") + "/DBNavigatorProjects/default"));
    }

    @Test
    public void testRecentProjectsExcludesCurrent() {
        Project p1 = ProjectStore.createOrOpenProject(System.getProperty("user.home") + "/p1");
        Project p2 = ProjectStore.createOrOpenProject(System.getProperty("user.home") + "/p2");

        ProjectStore.setCurrentProject(p2);
        List<Project> recents = ProjectStore.getRecentProjects();

        // p2 is current, so it should not be in recents
        assertFalse(recents.stream().anyMatch(p -> p.getName().equals(p2.getName())));
        // p1 should be in recents
        assertTrue(recents.stream().anyMatch(p -> p.getName().equals(p1.getName())));

        // Clean up
        ProjectStore.delete(p1);
        ProjectStore.delete(p2);
    }

    @Test
    public void testRecentProjectsWithExplicitCurrent() {
        Project p1 = ProjectStore.createOrOpenProject(System.getProperty("user.home") + "/exp1");
        Project p2 = ProjectStore.createOrOpenProject(System.getProperty("user.home") + "/exp2");

        List<Project> recentsForP1 = ProjectStore.getRecentProjects(p1);
        assertFalse(recentsForP1.stream().anyMatch(p -> p.getName().equals(p1.getName())));
        assertTrue(recentsForP1.stream().anyMatch(p -> p.getName().equals(p2.getName())));

        List<Project> recentsForP2 = ProjectStore.getRecentProjects(p2);
        assertFalse(recentsForP2.stream().anyMatch(p -> p.getName().equals(p2.getName())));
        assertTrue(recentsForP2.stream().anyMatch(p -> p.getName().equals(p1.getName())));

        ProjectStore.delete(p1);
        ProjectStore.delete(p2);
    }

    @Test
    public void testProjectConnectionIsolation() {
        ConnectionProfile profileDefault = new ConnectionProfile();
        profileDefault.setName("Default PG");
        profileDefault.setProjectName("default");

        ConnectionProfile profileCustom = new ConnectionProfile();
        profileCustom.setName("Custom MySQL");
        profileCustom.setProjectName("e-commerce");

        ConnectionStore.saveOrUpdate(profileDefault);
        ConnectionStore.saveOrUpdate(profileCustom);

        List<ConnectionProfile> forDefault = ConnectionStore.loadForProject("default");
        List<ConnectionProfile> forCustom = ConnectionStore.loadForProject("e-commerce");

        assertTrue(forDefault.stream().anyMatch(p -> "Default PG".equals(p.getName())));
        assertFalse(forDefault.stream().anyMatch(p -> "Custom MySQL".equals(p.getName())),
                "Custom project connection must NOT leak into default project");

        assertTrue(forCustom.stream().anyMatch(p -> "Custom MySQL".equals(p.getName())));
        assertFalse(forCustom.stream().anyMatch(p -> "Default PG".equals(p.getName())),
                "Default project connection must NOT leak into custom project");

        // Cleanup
        ConnectionStore.delete(profileDefault);
        ConnectionStore.delete(profileCustom);
    }

    @Test
    public void testLegacyProfileWithoutProjectDefaultsToDefault() {
        ConnectionProfile legacyProfile = new ConnectionProfile();
        legacyProfile.setName("Legacy Connection");
        legacyProfile.setProjectName(""); // empty, legacy behavior

        ConnectionStore.saveOrUpdate(legacyProfile);

        List<ConnectionProfile> forDefault = ConnectionStore.loadForProject("default");
        assertTrue(forDefault.stream().anyMatch(p -> "Legacy Connection".equals(p.getName())),
                "Legacy profiles without project must be accessible under default project");

        ConnectionStore.delete(legacyProfile);
    }
}
