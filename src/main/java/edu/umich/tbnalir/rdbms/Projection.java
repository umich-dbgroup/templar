package edu.umich.tbnalir.rdbms;

/**
 * Created by cjbaik on 9/12/17.
 */
public class Projection {
    String alias;           // Alias for attribute, if there is one
    Attribute attribute;

    public Projection(String alias, Attribute attribute) {
        this.alias = alias;
        this.attribute = attribute;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public boolean covers(Projection other) {
        if (this.equals(other)) return true;

        if (this.getAlias() == null && this.getAttribute().equals(other.getAttribute())) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.alias != null) {
            sb.append(this.alias);
            sb.append(".");
        }
        sb.append(this.attribute.getName());
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Projection that = (Projection) o;

        if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
        return !(attribute != null ? !attribute.equals(that.attribute) : that.attribute != null);

    }

    @Override
    public int hashCode() {
        int result = alias != null ? alias.hashCode() : 0;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        return result;
    }
}
