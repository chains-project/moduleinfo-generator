package io.github.algomaster99.moduleinfo.it;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
class ModuleInfoGeneratorMojoIT {
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:generate")
    @DisplayName("Generates expected module-info.java for victim project")
    @MavenTest
    void maven_hijack(MavenExecutionResult result) throws IOException {
        assertThat(result).isSuccessful();
        String expectedModuleInfo = """
                module victim {
                	requires org.postgresql.jdbc;
                }
                """.stripIndent().trim();
        Path actualModuleInfo = Paths.get(String.valueOf(result.getMavenProjectResult().getTargetProjectDirectory()), "src", "main", "java", "module-info.java");
        assertThat(actualModuleInfo).exists();
        String actualContent = java.nio.file.Files.readString(actualModuleInfo).strip();
        assertThat(actualContent).isEqualTo(expectedModuleInfo);


    }
}

