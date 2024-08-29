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

    @Override
    public String toString() {
        return relativePath;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResourceLocation r) {
            return r.relativePath.equals(relativePath);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return relativePath.hashCode();
    }
}
