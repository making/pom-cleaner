package am.ik.tools.pom_cleaner;

public interface Versionable {
    String getGroupId();
    String getArtifactId();
    String getVersion();
    void setVersion(String value);
}
