package com.roze.dbnavigator.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.dbnavigator.model.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages persistence and active state of DBNavigator projects.
 * Persisted to ~/.dbnavigator/projects.json.
 */
public final class ProjectStore {

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "projects.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final List<Project> projects = new ArrayList<>();
    private static Project currentProject = null;
    private static boolean loadedFromDisk = false;

    @FunctionalInterface
    public interface ProjectChangeListener extends Consumer<Project> {}

    private static final List<Consumer<Project>> projectChangeListeners = new CopyOnWriteArrayList<>();

    private ProjectStore() {}

    public static synchronized List<Project> load() {
        if (loadedFromDisk) {
            return new ArrayList<>(projects);
        }
        loadedFromDisk = true;
        projects.clear();

        if (Files.exists(FILE)) {
            try {
                List<Project> loaded = MAPPER.readValue(FILE.toFile(), new TypeReference<List<Project>>() {});
                if (loaded != null) {
                    projects.addAll(loaded);
                }
            } catch (IOException e) {
                System.err.println("Could not read projects file: " + e.getMessage());
            }
        }

        // Ensure "default" project always exists
        Project defaultProject = findByName("default");
        if (defaultProject == null) {
            String defaultPath = Path.of(System.getProperty("user.home"), "DBNavigatorProjects", "default").toString();
            defaultProject = new Project("default", defaultPath);
            try {
                Files.createDirectories(Path.of(defaultPath));
            } catch (Exception ignored) {}
            projects.add(0, defaultProject);
            persist();
        }

        if (currentProject == null) {
            currentProject = defaultProject;
        }

        return new ArrayList<>(projects);
    }

    public static synchronized Project getCurrentProject() {
        if (!loadedFromDisk) {
            load();
        }
        if (currentProject == null) {
            currentProject = findByName("default");
            if (currentProject == null && !projects.isEmpty()) {
                currentProject = projects.get(0);
            }
        }
        return currentProject;
    }

    public static synchronized void setCurrentProject(Project project) {
        if (project == null) return;
        load();
        project.setLastOpened(System.currentTimeMillis());
        currentProject = project;

        // Update in list
        projects.removeIf(p -> p.getName().equalsIgnoreCase(project.getName()));
        projects.add(0, project);
        persist();

        notifyListeners(project);
    }

    public static synchronized Project findByName(String name) {
        if (!loadedFromDisk) load();
        if (name == null || name.isBlank()) return null;
        for (Project p : projects) {
            if (p.getName().equalsIgnoreCase(name.trim())) {
                return p;
            }
        }
        return null;
    }

    public static synchronized Project saveOrUpdate(Project project) {
        if (project == null) return null;
        load();
        projects.removeIf(p -> p.getName().equalsIgnoreCase(project.getName()));
        projects.add(0, project);
        persist();
        return project;
    }

    public static synchronized void delete(Project project) {
        if (project == null || "default".equalsIgnoreCase(project.getName())) return;
        load();
        projects.removeIf(p -> p.getName().equalsIgnoreCase(project.getName()));
        if (currentProject != null && currentProject.getName().equalsIgnoreCase(project.getName())) {
            currentProject = findByName("default");
            notifyListeners(currentProject);
        }
        persist();
    }

    public static synchronized List<Project> getRecentProjects() {
        return getRecentProjects(getCurrentProject());
    }

    public static synchronized List<Project> getRecentProjects(Project current) {
        load();
        List<Project> recents = new ArrayList<>();
        for (Project p : projects) {
            if (current == null || !p.getName().equalsIgnoreCase(current.getName())) {
                recents.add(p);
            }
        }
        recents.sort((a, b) -> Long.compare(b.getLastOpened(), a.getLastOpened()));
        return recents;
    }

    public static synchronized Project createOrOpenProject(String nameOrPath) {
        load();
        if (nameOrPath == null || nameOrPath.isBlank()) {
            return getCurrentProject();
        }
        String trimmed = nameOrPath.trim();
        Path p = Path.of(trimmed);

        String projectName;
        String projectPath;

        if (trimmed.contains("/") || trimmed.contains("\\") || p.isAbsolute()) {
            projectPath = p.toAbsolutePath().normalize().toString();
            projectName = p.getFileName() != null ? p.getFileName().toString() : "untitled";
        } else {
            projectName = trimmed;
            projectPath = Path.of(System.getProperty("user.home"), "DBNavigatorProjects", projectName).toString();
        }

        try {
            Files.createDirectories(Path.of(projectPath));
        } catch (Exception e) {
            System.err.println("Could not create project directory: " + e.getMessage());
        }

        Project existing = findByName(projectName);
        if (existing != null) {
            existing.setPath(projectPath);
            existing.setLastOpened(System.currentTimeMillis());
            saveOrUpdate(existing);
            return existing;
        }

        Project newProject = new Project(projectName, projectPath);
        saveOrUpdate(newProject);
        return newProject;
    }

    public static void addProjectChangeListener(Consumer<Project> listener) {
        if (listener != null && !projectChangeListeners.contains(listener)) {
            projectChangeListeners.add(listener);
        }
    }

    public static void removeProjectChangeListener(Consumer<Project> listener) {
        projectChangeListeners.remove(listener);
    }

    private static void notifyListeners(Project project) {
        for (Consumer<Project> listener : projectChangeListeners) {
            try {
                listener.accept(project);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void persist() {
        try {
            Files.createDirectories(FILE.getParent());
            MAPPER.writeValue(FILE.toFile(), projects);
        } catch (IOException e) {
            System.err.println("Could not save projects: " + e.getMessage());
        }
    }
}
