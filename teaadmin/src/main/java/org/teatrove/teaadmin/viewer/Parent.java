package org.teatrove.teaadmin.viewer;

public class Parent implements Comparable<Parent> {
    private String path;
    private String name;
    private boolean directory;

    public Parent(String path, String name, boolean directory) {
        this.path = path;
        this.name = name;
        this.directory = directory;
    }

    public String getPath() { return this.path; }
    public String getName() { return this.name; }
    public boolean isDirectory() { return this.directory; }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (directory ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Parent other = (Parent) obj;
        if (directory != other.directory) return false;
        if (name == null) {
            if (other.name != null) return false;
        }
        else if (!name.equals(other.name)) return false;
        if (path == null) {
            if (other.path != null) return false;
        }
        else if (!path.equals(other.path)) return false;
        return true;
    }

    @Override
    public int compareTo(Parent other) {
        if (this.isDirectory() && !other.isDirectory()) { return -1; }
        else if (other.isDirectory() && !this.isDirectory()) { return 1; }

        int compare = this.getPath().compareTo(other.getPath());
        if (compare != 0) { return compare; }

        return this.getName().compareTo(other.getName());
    }
}
