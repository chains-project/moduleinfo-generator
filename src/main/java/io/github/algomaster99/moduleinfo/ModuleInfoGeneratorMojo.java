package io.github.algomaster99.moduleinfo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;

/**
 * Generates module-info.java from project dependencies
 */
@Mojo(
		name = "generate",
		defaultPhase = LifecyclePhase.GENERATE_SOURCES,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class ModuleInfoGeneratorMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(property = "moduleName", required = false)
	private String moduleName;

	@Parameter(defaultValue = "${project.basedir}/src/main/java", property = "outputDirectory")
	private File outputDirectory;

	public void execute() throws MojoExecutionException {
		getLog().info("Generating module-info.java...");

		// Determine module name
		String finalModuleName = moduleName;
		if (finalModuleName == null || finalModuleName.trim().isEmpty()) {
			finalModuleName = project.getArtifactId().replace("-", ".");
		}
		getLog().info("Module name: " + finalModuleName);

		// Collect all dependencies (including transitive)
		Set<Artifact> artifacts = project.getArtifacts();
		getLog().info("Found " + artifacts.size() + " dependencies (including transitive)");

		// Use TreeSet to maintain sorted order
		Set<String> moduleNames = new TreeSet<>();

		for (Artifact artifact : artifacts) {
			// Skip runtime scope dependencies because they are not needed at compile time
			if (SCOPE_RUNTIME.equals(artifact.getScope())) {
				continue;
			}
			// Convert artifact name to module name
			String depModuleName = convertToModuleName(artifact);
			moduleNames.add(depModuleName);
			getLog().debug("Adding dependency: " + depModuleName +
					" (from " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ")");
		}

		// Generate module-info.java
		try {
			generateModuleInfo(finalModuleName, moduleNames);
			getLog().info("Successfully generated module-info.java at " +
					new File(outputDirectory, "module-info.java").getAbsolutePath());
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to generate module-info.java", e);
		}
	}

	/**
	 * Converts Maven artifact to module name
	 */
	private String convertToModuleName(Artifact artifact) {
		String artifactId = artifact.getArtifactId();

		// Check for Automatic-Module-Name in JAR manifest
		File jarFile = artifact.getFile();
		if (jarFile != null && jarFile.exists() && jarFile.getName().endsWith(".jar")) {
			String automaticModuleName = getAutomaticModuleName(jarFile);
			if (automaticModuleName != null) {
				getLog().debug("Using Automatic-Module-Name: " + automaticModuleName +
						" for " + artifact.getGroupId() + ":" + artifact.getArtifactId());
				return automaticModuleName;
			}
		}

		// Fallback: use artifact name only
		getLog().debug("Using artifact name: " + artifactId +
				" for " + artifact.getGroupId() + ":" + artifact.getArtifactId());
		return artifactId;
	}

	/**
	 * Reads Automatic-Module-Name from JAR manifest
	 */
	private String getAutomaticModuleName(File jarFile) {
		try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
			java.util.jar.Manifest manifest = jar.getManifest();
			if (manifest != null) {
				java.util.jar.Attributes mainAttributes = manifest.getMainAttributes();
				return mainAttributes.getValue("Automatic-Module-Name");
			}
		} catch (IOException e) {
			getLog().warn("Failed to read manifest from " + jarFile.getName() + ": " + e.getMessage());
		}
		return null;
	}

	/**
	 * Generates the module-info.java file
	 */
	private void generateModuleInfo(String moduleName, Set<String> dependencies) throws IOException {
		// Ensure output directory exists
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		File moduleInfoFile = new File(outputDirectory, "module-info.java");

		try (FileWriter writer = new FileWriter(moduleInfoFile)) {
			writer.write("module " + moduleName + " {\n");

			for (String dep : dependencies) {
				writer.write("\trequires " + dep + ";\n");
			}

			writer.write("}\n");
		}
	}
}