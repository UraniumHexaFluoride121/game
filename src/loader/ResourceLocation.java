package loader;

public class ResourceLocation {
    //Path relative to the assets folder
    public String relativePath;

    public ResourceLocation(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getPath(String assetsPath) {
        return assetsPath + relativePath;
    }
}
